# MOSIP ID-Repository

Consolidated identity + VID + credential service. **JDK 21.** Nested `AGENTS.md` apply only when you work in that folder — do not load every guide up front.

| Work | Folder |
|------|--------|
| Java / Maven | `id-repository/` |
| Local Docker deps | `id-repository/local-dev-setup/` |
| Fresh DB | `db_scripts/` |
| Version hops | `db_upgrade_scripts/` |
| Release SQL | `db_release_scripts/` |
| Helm | `helm/` |
| Cluster install | `deploy/` |
| API tests | `api-test/` |
| OpenAPI | `api-docs/` |

**Build:** `cd id-repository && mvn clean install`

## Rules

- Edit one area; read only that folder’s `AGENTS.md`. How-to is README. Prefer Grep/Glob. Skip `*.iso`, `*.p12`, `target/`, WireMock mapping JSON unless needed.
- Three DBs: `mosip_idrepo` (UIN), `mosip_idmap` (VID), `mosip_credential` (store + `BATCH_*`). Never merge schemas or salt tables.
- Salts (`uin_hash_salt`, `uin_encrypt_salt`) exist in **both** idrepo and idmap. Populate via Job `helm/idrepo-saltgen` — never a Deployment or the HTTP JVM. IDA uses `ida.uin_hash_salt` (separate).
- Schema change → `db_scripts` + `db_upgrade_scripts` + `db_release_scripts` (paired rollback/revoke).
- Freeze REST `/idrepository/v1/identity`, `/idrepository/v1/vid`, `/v1/credentialservice`, `/v1/credentialrequest`; WebSub `{partnerId}/CREDENTIAL_ISSUED`, `CREDENTIAL_STATUS_UPDATE`; Keycloak `mosip-idrepo-client`. Update `api-test` + `api-docs` if they change.
- Local laptop: deps in Docker, **app on host** (`local-dev-setup/run-idrepo-local.*`). No id-repo container.
