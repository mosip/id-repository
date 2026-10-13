# api-test

E2E REST Assured/TestNG for **public** id-repo HTTP. Artifact `apitest-idrepo`. How-to: [`README.md`](README.md).

Smoke = positives. `smokeAndRegression` = + negatives.

Local: `run-local-smoke.bat|sh` vs host `:8090` + WireMock `:8082`. Do not edit server `Idrepo.properties` — use `Idrepo-local.properties`. `env.endpoint` = WireMock; `mosip_components_base_urls` points identity/VID/credential at `:8090`. Cluster: `deploy/idrepo-apitestrig/install.sh`.

## Rules

- Contract change → YAML + HBS under `src/main/resources/idRepository/` (and `api-docs/` if paths change).
- AddIdentity token order: `$PHONENUMBERFORIDENTITY$` / `$EMAILVALUE$` **then** `IdRepoArrayHandle.replaceArrayHandleValues`. Reverse → duplicate tests never fire.
- AddIdentity also calls `IdRepoUtil.ensureDemographicDocumentsInRequest` so schema `documentType` fields (proofOf*) get `documents[]` binaries → `uin_document` / `uin_document_h` (JMeter parity; bio alone only fills `uin_biometric`).
- `requiredSchemaFields` skips when live IdSchema lacks the field (literal name; `dateOfBirth` ≠ `DOB`).
- Duplicate chain YAML order: `_save_withdublicatevalue` → `_withdublicatevalue` → `_withmultipledublicatevalue` → `_removevalueaddexistingvalue` (static `selectedHandlesValue`).
- `instanceof JSONArray` before `getJSONArray` (phone may be a string handle).
- `testCaseName.contains`: longer patterns **before** substrings (`_save_withdublicatevalue` before `_withdublicatevalue`).
- Handles: schema `type:string` = plain string; `type:array` = `[{value, tags}]`. Both in `selectedHandles`.
- Draft V2 (`CreateDraftV2` … `ExtractBiometricsV2`) uses `$RIDV2NEW$` / `$RIDV2LOST$` / … resolved in `IdRepoUtil.inputStringKeyWordHandeler`. Replace `$RIDV2NEWEXPLICIT$` before `$RIDV2NEW$`.
- Array-of-handles V2: `AddIdentityV2/AddIdentityV2ArrayHandle.yml` then `UpdateIdentityV2ArrayHandle` (depends on V2 add UINs).
- HBS: `modifySchemaGenerateHbs` uses `$ref` type, not field name. `updateIdentityHbs` still uses `$EMAILVALUE$` literal — keep token replace **before** handle mutations.
- `skipPartnerSetup` skips Keycloak Admin + PMS only. Mock SBI from `resources/mds/` — empty `$BIOVALUE$` → `IDR-IDS-009`. Fail fast on BioValue errors.
- Do not commit secrets. Do not break IDA/partner response shapes for the rig.
