# IdRepo API Test — Module Guide

Module artifact: `apitest-idrepo`. Package: `io.mosip.testrig.apirig.idrepo`. Runner: `MosipTestRunner`.

## Key Files

| File | Purpose |
|---|---|
| `testscripts/AddIdentity.java` | Adds identity; handles `$EMAILVALUE$`/`$PHONENUMBERFORIDENTITY$` replacement and calls `IdRepoArrayHandle` |
| `testscripts/UpdateIdentityForArrayHandles.java` | Updates identity for handle-related negative tests |
| `utils/IdRepoUtil.java` | Extends `AdminTestUtil`; `isTestCaseValidForExecution` with schema-field skip logic |
| `utils/IdRepoArrayHandle.java` | All `selectedHandles` mutation logic for negative test cases |
| `resources/idRepository/AddIdentity/AddIdentity.yml` | Main AddIdentity test cases |
| `resources/idRepository/AddIdentity/AddIdentityArrayHandle.yml` | Handle-specific AddIdentity negative tests |
| `resources/idRepository/UpdateIdentity/UpdateIdentity.yml` | UpdateIdentity test cases |
| `resources/idRepository/UpdateIdentityArrayHandle/UpdateIdentityArrayHandle.yml` | Handle-specific UpdateIdentity negative tests |

## HBS Template Generation

`AdminTestUtil.modifySchemaGenerateHbs` (called in `AddIdentity.java`) generates the identity request template dynamically from the live IdSchema. It uses the field's `$ref` type — NOT the field name — to decide the value structure:

| `$ref` type | Generated structure |
|---|---|
| `documentType` | `{format:"txt", type:"DOC001", value:"fileReferenceID"}` |
| `biometricsType` | `{format:"cbeff", version:1.0, value:"fileReferenceID"}` |
| `simpleType` | `[{language, value}]` per language |
| `hashType` | `{hash, salt}` from keymanager |
| `type:"array"` + `handle:true` (inline) | `[{value:"{{fieldName}}", tags:["handles"]}]` |
| phone field name (actuator-resolved) | `"$PHONENUMBERFORIDENTITY$"` token |
| email field name (actuator-resolved, `type:"string"`) | `"{{email}}"` Handlebars placeholder |
| email field name (actuator-resolved, `type:"array"`) | `[{value:"{{email}}", tags:["handles"]}]` |
| everything else | `"{{fieldName}}"` + logger.warn |

`updateIdentityHbs` (for UpdateIdentity tests) uses the same logic but additionally skips `documentType` and `biometricsType` fields (updates don't re-upload documents/biometrics).

## Handle Types in the Schema

Two distinct structures depending on schema `type`, both marked `"handle": true`:

- **`type: "string"`** → plain string in the identity body (e.g. `"phone": "+4082079007"`). `selectedHandles` tells the server it is a handle; server reads the string directly.
- **`type: "array"`** → array of `{value, tags}` objects (e.g. `"email": [{"value": "...", "tags": ["handles"]}]`). Server reads each item's `value` for handle lookups.

Both appear in `selectedHandles`. The body structure difference is entirely schema-driven, not code-driven.

## Email Token Flow

```text
YAML input:  "email": "$EMAILVALUE$"
    ↓ Handlebars rendering (getJsonFromTemplate)
HBS template: {{email}} → "$EMAILVALUE$"   (the token passes through as a string)
    ↓ Java replacement in AddIdentity.java (BEFORE replaceArrayHandleValues)
Request body: "email": [{"value": "TestCaseName_runContext@mosip.net", "tags": ["handles"]}]
```

**Negative email tests** skip `$EMAILVALUE$` entirely — they provide the bad value directly in YAML:

```yaml
"email": "notanemail"    # no @, server rejects
"email": ""              # empty, server rejects
```

## Critical Ordering Rule in `AddIdentity.java`

`$EMAILVALUE$` and `$PHONENUMBERFORIDENTITY$` **must be replaced before** `IdRepoArrayHandle.replaceArrayHandleValues` is called. If the order is reversed, `applyWithDuplicateValue` reads the literal token string `"$EMAILVALUE$"` as the handle value, saves it in `selectedHandlesValue`, and every subsequent duplicate-check test replaces the token with its own unique email — so no duplicate is ever detected by the server.

Current correct order in `AddIdentity.java`:
1. `$FUNCTIONALID$` replacement
2. `$UIN$` / `$RID$` replacement
3. `$PHONENUMBERFORIDENTITY$` + `$EMAILVALUE$` replacement  ← must be here
4. `IdRepoArrayHandle.replaceArrayHandleValues(inputJson, testCaseName)`  ← then this

## `requiredSchemaFields` — Schema-Conditional Skip

YAML test cases can declare required schema fields. If the live IdSchema's `required` array does not include that field, the test auto-skips (not fails):

```yaml
requiredSchemaFields: [dateOfBirth]   # skips when schema has no dateOfBirth
requiredSchemaFields: [email]         # skips when schema has no email field
```

Implemented in `IdRepoUtil.isTestCaseValidForExecution`, checked against `AdminTestUtil.globalRequiredFields`. Use for negative tests targeting fields that may be absent in some country schemas. The field name check is case-insensitive but must match the actual schema field name — `[dateOfBirth]` will NOT match a schema that names the field `DOB`.

## `IdRepoArrayHandle` — Dispatch Structure

`replaceArrayHandleValues` (AddIdentity) and `replaceArrayHandleValuesForUpdateIdentity` (UpdateIdentity) both follow the same structure:

```text
1. Pre-loop early returns — global operations that modify selectedHandles or exit immediately
   (e.g. _withoutselectedhandles, _removealltagshandles, _replaceselectedhandles)

2. Per-handle loop — for each handle in selectedHandles that is a JSONArray:
   applyAddIdentityHandleMutation (or applyUpdateIdentityHandleMutation)
   → dispatches to named private static methods by testCaseName.contains(...)
```

**Pattern ordering rule**: more-specific strings must be checked before shorter substrings they contain. Examples:
- `_withmultiplevaluesandwithouttags` before `_withouttags` and `_withmultiplevalues`
- `_withupdatedselectedhandleanddemo` before `_withupdatedselectedhandle`
- `_withupdatetagsandhandles` before `_withupdatetags`
- `_save_withdublicatevalue` before `_withdublicatevalue`
- `_withmultipledublicatevalue` before `_withdublicatevalue`

Wrong order → wrong branch taken silently, test sends wrong data.

**Guard for non-array handles**: always check `identity.get(handle) instanceof JSONArray` before calling `getJSONArray`. Some handles (e.g. phone with `type: "string"`) are plain strings, not arrays.

## Duplicate Handle Test Chain (TC_38 → TC_39 → TC_40 → TC_41)

These four tests in `AddIdentityArrayHandle.yml` form a chain:

| TC | Test case name suffix | Purpose |
|---|---|---|
| TC_38 | `_save_withdublicatevalue` | Creates identity; saves the resolved email handle value into `savedHandleValues` |
| TC_39 | `_withdublicatevalue` | Sends TC_38's saved email as the handle → duplicate → IDR-IDC-014 |
| TC_40 | `_withmultipledublicatevalue` | Adds a second array entry using `savedHandleValues` → duplicate → IDR-IDC-014 |
| TC_41 | `_removevalueaddexistingvalue` | Removes value then re-adds from `savedHandleValues` → duplicate → IDR-IDC-014 |

`savedHandleValues` is a `private static final Map<String, String>` in `IdRepoArrayHandle`. TC_38 saves into it; TC_39/40/41 read from it. If TC_38 fails or runs out of order, TC_39/40/41 get null or a stale value → server accepts as unique → tests fail with ACTIVATED instead of IDR-IDC-014.

## UpdateIdentity Array Handle Fixes

Three known issues fixed in `IdRepoArrayHandle.java` for UpdateIdentity tests:

| Pattern | Problem | Fix |
|---|---|---|
| `_withupdatevalues` | Set all handle values to `"mosip...0"` — not a valid email format, server rejects email field | Email handle gets `"mosip_update_<random>@mosip.net"`; other handles get generic string |
| `_withupdatedselectedhandleanddemo` | `getJSONArray(handle)` throws when handle is a plain String (phone `type:"string"`) | `instanceof JSONArray` guard added before `getJSONArray` call |
| `_removeselectedhandlesandupdateemail` | Put `"$ID:...EMAIL$"` literal into identity after framework token resolution is done — server receives the raw token string | Removed email overwrite; email already has correct value from HBS + `$EMAILVALUE$` substitution |

## `updateIdentityHbs` vs `modifySchemaGenerateHbs`

`updateIdentityHbs` (in `AdminTestUtil`) still uses the **old** `$EMAILVALUE$` literal in the HBS template (not `{{email}}`). This is intentional for `UpdateIdentity` tests — the replacement still works because `UpdateIdentityForArrayHandles.java` replaces tokens **before** calling `replaceArrayHandleValuesForUpdateIdentity`.

The fix to use `{{email}}` was applied only to `modifySchemaGenerateHbs` (used by `AddIdentity`). The three sibling methods (`updateIdentityHbs`, `generateHbsForUpdateDraft`, `generateHbsForPrereg`) are pending the same `$ref`-type-driven refactor.

## Common Pitfalls

- **`_save_withdublicatevalue` must run before `_withdublicatevalue`** — YAML order matters; they must be in this sequence in the file.
- **Email in `UpdateIdentityArrayHandle.yml`** — all inputs use `"email": "mosipuser123@mailinator.com"` (ignored by Handlebars since HBS uses `$EMAILVALUE$`); the actual email comes from the HBS template.
- **`requiredSchemaFields` field names are literal** — `[dateOfBirth]` will NOT match a schema that calls the field `DOB`. Both the YAML key and `requiredSchemaFields` must use the same field name as the live schema.
- **Recovery from accidentally emptied YAML**: `cd target && jar xf apitest-idrepo-1.2.1-SNAPSHOT.jar <path/in/jar>`, then copy to `src/main/resources/`.
