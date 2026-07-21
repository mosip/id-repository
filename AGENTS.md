# AGENTS.md — MOSIP ID-Repository (repo root)

> Infrastructure, database, deployment, and Helm guides for the **id-repository** git repo.  
> For Java / Maven application work, see [`id-repository/AGENTS.md`](id-repository/AGENTS.md).

---

## Guide index

| Area | Path | Guide |
|------|------|-------|
| **Java / Maven** (core, service, salt-gen) | `id-repository/` | [`id-repository/AGENTS.md`](id-repository/AGENTS.md) |
| Fresh DB install (DDL) | `db_scripts/` | [§ db_scripts](#db_scripts) |
| Version upgrade SQL | `db_upgrade_scripts/` | [§ db_upgrade_scripts](#db_upgrade_scripts) |
| Point-in-time release SQL | `db_release_scripts/` | [§ db_release_scripts](#db_release_scripts) |
| K8s Helm charts | `helm/` | [§ helm](#helm) |
| Cluster install scripts | `deploy/` | [§ deploy](#deploy) |
| Functional API tests | `api-test/` | [§ api-test](#api-test) |
| OpenAPI specs | `api-docs/` | [§ api-docs](#api-docs) |

**Java build:** `cd id-repository && mvn clean install` (JDK 21, Maven 3.9+).

---

## Repository layout (repo root)

```
id-repository/                    # git repo root (this AGENTS.md)
├── id-repository/                # Maven parent → see id-repository/AGENTS.md
├── db_scripts/                   # Greenfield DB create (3 schemas)
├── db_upgrade_scripts/           # Incremental version upgrades + rollback
├── db_release_scripts/           # Release / revoke DDL per MOSIP version
├── helm/                         # Kubernetes Helm charts
├── deploy/                       # Shell installers for cluster components
├── api-test/                     # End-to-end API test rig
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

Greenfield PostgreSQL install. One folder per schema:

```
db_scripts/
├── mosip_idrepo/     # deploy.sh, ddl/, db.sql, role_dbuser.sql, grants.sql
├── mosip_idmap/
└── mosip_credential/
```

### Agent rules

- New tables/columns: add DDL under `<schema>/ddl/`, include in `<schema>/ddl.sql`.
- Keep schemas separate — never merge idrepo and idmap salt DDL.
- Run per schema: `cd db_scripts/mosip_idrepo && ./deploy.sh` (set `deploy.properties` first).
- Used automatically in [MOSIP Sandbox](https://docs.mosip.io/1.2.0/deployment/sandbox-deployment) DB init.

### Key salt DDL

| Schema | Tables |
|--------|--------|
| `idrepo` | `uin_hash_salt`, `uin_encrypt_salt` |
| `idmap` | `uin_hash_salt`, `uin_encrypt_salt` |

After deploy, run salt-generator Job (`helm/idrepo-saltgen`) to populate salt rows.

---

## `db_upgrade_scripts`

Incremental upgrades between MOSIP versions. Paired `_upgrade.sql` / `_rollback.sql` per hop.

```
db_upgrade_scripts/
├── mosip_idrepo/sql/       # e.g. 1.2.1.0_to_1.3.0_upgrade.sql
├── mosip_idmap/sql/
├── mosip_credential/sql/
└── */upgrade.sh + upgrade.properties
```

### Agent rules

- Add **both** upgrade and rollback for every schema change.
- Name files `{from}_to_{to}_upgrade.sql` / `_rollback.sql`.
- Run via `<schema>/upgrade.sh` after updating `upgrade.properties` (DB host, user, log path).
- Apply all three schemas in dependency order when cross-schema FKs exist.

---

## `db_release_scripts`

Point-in-time release and revoke scripts per MOSIP module version (e.g. `1.2.1_release.sql`, `1.2.1_revoke.sql`).

```
db_release_scripts/
├── mosip_idrepo/     # deploy.sh, revoke.sh, ddl/, sql/
├── mosip_idmap/
└── mosip_credential/
```

### Agent rules

- New feature DDL for a release: add under `<schema>/ddl/` **and** wire into `<version>_release.sql`.
- Always provide matching `_revoke.sql` for rollback.
- Update `deploy.properties` before `deploy.sh` / `revoke.sh`.
- See `db_release_scripts/README.MD` for WinSCP encoding and log directory setup.

---

## `helm`

Kubernetes charts for id-repository components.

| Chart | Path | Deploys |
|-------|------|---------|
| **idrepo** (umbrella) | `helm/idrepo/` | Modular install entry |
| **identity** | `helm/identity/` | Consolidated HTTP service (identity + credential + credreq) |
| **idrepo-saltgen** | `helm/idrepo-saltgen/` | One-shot salt Job |

### Consolidated deployment model

| Workload | Chart / template | Notes |
|----------|------------------|-------|
| HTTP API (no jobs) | `helm/identity` | `mosip.idrepo.jobs.enabled=false`, HPA 3–10 |
| HTTP + batch jobs | `helm/identity` | `mosip.idrepo.jobs.enabled=true`, replicas 1–3 |
| Salt population | `helm/idrepo-saltgen` | K8s **Job** only — run after DB deploy |

```console
helm repo add mosip https://mosip.github.io
helm -n idrepo install my-release mosip/idrepo
```

### Agent rules

- Do not deploy salt-generator as a long-lived Deployment.
- Same Docker image for HTTP and jobs pods; split via env (`mosip.idrepo.jobs.enabled`).
- Chart values: `helm/identity/values.yaml`, `helm/idrepo-saltgen/values.yaml`.
- After schema deploy, install/run saltgen Job before starting HTTP service.

---

## `deploy`

Shell installers for cluster-side deployment (wrap Helm / config).

```
deploy/
├── idrepo/                 # install.sh, delete.sh, restart.sh
├── idrepo-apitestrig/      # API test rig install
├── credential-feeder/      # credential feeder (legacy)
└── copy_cm_func.sh         # shared configmap helper
```

### Agent rules

- Primary id-repo install: `deploy/idrepo/install.sh`.
- Teardown: `deploy/idrepo/delete.sh`.
- Rolling restart: `deploy/idrepo/restart.sh`.
- Coordinate with `helm/` chart versions and config server labels.

---

## `api-test`

Functional end-to-end tests (Karate-style resources under `src/main/resources/idRepository/`).

### Agent rules

- Run after service deploy to validate external contracts (identity, VID, credential paths).
- Config: `api-test/src/main/resources/config/Idrepo.properties`.
- Install rig via `deploy/idrepo-apitestrig/install.sh` on cluster, or run locally per `api-test/README.md`.
- Do not change request/response shapes that IDA or partners depend on without updating tests.

---

## `api-docs`

OpenAPI YAML for credential and related services (`api-docs/credential-service.yaml`, etc.). Keep in sync with controller paths in `id-repository-service`.

---

## `contrib`

Reference implementations not shipped in main artifacts — e.g. `contrib/kernel-auth-defaultadapter/BeanConfig.java` (RestTemplate pool tuning).

---

## Cross-cutting agent rules (infra)

### Do

1. Change **all three** DB folders (`db_scripts`, `db_upgrade_scripts`, `db_release_scripts`) when schema changes affect a schema.
2. Run salt-generator Job after fresh DB or salt-table DDL changes.
3. Keep Helm values aligned with consolidated single-image deployable (`id-repository-service`).
4. Point Java work to [`id-repository/AGENTS.md`](id-repository/AGENTS.md).

### Do not

1. Merge `mosip_idrepo` and `mosip_idmap` schemas or salt routing.
2. Deploy salt-generator as a scaled Deployment.
3. Skip rollback scripts for upgrade/release changes.
4. Change REST paths or WebSub topics without updating `api-test` and documenting in Java AGENTS.

---

## IDA note (infra)

IDA does **not** use id-repo salt tables. IDA schema is separate (`ida.uin_hash_salt` in id-authentication). Infra changes to idrepo/idmap salt tables affect id-repository crypto only — smoke-test identity retrieve + credential issuance after salt deploy.

---

*Last updated: 2026-07-07.*
