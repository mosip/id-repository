# AGENTS.md — Maven Parent (`id-repository/`)

> Java application code for consolidated MOSIP ID-Repository (JDK 21, Spring Boot 2.0.2).  
> For **database, Helm, deploy, and api-test** work, see the [repo root `AGENTS.md`](../AGENTS.md) and folder guides: [`db_scripts`](../db_scripts/AGENTS.md), [`db_upgrade_scripts`](../db_upgrade_scripts/AGENTS.md), [`db_release_scripts`](../db_release_scripts/AGENTS.md), [`helm`](../helm/AGENTS.md), [`deploy`](../deploy/AGENTS.md).

---

## Module guides

| Module | Role | Agent guide |
|--------|------|-------------|
| `id-repository-core` | Shared library — `io.mosip.idrepository.core.*` only (IDA API) | [`id-repository-core/AGENTS.md`](id-repository-core/AGENTS.md) |
| `id-repository-service` | HTTP deployable — identity, VID, credential + controllers | [`id-repository-service/AGENTS.md`](id-repository-service/AGENTS.md) |
| `id-repository-salt-generator` | One-shot K8s Job — salt tables only | [`id-repository-salt-generator/AGENTS.md`](id-repository-salt-generator/AGENTS.md) |

---

## 1. Project overview

| Service | Module | Port | Servlet path | DB |
|---------|--------|------|--------------|-----|
| ID-Repository (all APIs) | `id-repository-service` | 8090 | `/idrepository/v1/identity`, `/idrepository/v1/vid`, `/v1/credentialservice/`, `/v1/credentialrequest/` | `mosip_idrepo` + `mosip_idmap` + `mosip_credential` |
| Salt generator (K8s Job) | `id-repository-salt-generator` | — | — | `idrepo` + `idmap` |
| Shared library | `id-repository-core` | — | — | business logic (published JAR for IDA) |

**Config:** Spring Cloud Config (`bootstrap.properties` per module).  
**Auth:** `kernel-auth-adapter` (requires `kernel-auth-adapter.jar` on classpath for local runs).

---

## 2. Architecture (post-merge)

```
id-repository-service (deployable)
    ├── depends on id-repository-core (library — core.* only)
    ├── io.mosip.idrepository.identity.*
    ├── io.mosip.idrepository.vid.*
    ├── io.mosip.idrepository.credential.*
    ├── io.mosip.idrepository.pipeline.* (in-process adapters)
    └── io.mosip.idrepository.manager.* (credential orchestration)

id-repository-core (library)
    └── io.mosip.idrepository.core.* only
```

### What must NOT change externally

- REST URLs: `/idrepository/v1/identity/*`, `/idrepository/v1/vid/*`, `/v1/credentialservice/*`, `/v1/credentialrequest/*`
- WebSub topics: `{partnerId}/CREDENTIAL_ISSUED`, `CREDENTIAL_STATUS_UPDATE`
- Database schemas: `idrepo`, `idmap`, `credential`
- Keycloak client: `mosip-idrepo-client`

### IDA compatibility (summary)

[ID Authentication](https://github.com/mosip/id-authentication/tree/develop) consumes id-repository via WebSub, Datashare, and REST — **not** id-repo salt tables. Keep `io.mosip.idrepository.core.*` API stable when publishing core. Full IDA contract list: [`id-repository-core/AGENTS.md`](id-repository-core/AGENTS.md#ida-compatibility).

---

## 3. Maven module layout

```
id-repository/                         # Maven parent (this folder)
├── id-repository-core/                # LIBRARY
├── id-repository-service/             # HTTP deployable
└── id-repository-salt-generator/      # Salt K8s Job
```

Repo-root folders: [../AGENTS.md](../AGENTS.md) — detailed guides under [`db_scripts`](../db_scripts/AGENTS.md), [`helm`](../helm/AGENTS.md), [`deploy`](../deploy/AGENTS.md), etc.

---

## 4. Consolidation status (recent work)

| Area | Status |
|------|--------|
| Single HTTP deployable | `id-repository-service` hosts identity, VID, credential, credreq |
| Core as library | `id-repository-core` = `io.mosip.idrepository.core.*` only (upstream-aligned) |
| Domain in service | `identity`, `vid`, `credential`, `common` moved to `id-repository-service` |
| Pipeline in service | `io.mosip.idrepository.pipeline.*`, `io.mosip.idrepository.manager.*` |
| Salt isolation | **All** salt code in `id-repository-salt-generator` (`io.mosip.idrepository.saltgenerator.*`), **not** in core |
| Duplicate configs removed | `RestTemplateConfig`, `ObjectMapperConfig`, `IdRepoFilter` removed from service (live in core) |
| Role DTOs in core | `IdentityAuthorizedRolesDto`, `CredentialAuthorizedRolesDto`, `VidAuthorizedRolesDto` |
| Config-driven validation | `IdRepoValidationMessageHelper` — descriptive errors from config (VID ids, versions, statuses) |
| In-process pipeline | credreq → credential → identity via `InProcessCredentialClient` / `InProcessIdentityClient` |

---

## 5. Agent working rules

### Do

1. Keep **business logic in core**; service = boot + controllers + HTTP/security config only.
2. Keep **salt logic in salt-generator** only — never add `io.mosip.idrepository.saltgenerator.*` to core.
3. Preserve external contracts (URLs, WebSub, schemas, IDA-facing core APIs).
4. Set `spring.main.allow-bean-definition-overriding=false`; use `@Primary` / `@Qualifier`.
5. Keep credential issuance synchronous in-process (no batch / jobs flag).
6. Run `mvn test` in affected modules; api-test for integration.

### Do not

1. Run salt-generator inside the long-lived HTTP JVM.
2. Use class-level `@Transactional` on credential status managers.
3. Broaden `ComponentScan` to `io.mosip.*`.
4. Put salt-only code back into `id-repository-core`.
5. Reintroduce `mosip.idrepo.jobs.enabled` or Spring Batch credential jobs.

### Phase checklist

**Phase 0 — HOST prep**
- [ ] `spring.main.allow-bean-definition-overriding=false`
- [ ] Mark idrepo beans `@Primary`
- [ ] Synchronous credential pipeline (no jobs flag)
- [ ] Baseline credential latency benchmark

**Phase 1 — + credential-service**
- [ ] Resolve 12 bean collisions (see core AGENTS.md)
- [ ] Verify `/v1/credentialservice/issue`

**Phase 2 — + credreq-gen**
- [ ] PU3 on `mosip_credential`
- [ ] In-process `CredentialIssuanceProcessor` → `CredentialStoreService`
- [ ] Helm: single image / single HTTP deployment — see [../helm/AGENTS.md](../helm/AGENTS.md)

---

## 6. K8s deployment

| Deployment | Replicas | Purpose |
|------------|----------|---------|
| id-repository (`helm/identity`) | HPA as needed | HTTP + synchronous credential pipeline |

Salt: separate Job — chart details in [`../helm/AGENTS.md`](../helm/AGENTS.md), installers in [`../deploy/AGENTS.md`](../deploy/AGENTS.md); Java entry in [`id-repository-salt-generator/AGENTS.md`](id-repository-salt-generator/AGENTS.md).

---

## 7. Rollback

Each phase is a discrete git revert. Production rollback = redeploy previous release tag. Use `git log --follow` for origin tracing after code-copy merges.

---

*Last updated: 2026-07-07. Align with HLD_idrepo_consolidation.md v1.0.*
