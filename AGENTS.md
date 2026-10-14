# MOSIP ID-Repository

JDK 21. Identity + VID + credential in one service. Deps in Docker, app on host :8090.
Load only the `AGENTS.md` of the folder you edit. How-to = README. Grep/Glob before Read.
Skip: `target/` `.local/` `testng-report/` `*.iso` `*.p12` WireMock `mappings/*.json`.

```text
.
├─ id-repository/                    Maven parent · build: cd id-repository && mvn clean install
│  ├─ id-repository-core/            published lib core.* (IDA API)
│  ├─ id-repository-service/         HTTP :8090, all domain code
│  ├─ id-repository-salt-generator/  salt Job
│  └─ local-dev-setup/               Docker deps + host-run scripts
├─ db_scripts/          fresh DB (drops!)
├─ db_upgrade_scripts/  version hops
├─ db_release_scripts/  release / revoke
├─ helm/                charts
├─ deploy/              cluster scripts
├─ api-test/            API test rig
└─ api-docs/            OpenAPI
```

## Global rules

- DBs: `mosip_idrepo` (UIN) · `mosip_idmap` (VID) · `mosip_credential` (store + `BATCH_*`). Never merge schemas or salt tables.
- Salts `uin_hash_salt` / `uin_encrypt_salt` live in idrepo **and** idmap; fill only via Job `helm/idrepo-saltgen`, never a Deployment or the HTTP JVM. IDA's `ida.uin_hash_salt` is separate.
- Schema change → `db_scripts` + `db_upgrade_scripts` + `db_release_scripts` (with rollback / revoke).
- Frozen contracts: REST `/idrepository/v1/identity`, `/idrepository/v1/vid`, `/v1/credentialservice`, `/v1/credentialrequest`; WebSub `{partnerId}/CREDENTIAL_ISSUED`, `CREDENTIAL_STATUS_UPDATE`; Keycloak `mosip-idrepo-client`. Changing one → update `api-test` + `api-docs`.
- Local: no id-repo container; run `local-dev-setup/run-idrepo-local.*`.
