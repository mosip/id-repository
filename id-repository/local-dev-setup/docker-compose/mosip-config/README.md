# Bundled MOSIP config (local docker-compose)

Spring Cloud Config Server in this stack serves files from **this folder** (`./mosip-config` → `/mosip-config`).  
Do **not** point compose at an external `D:\Project\Mosip\mosip-config` checkout for laptop runs.

## ID-Repository relevant files

Synced from a local `mosip-config` checkout and then adjusted for compose DNS:

| File | Role |
|------|------|
| `application-default.properties` | Shared service URLs (WireMock, keymanager, datashare, BioSDK, id-repo) |
| `id-repository-default.properties` | ID-Repo / credential DB, MinIO, BioSDK, datashare create URL |
| `kernel-default.properties` | Keymanager PKCS12 + DB host (`database`) |
| `credential-request-default.properties` | Credential request generator |
| `credential-service-default.properties` | Credential service / VC context URLs |

Local overrides (examples): `database`, `mock-service:8082`, `keymanager-service:8088`, `minio:9000`, `datashare-service:8097`, `biosdk-service:9099`, `mosip.auth.filter_disable=true`.

## Refresh from an external mosip-config

Copy the five files above from your `mosip-config` repo into this directory, then re-apply the compose URL/password overrides (see `LOCAL-DEV-SETUP.md` §3.2). Do not leave `api-internal.*.mosip.net` / cluster DB hosts in these files.

## Host IDE / Maven

Use `id-repository-service/src/main/resources/application-local.properties` with `-Dspring.profiles.active=local` (published localhost ports), not this folder.
