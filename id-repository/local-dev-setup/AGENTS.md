# local-dev-setup

Deps in Docker; `id-repository-service` on host. How-to: `README.md` · detail: `LOCAL-DEV-SETUP.md`.

```text
Ports
├─ 5455   Postgres  mosip_idrepo / idmap / credential / keymgr  (*user / mosip123)
├─ 51100  config-server
├─ 8082   WireMock (IAM + stubs + idschema)
├─ 9000   MinIO
├─ 8088   keymanager
└─ 8090   host app  profiles default,local  logs .local/logs/

Files
├─ deps/init.sql                         users, salts 0–999, static keymgr seed
├─ deps/wiremock/mappings/               stubs (idschema-latest.json → bodyFileName)
├─ deps/wiremock/__files/idschema-latest-*.json   active + variants
├─ deps/wiremock/idschema/               switch guide + bare schema sources
├─ deps/additional_jars/                 kernel-ref-idobjectvalidator.jar (prep)
├─ deps/mosip-config/                    config-server repo
└─ ../id-repository-service/.../application-local.properties

Cmds
├─ run-local-stack.sh up|wipe|idschema [default|email-handle|diff-handle]|smoke
└─ run-idrepo-local.bat|.sh  run|start|stop|up|wipe|help
```

## Rules

- Edit only this tree + `application-local.properties`.
- After `down -v`: `up`, then restart host. Keep `keys/mosip-idrepo-ks.p12`.
- Schema switch: `idschema <variant>` → recreate WireMock → **restart host** (schema cache).
- UINs from WireMock `GET /v1/idgenerator/uin`. Draft `?UIN=` must exist in `idrepo.uin`.
- Mapping edit → recreate `mock-service`. Containers→`mock-service:8082`; host→`localhost:8082`.
- Never: id-repo in Compose, `{cipher}` w/o key, mint scripts, `mosip.auth.filter_disable=true`, auth-adapter in `additional_jars`.

```text
Symptom      → cause
nothing:8090 → host app not started
IDR-IDC-002  → invented UIN; use idgenerator
IDR-IDC-012  → UIN exists; wipe + restart app
KER-WSC-101  → WireMock / mosip.websub.url
KER-KMA-004  → recreate keymanager-service
IDR-IDS-009  → empty CBEFF; fix $BIOVALUE$
```
