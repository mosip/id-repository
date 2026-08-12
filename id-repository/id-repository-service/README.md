# id-repository-service

MOSIP **ID-Repository deployable service**. Contains only:

- `IdRepositoryBootApplication` — Spring Boot entry point
- REST **controllers** (identity, credential, credential-request, **VID**)
- Web wiring (security, filters, OpenAPI, URL path prefixes)

All business logic, entities, batch jobs, and scheduled tasks live in **`id-repository-core`**.

## Module split

```
id-repository-core.jar    ← library (services, jobs, entities, batch, pipeline)
id-repository-service.jar ← deployable (controllers + boot only)
```

## Local development

Use docker-compose under `id-repository/local-dev-setup/` (Postgres, config-server, WireMock, BioSDK, keymanager, datashare, and this service).

See [`../local-dev-setup/LOCAL-DEV-SETUP.md`](../local-dev-setup/LOCAL-DEV-SETUP.md) and [`../local-dev-setup/AGENTS.md`](../local-dev-setup/AGENTS.md).

Requires JDK 21, Maven 3.9+, Docker.

## Build

```bash
cd id-repository
mvn clean install -pl id-repository-service -am -DskipTests=true
```

## Jobs

All background work is in core under `io.mosip.idrepository.core.jobs`:

| Job | Class |
|-----|-------|
| Credential status poll (~1s) | `CredentialStatusJob` |
| Batch process / reprocess | `CredentialScheduleJobConfiguration` + `BatchConfiguration` |
| Partner cache refresh | `PartnerCacheUpdateSchedulerConfig` |
| Scheduler thread pool | `JobSchedulerPoolConfig` |

Gate with `mosip.idrepo.jobs.enabled=true` (false on HTTP-only HPA pods).

## URLs (unchanged)

- `/idrepository/v1/identity/*`
- `/idrepository/v1/vid/*` (and `/idrepository/v1/draft/vid`)
- `/v1/credentialservice/*`
- `/v1/credentialrequest/*`

## Salt generator (separate module)

Salt population is **not** part of this HTTP service.

- **Local docker-compose:** salts are seeded in `local-dev-setup/docker-compose/init.sql` (no salt-gen Job).
- **Cluster:** Kubernetes Job via `helm/idrepo-saltgen` with image `id-repository-salt-generator` (not `id-repository-service`).
