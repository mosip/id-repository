# AGENTS.md — MOSIP ID-Repository (repo root)

> Infrastructure, database, deployment, and Helm guides for the **id-repository** git repo.  
> For Java / Maven application work, see [`id-repository/AGENTS.md`](id-repository/AGENTS.md).

---

## Guide index

| Area | Path | Guide |
|------|------|-------|
| **Java / Maven** (core, service, salt-gen) | `id-repository/` | [`id-repository/AGENTS.md`](id-repository/AGENTS.md) |
| **Local docker-compose** (IDA-style) | `id-repository/local-dev-setup/` | [`local-dev-setup/AGENTS.md`](id-repository/local-dev-setup/AGENTS.md) |
| Fresh DB install (DDL) | `db_scripts/` | [`db_scripts/AGENTS.md`](db_scripts/AGENTS.md) |
| Version upgrade SQL | `db_upgrade_scripts/` | [`db_upgrade_scripts/AGENTS.md`](db_upgrade_scripts/AGENTS.md) |
| Point-in-time release SQL | `db_release_scripts/` | [`db_release_scripts/AGENTS.md`](db_release_scripts/AGENTS.md) |
| K8s Helm charts | `helm/` | [`helm/AGENTS.md`](helm/AGENTS.md) |
| Cluster install scripts | `deploy/` | [`deploy/AGENTS.md`](deploy/AGENTS.md) |
| Functional API tests | `api-test/` | [`api-test/AGENTS.md`](api-test/AGENTS.md) |
| OpenAPI specs | `api-docs/` | [§ api-docs](#api-docs) |

**Java build:** `cd id-repository && mvn clean install` (JDK 21, Maven 3.9+).

---

## Repository layout (repo root)

```
id-repository/                    # git repo root (this AGENTS.md)
├── id-repository/                # Maven parent → see id-repository/AGENTS.md
│   └── local-dev-setup/          # Local docker-compose → local-dev-setup/AGENTS.md
├── db_scripts/                   # Greenfield DB create → db_scripts/AGENTS.md
├── db_upgrade_scripts/           # Incremental upgrades → db_upgrade_scripts/AGENTS.md
├── db_release_scripts/           # Release / revoke DDL → db_release_scripts/AGENTS.md
├── helm/                         # Kubernetes Helm charts → helm/AGENTS.md
├── deploy/                       # Shell installers → deploy/AGENTS.md
├── api-test/                     # E2E API test rig → api-test/AGENTS.md
├── api-docs/                     # OpenAPI YAML
└── contrib/                      # Reference configs (e.g. kernel auth BeanConfig)
```

### Databases (3 schemas — do not merge)

| Schema | Purpose |
|--------|---------|
| `mosip_idrepo` | UIN, identity, credential request status, idrepo salt tables |
| `mosip_idmap` | VID, idmap salt tables |
| `mosip_credential` | Credential store + Spring Batch `BATCH_*` metadata |

Salt tables (`uin_hash_salt`, `uin_encrypt_salt`) exist in **both** `idrepo` and `idmap`. Populate via salt-generator Job after DB deploy — not via HTTP service.

---

## `db_scripts`

Greenfield PostgreSQL install. Full agent guide: [`db_scripts/AGENTS.md`](db_scripts/AGENTS.md).

```
db_scripts/
├── mosip_idrepo/     # deploy.sh, ddl/, db.sql, role_dbuser.sql, grants.sql
├── mosip_idmap/
└── mosip_credential/
```

### Agent rules (summary)

- New tables/columns: add DDL under `<schema>/ddl/`, include in `<schema>/ddl.sql`.
- Keep schemas separate — never merge idrepo and idmap salt DDL.
- Run per schema: `cd db_scripts/mosip_idrepo && ./deploy.sh` (set `deploy.properties` first).
- Used automatically in [MOSIP Sandbox](https://docs.mosip.io/1.2.0/deployment/sandbox-deployment) DB init.
- After deploy, run salt-generator Job (`helm/idrepo-saltgen`) to populate salt rows.

---

## `db_upgrade_scripts`

Incremental upgrades between MOSIP versions. Full agent guide: [`db_upgrade_scripts/AGENTS.md`](db_upgrade_scripts/AGENTS.md).

```
db_upgrade_scripts/
├── mosip_idrepo/sql/       # e.g. 1.2.1.0_to_1.3.0_upgrade.sql
├── mosip_idmap/sql/
├── mosip_credential/sql/
└── */upgrade.sh + upgrade.properties
```

### Agent rules (summary)

- Add **both** upgrade and rollback for every schema change.
- Name files `{from}_to_{to}_upgrade.sql` / `_rollback.sql`.
- Run via `<schema>/upgrade.sh` after updating `upgrade.properties` (DB host, user, versions, `ACTION`).
- Apply all three schemas in dependency order when cross-schema FKs exist.

---

## `db_release_scripts`

Point-in-time release and revoke scripts per MOSIP module version. Full agent guide: [`db_release_scripts/AGENTS.md`](db_release_scripts/AGENTS.md).

```
db_release_scripts/
├── mosip_idrepo/     # deploy.sh, revoke.sh, ddl/, sql/
├── mosip_idmap/
└── mosip_credential/
```

### Agent rules (summary)

- New feature DDL for a release: add under `<schema>/ddl/` **and** wire into `<version>_release.sql`.
- Always provide matching `_revoke.sql` for rollback.
- Update `deploy.properties` before `deploy.sh` / `revoke.sh`.
- See `db_release_scripts/README.MD` for WinSCP encoding and log directory setup.

---

## `helm`

Kubernetes charts for id-repository components. Full agent guide: [`helm/AGENTS.md`](helm/AGENTS.md).

| Chart | Path | Deploys |
|-------|------|---------|
| **identity** | `helm/identity/` | Consolidated HTTP service (identity + credential + credreq + vid) |
| **idrepo-saltgen** | `helm/idrepo-saltgen/` | One-shot salt Job |

### Consolidated deployment model

| Workload | Chart / template | Notes |
|----------|------------------|-------|
| HTTP API | `helm/identity` | Single `id-repository-service` image |
| Salt population | `helm/idrepo-saltgen` | K8s **Job** only — run after DB deploy |

```console
helm repo add mosip https://mosip.github.io
helm -n idrepo install idrepo-saltgen mosip/idrepo-saltgen --wait --wait-for-jobs
helm -n idrepo install identity mosip/identity
```

### Agent rules (summary)

- Do not deploy salt-generator as a long-lived Deployment.
- Same Docker image family for the HTTP deployable; salt is a separate Job chart.
- Chart values: `helm/identity/values.yaml`, `helm/idrepo-saltgen/values.yaml`.
- After schema deploy, install/run saltgen Job before starting HTTP service.

---

## `deploy`

Shell installers for cluster-side deployment (wrap Helm / config). Full agent guide: [`deploy/AGENTS.md`](deploy/AGENTS.md).

```
deploy/
├── idrepo/                 # install.sh, delete.sh, restart.sh
├── idrepo-apitestrig/      # API test rig install
├── credential-feeder/      # credential feeder (legacy)
└── copy_cm_func.sh         # shared configmap helper
```

### Agent rules (summary)

- Primary id-repo install: `deploy/idrepo/install.sh`.
- Teardown: `deploy/idrepo/delete.sh`.
- Rolling restart: `deploy/idrepo/restart.sh`.
- Coordinate with `helm/` chart versions and config server labels.

---

## `api-test`

Functional end-to-end tests (REST Assured / TestNG). Full agent guide: [`api-test/AGENTS.md`](api-test/AGENTS.md).

### Agent rules (summary)

- Run after service deploy to validate external contracts (identity, VID, credential paths).
- Config: `api-test/src/main/resources/config/Idrepo.properties`.
- Install rig via `deploy/idrepo-apitestrig/install.sh` on cluster, or run locally per `api-test/README.md` / `api-test/AGENTS.md`.
- Do not change request/response shapes that IDA or partners depend on without updating tests.
- Handle / HBS / duplicate-chain details: [`api-test/CLAUDE.md`](api-test/CLAUDE.md).

---

## `api-docs`

OpenAPI YAML for credential and related services (`api-docs/credential-service.yaml`, etc.). Keep in sync with controller paths in `id-repository-service`.

---

## `contrib`

Reference implementations not shipped in main artifacts — e.g. `contrib/kernel-auth-defaultadapter/BeanConfig.java` (RestTemplate pool tuning).

---

## Cross-cutting agent rules (infra)

### Do

1. Change **all three** DB folders (`db_scripts`, `db_upgrade_scripts`, `db_release_scripts`) when schema changes affect a schema — see each folder’s AGENTS.md.
2. Run salt-generator Job after fresh DB or salt-table DDL changes — [`helm/AGENTS.md`](helm/AGENTS.md).
3. Keep Helm values aligned with consolidated single-image deployable (`id-repository-service`).
4. Point Java work to [`id-repository/AGENTS.md`](id-repository/AGENTS.md).
5. Point cluster install work to [`deploy/AGENTS.md`](deploy/AGENTS.md).
6. Point local docker-compose / WireMock / laptop stack work to [`id-repository/local-dev-setup/AGENTS.md`](id-repository/local-dev-setup/AGENTS.md).
7. Point functional API test work to [`api-test/AGENTS.md`](api-test/AGENTS.md).

### Do not

1. Merge `mosip_idrepo` and `mosip_idmap` schemas or salt routing.
2. Deploy salt-generator as a scaled Deployment.
3. Skip rollback scripts for upgrade/release changes.
4. Change REST paths or WebSub topics without updating `api-test` and documenting in Java AGENTS.
5. Use `docker compose restart id-repository-service` alone when deps must be healthy first — use `local-dev-setup/docker-compose/restart-idrepo.bat` (or `.sh`) / `docker compose up -d` instead.

---

## IDA note (infra)

IDA does **not** use id-repo salt tables. IDA schema is separate (`ida.uin_hash_salt` in id-authentication). Infra changes to idrepo/idmap salt tables affect id-repository crypto only — smoke-test identity retrieve + credential issuance after salt deploy.

---

*Last updated: 2026-08-11.*
