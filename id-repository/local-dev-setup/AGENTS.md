# local-dev-setup

Deps in Docker, `id-repository-service` on the host. How-to: `README.md`; detail: `LOCAL-DEV-SETUP.md`.

```text
Ports
├─ 5455   Postgres: mosip_idrepo / idmap / credential / keymgr (not postgres/public); users *user / mosip123
├─ 51100  config-server  http://localhost:51100/config
├─ 8082   WireMock (IAM + stubs)
├─ 9000   MinIO
├─ 8088   keymanager
└─ 8090   host app, profiles default,local; logs in .local/logs/
Files
├─ deps/init.sql                            DB users, salts 0–999, static keymgr seed
├─ deps/wiremock/mappings/id-repository.json  every stub
├─ deps/additional_jars/                    kernel-ref-idobjectvalidator.jar (fetched by prep)
├─ deps/mosip-config/                       config-server repo
└─ ../id-repository-service/src/main/resources/application-local.properties
```

Run: build service → `./run-local-stack.sh up` → `./run-idrepo-local.sh` (or `.bat`).

## Rules

- Edit only this tree (incl. `deps/mosip-config/`, WireMock mappings) and `application-local.properties`.
- After `down -v`: `up` again, then restart the host app. Keep `keys/mosip-idrepo-ks.p12`.
- UINs come from WireMock `GET /v1/idgenerator/uin`. A draft `?UIN=` must already exist in `idrepo.uin`.
- After a mapping edit, recreate `mock-service`. Containers call Keycloak at `http://mock-service:8082`, host uses `localhost:8082`. JWKS must match the embedded JWTs.
- `/hub` is proxied to `websub-partner-ack`; an immediate `STORED` races `ISSUED` and can wipe `credential_id`.
- Never: service in Compose, `{cipher}` without a decrypt key, mint scripts, `mosip.auth.filter_disable=true`, kernel-auth-adapter in `additional_jars`.

```text
Symptom      → cause
nothing:8090 → host app not started
IDR-IDC-002  → invented UIN; use idgenerator
IDR-IDC-012  → UIN exists; wipe, restart app
KER-WSC-101  → WireMock / mosip.websub.url
KER-KMA-004  → recreate keymanager-service
IDR-IDS-009  → empty CBEFF in MinIO; fix $BIOVALUE$, not WireMock
```
