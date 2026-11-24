# id-repository-service

Single HTTP deployable for MOSIP ID Repository. Hosts identity, draft, VID, credential store, and credential request APIs in one JVM.

- Port **8090**, servlet root `/`; each domain keeps its pre-consolidation prefix
- Identity and draft: `/idrepository/v1/identity/**` (DB `mosip_idrepo`)
- VID: `/idrepository/v1/vid/**`, `/idrepository/v1/draft/vid` (DB `mosip_idmap`)
- Credential store: `/v1/credentialservice/**` (DB `mosip_credential`)
- Credential request: `/v1/credentialrequest/**` (DB `mosip_credential`)
- Salt is a **separate** Job (`id-repository-salt-generator` / `helm/idrepo-saltgen`); local salts are seeded by `local-dev-setup/deps/init.sql`
- Swagger ("Select a definition" switches domains, Authorize header `Authorization`):
  - http://localhost:8090/idrepository/v1/identity/swagger-ui/index.html
  - http://localhost:8090/idrepository/v1/swagger-ui/index.html
  - http://localhost:8090/v1/credentialservice/swagger-ui/index.html
  - http://localhost:8090/v1/credentialrequest/swagger-ui/index.html
- OpenAPI JSON:
  - http://localhost:8090/idrepository/v1/identity/v3/api-docs
  - http://localhost:8090/idrepository/v1/v3/api-docs
  - http://localhost:8090/v1/credentialservice/v3/api-docs
  - http://localhost:8090/v1/credentialrequest/v3/api-docs
- Actuator (`health`, `info`, `prometheus`, `refresh`, `restart`):
  - http://localhost:8090/idrepository/v1/identity/actuator/health
  - http://localhost:8090/idrepository/v1/identity/actuator/info
  - http://localhost:8090/idrepository/v1/identity/actuator/prometheus
- Open without token (`mosip.global.end-points`): Swagger, OpenAPI, actuator, `/idrepository/v1/identity/callback/**`, `/v1/credentialrequest/callback/**`

```text
cd id-repository && mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true
cd local-dev-setup
./run-idrepo-local.sh [run] | start | rebuild | stop | smoke | test | wipe | wipe-run
run-idrepo-local.bat …
```

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| POST · PATCH | `/idrepository/v1/identity` · `/v2` | Add · update identity |
| GET · POST | `/idrepository/v1/identity/idvid/{id}` · `/idvid/` · `/idvid/v2` | Retrieve identity by UIN, VID, or handle |
| GET · POST | `/idrepository/v1/identity/authtypes/status/…` | Auth type lock status · update |
| GET | `/idrepository/v1/identity/rid/{individualId}` | Latest RID for an individual |
| POST | `/idrepository/v1/identity/idvid-metadata/search` | Search identity metadata |
| GET | `/idrepository/v1/identity/{individualId}/update-counts` · `/update-counts` | Remaining update counts |
| POST | `/idrepository/v1/identity/callback/vid_credential_status_update` | WebSub callback |
| POST | `/idrepository/v1/identity/draft/create/{registrationId}` · `/v2/…` | Create draft |
| PATCH | `/idrepository/v1/identity/draft/update/{registrationId}` · `/v2/…` · `/uindata/…` | Update draft |
| GET | `/idrepository/v1/identity/draft/publish/{registrationId}` · `/v2/…` | Publish draft (creates draft VID) |
| DELETE | `/idrepository/v1/identity/draft/discard/{registrationId}` · `/v2/…` | Discard draft |
| HEAD · GET | `/idrepository/v1/identity/draft/{registrationId}` · `/v2/…` | Draft exists · get draft |
| PUT | `/idrepository/v1/identity/draft/extractbiometrics/{registrationId}` · `/v2/…` | Extract biometric templates |
| GET | `/idrepository/v1/identity/draft/uin/{UIN}` | Draft registration ID for a UIN |
| POST | `/idrepository/v1/vid` · `/draft/vid` | Create VID · draft VID |
| GET | `/idrepository/v1/vid/{VID}` · `/vid/uin/{UIN}` | UIN by VID · VIDs by UIN |
| PATCH · POST | `/idrepository/v1/vid/{VID}` · `/vid/{VID}/regenerate` | Update status · regenerate |
| POST | `/idrepository/v1/vid/deactivate` · `/vid/reactivate` | Deactivate · reactivate VIDs for a UIN |
| POST · GET | `/v1/credentialservice/issue` · `/types` | Issue credential · credential types |
| POST | `/v1/credentialrequest/requestgenerator` · `/v2/requestgenerator/{rid}` | Create credential request |
| GET · PUT | `/v1/credentialrequest/cancel/{requestId}` · `/get/{requestId}` · `/retrigger/{requestId}` | Cancel · status · retrigger |
| GET | `/v1/credentialrequest/getRequestIds` | Request IDs by status (paged) |
| POST | `/v1/credentialrequest/callback/notifyStatus` | WebSub callback |
| GET | `/v1/credentialrequest/scheduleRetrySubscription` · `/scheduleWebsubSubscription` | WebSub resubscription |

See [docs/design/service-consolidation.md](../../docs/design/service-consolidation.md), [local-dev-setup](../local-dev-setup/README.md), and [AGENTS.md](AGENTS.md).
