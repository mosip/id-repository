# api-test

REST Assured / TestNG rig for public id-repo HTTP (`apitest-idrepo`). How-to: `README.md`.
Smoke = positive cases; `smokeAndRegression` adds negatives.

```text
api-test/
├─ run-local-smoke.bat|sh              local run: host :8090 + WireMock :8082
├─ src/main/java/.../idrepo/
│  ├─ testrunner/MosipTestRunner       setup (dbCleanUp) → suites; data kept after run
│  ├─ testscripts/                     one class per YAML "testcaseClass"
│  └─ utils/IdRepoUtil                 run gate, $RID…$ tokens, dbCleanUp
├─ src/main/resources/
│  ├─ idRepository/<Api>/<Api>.yml+.hbs  cases + templates
│  ├─ config/Idrepo-local.properties   local config (never edit server Idrepo.properties)
│  ├─ config/*DeleteQueries.txt        cleanup SQL; ${testDataCreator} = cr_by to delete
│  └─ testCaseSkippedList.txt
└─ cluster: deploy/idrepo-apitestrig/install.sh
```

## Rules

- Contract change → YAML + HBS (and `api-docs/` if paths change).
- AddIdentity: replace `$PHONENUMBERFORIDENTITY$` / `$EMAILVALUE$` **before** `IdRepoArrayHandle.replaceArrayHandleValues`; it also calls `ensureDemographicDocumentsInRequest` (proofOf* docs).
- `$ID:<case>_<FIELD>$` is cached only for case names containing `_Pos` (UIN/RID/EMAIL).
- `requiredSchemaFields` skips when the live IdSchema lacks the literal field name.
- Duplicate chain order: `_save_withdublicatevalue` → `_withdublicatevalue` → `_withmultipledublicatevalue` → `_removevalueaddexistingvalue`. In `contains` checks, longer names first.
- Handles: schema `string` = plain value, `array` = `[{value, tags}]`; check `instanceof JSONArray` first.
- V2 array handles: `AddIdentityV2ArrayHandle.yml` before `UpdateIdentityV2ArrayHandle`.
- Draft V2 tokens `$RIDV2*$` resolve in `IdRepoUtil.inputStringKeyWordHandeler`; replace `$RIDV2NEWEXPLICIT$` before `$RIDV2NEW$`.
- `modifySchemaGenerateHbs` uses the `$ref` type, not the field name.
- `skipPartnerSetup` skips Keycloak Admin + PMS only. Empty `$BIOVALUE$` → `IDR-IDS-009`; fail fast.
- No secrets in git. Keep IDA/partner response shapes intact.
