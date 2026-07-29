# AGENTS.md — `db_scripts/`

> Greenfield PostgreSQL install for MOSIP ID-Repository. Creates databases, roles, grants, and DDL from scratch.  
> Parent guide: [repo root `AGENTS.md`](../AGENTS.md).  
> Related: [`db_upgrade_scripts/AGENTS.md`](../db_upgrade_scripts/AGENTS.md), [`db_release_scripts/AGENTS.md`](../db_release_scripts/AGENTS.md).

---

## 1. Purpose

Use this folder for **fresh** environments only (sandbox init, empty Postgres). Do **not** use these scripts to alter an existing production schema — use upgrade or release scripts instead.

Run order after schema create:

1. Deploy all three schemas via `deploy.sh`
2. Run salt-generator Job (`helm/idrepo-saltgen`) to populate salt rows
3. Start HTTP service (`helm/identity` / `deploy/idrepo`)

---

## 2. Layout

```
db_scripts/
├── README.md
├── mosip_idrepo/          # UIN, identity, credential_request_status, idrepo salts
├── mosip_idmap/           # VID + idmap salts
└── mosip_credential/      # Credential store + Spring Batch BATCH_* tables
```

Each schema folder:

| File / folder | Role |
|---------------|------|
| `deploy.sh` | Entry: drop → roles → DB → DDL → grants → optional DML |
| `deploy.properties` | Host, port, DB name, `DML_FLAG` |
| `db.sql` | `CREATE DATABASE` |
| `ddl.sql` | `\ir` includes of `ddl/*.sql` |
| `ddl/` | Per-table DDL |
| `role_dbuser.sql` / `grants.sql` | App role + privileges |
| `drop_db.sql` / `drop_role.sql` | Destructive reset (used by `deploy.sh`) |
| `dml.sql` / `dml/` | Optional seed data when `DML_FLAG=1` |

---

## 3. Schemas (do not merge)

| Schema | Purpose | Key tables |
|--------|---------|------------|
| `mosip_idrepo` | Identity / UIN | `uin`, `uin_*`, `credential_request_status`, `handle`, `uin_hash_salt`, `uin_encrypt_salt` |
| `mosip_idmap` | VID | `vid`, `vid_*`, `uin_hash_salt`, `uin_encrypt_salt` |
| `mosip_credential` | Credential + batch | `credential_transaction`, `BATCH_*` |

Salt tables exist in **both** `idrepo` and `idmap`. IDA uses a separate schema — do not assume IDA reads these salts.

---

## 4. How to run

```bash
export SU_USER_PWD=<postgres-superuser-password>
export DBUSER_PWD=<app-db-user-password>

cd db_scripts/mosip_idrepo
# edit deploy.properties (DB_SERVERIP, DB_PORT, MOSIP_DB_NAME, DML_FLAG)
./deploy.sh deploy.properties

cd ../mosip_idmap && ./deploy.sh deploy.properties
cd ../mosip_credential && ./deploy.sh deploy.properties
```

`deploy.sh` **drops** the existing DB and role first — never point it at a shared production instance by mistake.

Used automatically in [MOSIP Sandbox](https://docs.mosip.io/1.2.0/deployment/sandbox-deployment) DB init.

---

## 5. Adding schema changes

1. Add/edit DDL under `<schema>/ddl/` (one file per table or FK set).
2. Wire the file into `<schema>/ddl.sql` via `\ir ddl/...`.
3. Mirror the same change in:
   - [`db_upgrade_scripts/`](../db_upgrade_scripts/AGENTS.md) — paired `_upgrade.sql` / `_rollback.sql`
   - [`db_release_scripts/`](../db_release_scripts/AGENTS.md) — release DDL + `_release.sql` / `_revoke.sql` when shipping a version
4. If salt DDL changed: plan a salt-generator Job rerun after deploy.

---

## 6. Agent rules

### Do

1. Keep the three schemas separate — never merge idrepo and idmap salt DDL.
2. Include new tables/columns in `ddl/` **and** `ddl.sql`.
3. Update **all three** DB folders when a schema change ships.
4. Set `deploy.properties` and env passwords before running `deploy.sh`.
5. After salt-table changes, run `helm/idrepo-saltgen` before bringing up the HTTP service.

### Do not

1. Use `deploy.sh` against a live DB that must retain data (it drops DB/role).
2. Put seed/business data in DDL files — use DML when needed (`DML_FLAG=1`).
3. Skip upgrade/release companions for production-bound DDL.
4. Assume IDA consumes idrepo/idmap salt tables.

---

*Last updated: 2026-07-28.*
