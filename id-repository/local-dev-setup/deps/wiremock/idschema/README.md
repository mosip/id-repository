# Local ID schema (WireMock)

How to choose which Id Schema local id-repository and api-test see.

## How it works

| Piece | Path |
|-------|------|
| Stub | [`../mappings/idschema-latest.json`](../mappings/idschema-latest.json) — `GET /v1/masterdata/idschema/latest.*` |
| Active body | [`../__files/idschema-latest-active.json`](../__files/idschema-latest-active.json) (`bodyFileName`) |
| Variants | `../__files/idschema-latest-{default,email-handle,diff-handle}.json` |
| Bare schemas (edit/source) | this folder — `schema_with_*.json` |

- WireMock does **not** match `schemaVersion`; every query returns the **active** body.
- The host app **caches** the schema in memory. After a switch you must **restart** id-repository.
- Default committed active file = `default` variant.

## Variants

| Command arg | WireMock body | Source | Handles | Typical local use |
|-------------|---------------|--------|---------|-------------------|
| `default` | `idschema-latest-default.json` | extracted local sample | `email`/`phone` **string**; `functionalId` **array** | Normal api-test / smoke |
| `email-handle` | `idschema-latest-email-handle.json` | [`schema_with_email_handle.json`](schema_with_email_handle.json) | `email` **array** `[{value,tags}]`; `phone` string | Email array-handle cases (~14) that skip on default |
| `diff-handle` | `idschema-latest-diff-handle.json` | [`schema_with_diff_handle.json`](schema_with_diff_handle.json) | `licenseNo` string + `functionalId` array; email/phone **not** handles | Diff-handle / licenseNo scenarios |

Alias: `email-array` → `email-handle`.

## Switch (required steps)

Run from `id-repository/local-dev-setup/` (nested Maven tree).

**1. Pick a variant** (copies onto `idschema-latest-active.json` and recreates WireMock):

```bash
# Git Bash / macOS / Linux / WSL
./run-local-stack.sh idschema                 # list + show which is active
./run-local-stack.sh idschema default
./run-local-stack.sh idschema email-handle
./run-local-stack.sh idschema diff-handle
```

Windows cmd: `bash run-local-stack.sh idschema email-handle`

**2. Restart the host app** (schema cache):

```bash
./run-idrepo-local.sh stop
./run-idrepo-local.sh run
```

Windows: `run-idrepo-local.bat stop` then `run-idrepo-local.bat run`

**3. Verify WireMock** (before or after host restart):

```bash
curl -s "http://localhost:8082/v1/masterdata/idschema/latest?schemaVersion=0"
```

Check `response.schemaJson` (string): for `email-handle`, `identity.properties.email.type` should be `"array"`; for `diff-handle`, look for `licenseNo` with `"handle": true`.

**4. Run api-test** (optional) against the running host on `:8090` — see [`../../../../api-test/README.md`](../../../../api-test/README.md).

| Active schema | Expect |
|---------------|--------|
| `default` | Most suite; email **array**-handle cases **skipped** (`FEATURE_NOT_SUPPORTED`) |
| `email-handle` | Email array-handle cases **run**; some string-email assumptions may skip |
| `diff-handle` | licenseNo / functionalId-oriented cases; email is not a handle |

## Manual fallback (no script)

1. Copy the chosen `../__files/idschema-latest-<variant>.json` over `../__files/idschema-latest-active.json`.
2. Recreate WireMock:

   ```bash
   cd ../..   # deps/
   docker compose up -d --force-recreate --no-deps mock-service
   ```

3. Restart the host app (step 2 above).
4. Verify with the curl in step 3.

## Add another variant

1. Add bare schema JSON here (e.g. `schema_with_foo.json`).
2. Wrap it as a masterdata response under
   `../__files/idschema-latest-<name>.json`:

   ```json
   {
     "id": null,
     "version": null,
     "responsetime": "2026-08-11T09:56:11.931Z",
     "metadata": null,
     "response": {
       "id": "1001",
       "idVersion": 0.1,
       "schemaJson": "<paste entire schema as a JSON string>",
       "effectiveFrom": "2026-06-18T08:47:59.832Z",
       "status": "PUBLISHED",
       "createdBy": "admin"
     },
     "errors": null
   }
   ```

3. Add a `case` in `run-local-stack.sh` → `cmd_idschema`.
4. Update the tables in this file and in [`../../../README.md`](../../../README.md) § ID schema.
