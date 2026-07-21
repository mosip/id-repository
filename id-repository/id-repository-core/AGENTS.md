# AGENTS.md — `id-repository-core`

> Shared Maven library. All business logic for identity, VID, credential store, and credential-request. Published JAR consumed by IDA and `id-repository-service`. **No salt-generator code here.**

---

## 1. Package layout

| Package | Purpose |
|---------|---------|
| `io.mosip.idrepository.core.*` | Cross-cutting: datasource, security, jobs, pipeline, validators, shared DTOs/constants |
| `io.mosip.idrepository.common.*` | Shared duplicates (OpenAPI models, crypto DTOs, `LoggerFileConstant`) |
| `io.mosip.idrepository.identity.*` | UIN / identity business logic, config, validators, services |
| `io.mosip.idrepository.vid.*` | VID business logic, config, validators, services |
| `io.mosip.idrepository.credential.store.*` | Credential issuance |
| `io.mosip.idrepository.credential.request.*` | Credential request queue + Spring Batch |

### Package conventions (per domain)

`config`, `constant`, `dto`, `entity`, `exception`, `repository`, `service`, `util`, `validator`.

Controllers live in **`id-repository-service` only** — not in core.

### Role beans (in core)

- `IdentityAuthorizedRolesDto` — `mosip.role.idrepo.identity`
- `CredentialAuthorizedRolesDto` — credential endpoints
- `CredReqAuthorizedRolesDto` — credreq endpoints
- `VidAuthorizedRolesDto` — `mosip.role.idrepo.vid`

---

## 2. Key entry points

| Class | Role |
|-------|------|
| `IdRepoLibraryConfig` | Central wiring: DS, cache, providers, jobs import hub |
| `IdRepoDataSourceConfig` | Primary PU (`mosip_idrepo`) |
| `IdRepoJobsConfiguration` | All scheduled jobs |
| `IdRepoBatchConfig` | Spring Batch on credential DB |
| `InProcessCredentialClient` | credreq → credential (in-process) |
| `InProcessIdentityClient` | credential → identity (in-process) |
| `CredentialStatusManager` | Identity credential status @Scheduled handler |
| `IdRepoValidationMessageHelper` | Config-driven validation error messages |

---

## 3. Multi-datasource architecture

| PU | Bean names | DB | Entities |
|----|-----------|-----|----------|
| PU1 `@Primary` | `idRepoDataSource`, `entityManagerFactory`, `transactionManager` | `mosip_idrepo` | Uin, UinHistory, UinDraft, CredentialRequestStatus, UinHashSalt, UinEncryptSalt, Handle |
| PU2 (VID) | idmap datasource via `VidRepoConfig` | `mosip_idmap` | Vid, VidUinHashSalt, VidUinEncryptSalt |
| PU3 | `credentialDataSource`, `credentialEMF`, `credentialTransactionManager` | `mosip_credential` | CredentialEntity + Spring Batch `BATCH_*` |

### Transaction routing

| Annotation | Target |
|------------|--------|
| `@Transactional` (default) | PU1 idrepo |
| `@Transactional("credentialTransactionManager")` | PU3 credential |
| Per-row `TransactionTemplate` | Batch tasklets — **not** class-level `@Transactional` |

**Critical:** `CredentialStatusManager` must use per-row transactions to avoid rollback amplification.

### Salt routing (identity vs VID)

`UinHashSaltRepo` / `UinEncryptSaltRepo` on idrepo PU; `VidUinHashSaltRepo` / `VidUinEncryptSaltRepo` on idmap PU. Mis-routing causes silent crypto failure at scale.

---

## 4. IDA compatibility

[ID Authentication](https://github.com/mosip/id-authentication/tree/develop) is the primary downstream consumer. IDA does **not** use id-repo salt tables.

### Core classes referenced by IDA (do not rename/remove without IDA release)

- DTOs: `CredentialRequestIdsDto`, `AuthtypeStatus`, `AuthTypeStatusEventDTO`, `RestRequestDTO`
- Constants: `IdRepoConstants`, `IdRepoErrorConstants`, `IDAEventType`
- Utilities: `RestUtil`, `SaltUtil`, `RestRequestBuilder`, `IdRepoLogger`
- Exceptions: `RestServiceException`, `IdRepoRetryException`, `AuthenticationException`

### Must not change (IDA breaks)

- REST paths and WebSub topics (see parent AGENTS.md)
- Credential / Datashare payload shape
- `CredentialRequestIdsDto` and `AuthTypeStatusEventDTO` JSON field names
- Error codes IDA matches (e.g. `IDR-CRG-009`)

---

## 5. Internal pipeline (in-process)

```
CredentialStatusManager (@Scheduled)
    → CredentialRequestService (in-process)
        → CredentialStoreService (in-process)
            → IdentityService.retrieve (in-process)
            → PMS, KeyManager, Datashare, WebSub (HTTP)
```

`RestServicesConstants` config keys for remaining outbound HTTP:

- `mosip.idrepo.credential.request` / `credential-request-v2`
- `mosip.idrepo.retrieve-by-uin`
- `mosip.idrepo.vid-service` (if external VID deployment)

---

## 6. Bean collision registry

Set `spring.main.allow-bean-definition-overriding=false`.

### Phase 1 (identity + credential) — 12 items

| Bean | Resolution |
|------|------------|
| `cacheManager()` | Single `CaffeineCacheManager` in `IdRepoLibraryConfig` |
| `IdRepoSecurityManager` | One `@Primary` from core |
| `OpenApiProperties` / `groupedOpenApi()` | Merged in service `IdRepoOpenApiConfig` |
| `getRestRequestBuilder()` | Single `@Primary` |
| `RestUtil` | `CredentialStoreRestUtil`, `CredReqRestUtil` |
| `DummyPartnerCheckUtil`, `RestHelper`, `AuditHelper` | Core only |
| `AfterburnerModule` | Single bean |
| `PartnerCacheUpdatingSchedulerConfig` | Deduplicate to one |

### Phase 2 (credreq) — 10 additional

| Bean | Resolution |
|------|------------|
| `RestUtil` (credreq) | `@Qualifier("credReqRestUtil")` |
| `SchedulingConfigurer` | Gate on `mosip.idrepo.jobs.enabled` |
| Spring Batch `JobRepository` | Explicit on `credentialDataSource` |
| `entityManagerFactory` | Rename credreq EMF to `credentialEntityManagerFactory` |
| `CredentialTransactionInterceptor` | PU3 only |
| `ForkJoinPool` in `CredentialItemTasklet` | `credential.batch.thread.count` (default 10) |

---

## 7. Scheduled jobs

| Job | Class | Gate |
|-----|-------|------|
| Credential status handler | `IdentityScheduleConfig` | `mosip.idrepo.jobs.enabled` |
| Credential batch | `CredentialScheduleJobConfiguration` | jobs flag |
| Credential reprocess | `CredentialScheduleJobConfiguration` | jobs flag |
| Partner cache refresh | `PartnerCacheUpdateSchedulerConfig` | always (single instance) |

Batch correctness: `SELECT ... FOR UPDATE SKIP LOCKED` in `CredentialRequestStatusRepo`.

---

## 8. Validation (recent work)

`IdRepoValidationMessageHelper` builds descriptive errors from config:

| Config | Used for |
|--------|----------|
| `mosip.idrepo.vid.id.*` → `vidIdMap` | VID request `id` validation |
| `mosip.idrepo.vid.application.version` | VID `version` |
| `mosip.idrepo.identity.application.version` | Identity `version` |
| `mosip.idrepo.vid.allowedstatus` | VID status |
| `mosip.idrepo.identity.uin-status` | UIN status |

Wired in: `BaseIdRepoValidator`, `VidRequestValidator`, `IdRequestValidator`, `VidServiceImpl`.

---

## 9. Performance notes

| Optimization | Location / config |
|--------------|-------------------|
| Caffeine cache | `IdRepoLibraryConfig.cacheManager()` |
| HikariCP per PU | `IdRepoHikariDataSourceFactory` |
| In-process hops | `InProcessCredentialClient`, `InProcessIdentityClient` |
| Batch parallelism | `credential.batch.thread.count` |
| SKIP LOCKED | `CredentialRequestStatusRepo` |

**Anti-patterns:** class-level `@Transactional` on `CredentialStatusManager`; `DriverManagerDataSource` under load; internal HTTP after merge.

---

## 10. Key files

| Concern | File |
|---------|------|
| Library wiring | `core/config/IdRepoLibraryConfig.java` |
| Primary datasource | `core/config/IdRepoDataSourceConfig.java` |
| Cache | `core/config/CacheConfig.java` |
| Credential batch | `credential/request/batch/config/BatchConfiguration.java` |
| Batch tasklet | `credential/request/batch/config/CredentialItemTasklet.java` |
| Credential issuance | `credential/store/service/CredentialStoreService.java` |
| Status manager | `core/manager/CredentialStatusManager.java` |
| REST constants | `core/constant/RestServicesConstants.java` |
| Validation helper | `core/validator/IdRepoValidationMessageHelper.java` |
| VID service | `vid/service/impl/VidServiceImpl.java` |
| Identity validator | `identity/validator/IdRequestValidator.java` |

---

## 11. Agent rules (core-specific)

### Do

- Add new business logic here under the correct domain package.
- Keep `io.mosip.idrepository.core.*` stable for IDA.
- Use `@Primary` / `@Qualifier` for ambiguous beans.
- Run `mvn test -pl id-repository-core` after changes.

### Do not

- Add controllers, boot applications, or salt-generator code.
- Rename/remove IDA-facing classes without coordination.
- Put `io.mosip.idrepository.saltgenerator.*` in this module.

---

*Last updated: 2026-07-07.*
