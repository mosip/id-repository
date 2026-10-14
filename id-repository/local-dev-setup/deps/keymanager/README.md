# Keymanager local assets (DDL + bootstrap)

| Path | Role |
|------|------|
| [`ddl/`](ddl/) | Table DDL applied by `deps/init.sql` |
| [`../init.sql`](../init.sql) | **Static** keymgr seed inlined (policy + alias + store + `data_encrypt_keystore`) |
| [`bootstrap.sh`](bootstrap.sh) | Container entrypoint: start KM; skip jar/API when `KM_STATIC_SEED=true` |
| `keys-generator.jar` | **Not used** with default `KM_STATIC_SEED=true`. Gitignored optional fallback only |

## Default path (static seed in init.sql)

1. Postgres runs `deps/init.sql` (DDL + inlined keymgr DML).
2. `bootstrap.sh` with `KM_STATIC_SEED=true` (compose default) **skips** `keys-generator.jar` and full `generateMasterKey`.
3. Verifies `getCertificate?applicationId=ID_REPO&referenceId=identity_data` against `keys/mosip-idrepo-ks.p12` (password **`1234`**), then ensures IDA keys if missing.

### Hard requirement — PKCS12 must match the seed

`key_store.private_key` rows are encrypted with the HSM/PKCS12. Seed is paired with **`keys/mosip-idrepo-ks.p12`** (from `idrepo-ks.p12`, password **`1234`**).

- **Do not** delete/regenerate that PKCS12 after a DB wipe when using the static seed.
- Password is set in `mosip-config/kernel-default.properties` and `bootstrap.sh`.

## Fallback (jar + API)

```yaml
# docker-compose.yml keymanager-service environment:
KM_STATIC_SEED: "false"
```

Then place `keys-generator.jar` under this folder (`prep` can copy it from `mosipid/keys-generator:1.3.0`), and bootstrap runs keys-generator + `generateMasterKey`. Compose does **not** set `KEYS_GENERATOR_JAR` when using the default static seed.

## Why this seed

Paired with `idrepo-ks.p12` (password `1234`): alias UUIDs in the SQL match PKCS12 entries (e.g. `9556e227-…` = ID_REPO). Includes 10k ZK keys from the dump; `IDA` / `CREDENTIAL_SERVICE` policies are added if missing; bootstrap may create IDA certs into the same PKCS12 after verify.
