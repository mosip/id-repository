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

## Local development (Windows)

From the repository root:

```bat
copy local-run.env.example.bat local-run.env.bat
REM Edit DB_HOST, DB_USER, DB_PASSWORD, CONFIG_URI in local-run.env.bat
run-id-repository-local.bat
```

- **Default database mode:** three PostgreSQL databases (`mosip_idrepo`, `mosip_idmap`, `mosip_credential`) on `DB_HOST`
- **Config server:** MOSIP Spring Cloud Config (`CONFIG_URI`) supplies kernel URLs, Keycloak, etc.
- **Salt job:** `run-id-repository-saltgen-local.bat` (same `local-run.env.bat`)

Requires JDK 21, Maven 3.9+, network access to config server and PostgreSQL.

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

Salt population is **not** part of this HTTP service. Use the dedicated module:

```bash
cd id-repository
mvn install -pl id-repository-salt-generator -am -DskipTests=true
java -jar id-repository-salt-generator/target/id-repository-salt-generator-*.jar
```

Local Windows: `run-id-repository-saltgen-local.bat` from repo root.

Deployed as a **Kubernetes Job** via `helm/idrepo-saltgen` with image `id-repository-salt-generator` (not `id-repository-service`).
