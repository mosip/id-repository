# Local Docker deps

Deps in Docker, **id-repository-service on the host :8090**. How-to: [`README.md`](README.md). Detail: [`LOCAL-DEV-SETUP.md`](LOCAL-DEV-SETUP.md).

| Service | Port |
|---------|------|
| Postgres | 5455 — `mosip_idrepo` / `idmap` / `credential` / `keymgr` (not `postgres/public`) |
| Config-server | 51100 |
| WireMock | 8082 |
| MinIO | 9000 |
| Keymanager | 8088 |
| Host app | 8090 — profiles `default,local` |

App users: `*user` / `mosip123` (`deps/init.sql`). Config URI: `http://localhost:51100/config`. Overrides: `../id-repository-service/src/main/resources/application-local.properties`. Host-app file logs: `.local/logs/` (not `logs/`).

## Rules

- `./run-local-stack.sh up` then `./run-idrepo-local.sh` (or `.bat`). Build first: `mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true`. Host JVM needs `deps/additional_jars/kernel-ref-idobjectvalidator.jar` (`prep` fetches it).
- Edit only this tree (`mosip-config/`, WireMock, `application-local.properties`).
- After `down -v`, re-`up` (init.sql static keymgr seed + keymanager verify) then restart the host app. Keep `keys/mosip-idrepo-ks.p12` — do not regenerate it when using static seed.
- UINs from WireMock `GET /v1/idgenerator/uin` — invented digits → `IDR-IDC-002`. Draft `?UIN=` needs that UIN already in `idrepo.uin`.
- Salts are in `init.sql` (0–999) — do not run salt-generator.
- All stubs: `deps/wiremock/mappings/id-repository.json`. Recreate `mock-service` after mapping edits.
- Docker Keycloak URLs = `http://mock-service:8082` (host: `localhost:8082`). JWKS must match embedded JWTs.
- `/hub` is proxied to `websub-partner-ack`. Immediate `STORED` races ISSUED and can wipe `credential_id`.
- Do not put the service back into Compose. No `{cipher}` without a decrypt key. No mint scripts.

| Symptom | Cause |
|---------|-------|
| Nothing on :8090 | Host app not started |
| `IDR-IDC-002` | Bad UIN — use idgenerator |
| `IDR-IDC-012` | UIN exists — `wipe` then restart app |
| `KER-WSC-101` | WireMock / `mosip.websub.url` |
| `KER-KMA-004` | Recreate `keymanager-service` |
| `IDR-IDS-009` | Empty CBEFF in MinIO — fix `$BIOVALUE$`, not WireMock |
