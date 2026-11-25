# MOSIP ID-Repository

JDK 21. Identity + VID + credential in one JVM `:8090`. Docker deps; app on host.

```text
Protocol (low tokens)
├─ 1. Use this tree → pick folder
├─ 2. Load ONLY that folder's AGENTS.md
├─ 3. Grep/Glob before Read; Read ≤ needed lines
├─ 4. How-to = README (do not dump into context)
├─ 5. Skip: target/ .local/ testng-report/ *.iso *.p12 WireMock mappings/*.json
├─ 6. Short answers; cite paths; no recap
└─ 7. Ask before DB wipe / auth disable / commit-push

.
├─ id-repository/                 Maven parent · mvn clean install (from here)
│  ├─ id-repository-core/         published lib core.* (IDA API)
│  ├─ id-repository-service/      HTTP :8090 · all domain code
│  ├─ id-repository-salt-generator/  salt Job only
│  └─ local-dev-setup/            Docker deps + run-idrepo-local.* + idschema switch
├─ db_scripts/                    fresh DB (drops!)
├─ db_upgrade_scripts/            version hops
├─ db_release_scripts/            release / revoke
├─ helm/                          identity + idrepo-saltgen
├─ deploy/                        cluster install wrappers
├─ api-test/                      API test rig
├─ api-docs/                      OpenAPI YAML
└─ docs/                          config · design/service-consolidation.md
```

## Global (do not expand unless editing contracts/DB)

```text
DBs        mosip_idrepo (UIN) · mosip_idmap (VID) · mosip_credential (+ BATCH_*)
           never merge schemas / salt tables
Salts      uin_hash_salt / uin_encrypt_salt in idrepo+idmap → Job helm/idrepo-saltgen only
           not Deployment / not HTTP JVM · IDA ida.uin_hash_salt separate
Schema Δ   db_scripts + db_upgrade_scripts + db_release_scripts (rollback/revoke)
Contracts  /idrepository/v1/identity · /vid · /v1/credentialservice · /v1/credentialrequest
           WebSub {partnerId}/CREDENTIAL_ISSUED · CREDENTIAL_STATUS_UPDATE
           Keycloak mosip-idrepo-client · Δ → api-test + api-docs
Cluster    Service identity.idrepo (all prefixes) · mosip.idrepo.*.url → http://identity.idrepo
Local      no id-repo container · local-dev-setup/run-idrepo-local.*
```
