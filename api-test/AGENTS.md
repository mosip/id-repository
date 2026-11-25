# api-test

REST Assured / TestNG (`apitest-idrepo`). How-to: `README.md`. Smoke = positives; `smokeAndRegression` + negatives.

```text
api-test/
├─ run-local-smoke.bat|sh           host :8090 + WireMock :8082
├─ src/main/java/.../idrepo/
│  ├─ testrunner/MosipTestRunner    setup → suites; mkdirs target/local-reports
│  ├─ testscripts/                  one class per YAML testcaseClass
│  └─ utils/
│     ├─ IdRepoUtil                 gate, $RID…$, schemaHandleValue, resolveEmailFieldName, dbCleanUp
│     └─ IdRepoArrayHandle          array-handle scenarios (calls IdRepoUtil.schemaHandleValue)
├─ src/main/resources/
│  ├─ idRepository/<Api>/*.yml+.hbs
│  ├─ config/Idrepo-local.properties   local only (never edit Idrepo.properties)
│  ├─ config/*DeleteQueries.txt
│  └─ testCaseSkippedList.txt
└─ cluster: deploy/idrepo-apitestrig/install.sh
```

## Rules

- Contract Δ → YAML + HBS (+ `api-docs/` if paths change).
- AddIdentity: `$PHONENUMBERFORIDENTITY$` / `$EMAILVALUE$` **before** `IdRepoArrayHandle.replaceArrayHandleValues`.
- Email array handle: do **not** overwrite rendered email in uniquify; new entries → `IdRepoUtil.schemaHandleValue`.
- `$ID:<case>_<FIELD>$` cache only for names with `_Pos`.
- Duplicate chain: longer `_with*dublicate*` tokens first in `contains` checks.
- Handles: `string` vs `array` → check `instanceof JSONArray` first.
- V2 array: `AddIdentityV2ArrayHandle.yml` before `UpdateIdentityV2ArrayHandle`.
- Draft V2: `$RIDV2NEWEXPLICIT$` before `$RIDV2NEW$` in `IdRepoUtil.inputStringKeyWordHandeler`.
- Slack skip + `https?://` endpoint report come from `apitest-commons` (≥ #2029 / develop). No secrets in git.
