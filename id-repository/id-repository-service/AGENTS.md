# AGENTS.md — `id-repository-service`

> Thin HTTP deployable. Boot, REST controllers, web/security/OpenAPI config, and kernel auth shims. **All business logic is in `id-repository-core`.**

---

## 1. Role

| Concern | Where |
|---------|-------|
| Business logic | `id-repository-core` |
| REST controllers | This module |
| Spring Boot entry | `IdRepositoryBootApplication` |
| Security / OpenAPI / scan wiring | `io.mosip.idrepository.config.*` |
| Kernel auth overrides | `io.mosip.kernel.auth.defaultadapter.*` (local shims) |

Port **8090**. Servlet paths: `/idrepository/v1/identity`, `/idrepository/v1/vid`, `/v1/credentialservice/`, `/v1/credentialrequest/`.

---

## 2. Package layout (this module only)

```
io.mosip.idrepository/
├── IdRepositoryBootApplication.java      # main()
├── bootstrap/                            # IdRepositoryLauncher, classloader helpers
├── config/                               # HTTP mode scan, OpenAPI, security, schedulers
├── identity/controller/                  # IdRepoController, IdRepoDraftController, VidEventCallbackController
├── identity/httpfilter/                  # (removed — use core IdRepoFilter)
├── vid/controller/                       # VidController
├── credential/store/controller/          # CredentialStoreController
└── credential/request/controller/        # CredentialRequestGeneratorController

io.mosip.kernel.*                         # auth adapter shims (TokenHelper, etc.)
io.mosip.biosdk.client.utils.Util         # classpath stub if needed
```

---

## 3. Boot & component scan

**Entry:** `IdRepositoryBootApplication`

```java
@SpringBootApplication
@ComponentScan(excludeFilters = { /* IdRepoDataSourceConfig, CacheConfig, cred configs, SwaggerConfig, VidRepoConfig */ })
@Import({ IdRepoLibraryConfig.class, IdRepoOpenApiConfig.class, IdRepoApiPathConfig.class,
          IdRepoKernelAuthHelperConfig.class, HttpModeScanConfiguration.class })
```

`HttpModeScanConfiguration` scans `io.mosip.idrepository`, `io.mosip.kernel`, `io.mosip.commons`, auth adapter package. Excludes duplicate configs already `@Import`ed and kernel crypto packages.

**Salt-generator is not on this classpath** — runs only in `id-repository-salt-generator`.

---

## 4. Controllers (external contracts — do not change paths)

| Controller | Base path |
|------------|-----------|
| `IdRepoController` | `/idrepository/v1/identity` |
| `IdRepoDraftController` | `/idrepository/v1/identity/draft` |
| `VidController` | `/idrepository/v1/vid` |
| `CredentialStoreController` | `/v1/credentialservice` |
| `CredentialRequestGeneratorController` | `/v1/credentialrequest` |
| `VidEventCallbackController` | WebSub callback |

---

## 5. Service-specific config

| Class | Purpose |
|-------|---------|
| `IdRepoOpenApiConfig` | Merged OpenAPI groups (identity, credential, credreq, VID) |
| `IdRepoApiPathConfig` | API path / callback registration |
| `IdRepoKernelAuthHelperConfig` | Kernel auth helper beans |
| `HttpModeScanConfiguration` | Component scan for HTTP deployment |
| `KernelAuthSecurityConfig` | Security filter chain |
| `IdRepoTaskSchedulerConfig` | Task scheduler for jobs pod |
| `IdRepoSelfTokenStartupConfig` | Self-token REST client warmup |

Configs **removed from service** (live in core): `RestTemplateConfig`, `ObjectMapperConfig`, `IdRepoFilter`.

---

## 6. K8s deployment

Same Docker image, two deployments:

| Deployment | `mosip.idrepo.jobs.enabled` | Replicas |
|------------|----------------------------|----------|
| id-repository-jobs | `true` | 1–3 |
| id-repository | `false` | 3–10 (HPA) |

Chart: `helm/identity` (repo root).

---

## 7. Local run

```bash
cd id-repository
mvn install -pl id-repository-service -am -DskipTests=true
java -jar id-repository-service/target/id-repository-service-*.jar
```

Requires `kernel-auth-adapter.jar` on classpath for auth. See `bootstrap.properties` for config server.

---

## 8. Agent rules (service-specific)

### Do

- Add new REST endpoints here as thin controllers delegating to core services.
- Keep `@PreAuthorize` SpEL aligned with role DTOs in core (`authorizedRoles`, `identityAuthorizedRoles`, etc.).
- Import domain wiring via `IdRepoLibraryConfig` — do not duplicate `@Bean` definitions from core.

### Do not

- Add business logic, entities, repositories, or batch jobs here.
- Duplicate core `@Configuration` classes.
- Add salt-generator or `io.mosip.idrepository.saltgenerator.*` code.
- Broaden scan to `io.mosip.*`.

---

## 9. Key files

| File | Purpose |
|------|---------|
| `IdRepositoryBootApplication.java` | Main entry |
| `bootstrap/IdRepositoryLauncher.java` | Alternate launcher |
| `config/HttpModeScanConfiguration.java` | Scan boundaries |
| `config/IdRepoOpenApiConfig.java` | OpenAPI |
| `config/KernelAuthSecurityConfig.java` | Security |
| `src/main/resources/bootstrap.properties` | Config server bootstrap |

---

*Last updated: 2026-07-07.*
