# AGENTS.md — `db_upgrade_scripts/`

> Incremental PostgreSQL upgrades between MOSIP versions. Each hop has paired upgrade and rollback SQL.  
> Parent guide: [repo root `AGENTS.md`](../AGENTS.md).  
> Related: [`db_scripts/AGENTS.md`](../db_scripts/AGENTS.md), [`db_release_scripts/AGENTS.md`](../db_release_scripts/AGENTS.md).

---

## 1. Purpose

Use this folder when moving an **existing** database from version `CURRENT` to `UPGRADE` (or rolling back). Prefer these scripts over greenfield `db_scripts` for production and long-lived environments.

Greenfield create → [`db_scripts/`](../db_scripts/AGENTS.md).  
Point-in-time release/revoke packaging → [`db_release_scripts/`](../db_release_scripts/AGENTS.md).

---

## 2. Layout

```
db_upgrade_scripts/
├── README.MD
├── mosip_idrepo/
│   ├── upgrade.sh
│   ├── upgrade.properties
│   └── sql/                    # {from}_to_{to}_upgrade.sql + _rollback.sql
├── mosip_idmap/
│   └── ...
└── mosip_credential/
    └── ...
```

### Naming convention

| Action | File pattern |
|--------|--------------|
| Upgrade | `sql/{from}_to_{to}_upgrade.sql` |
| Rollback | `sql/{from}_to_{to}_rollback.sql` |

Examples: `1.2.1.0_to_1.3.0_upgrade.sql`, `1.2.0.1-B1_to_1.2.0.1-B2_rollback.sql`.

`upgrade.sh` builds the path as `sql/${CURRENT_VERSION}_to_${UPGRADE_VERSION}_${ACTION}.sql` where `ACTION` is `upgrade` or `rollback`.

---

## 3. Properties (`upgrade.properties`)

| Variable | Meaning |
|----------|---------|
| `MOSIP_DB_NAME` | Target DB (`mosip_idrepo`, `mosip_idmap`, `mosip_credential`) |
| `DB_SERVERIP` / `DB_PORT` | Postgres endpoint |
| `SU_USER` / `SU_USER_PWD` | Superuser (password often set in env or properties) |
| `DEFAULT_DB_NAME` | Usually `postgres` |
| `ACTION` | `upgrade` or `rollback` |
| `CURRENT_VERSION` | Source version string (must match filename) |
| `UPGRADE_VERSION` | Target version string (must match filename) |

Passwords may also be supplied via environment; keep secrets out of git.

---

## 4. How to run

```bash
cd db_upgrade_scripts/mosip_idrepo
# set CURRENT_VERSION, UPGRADE_VERSION, ACTION=upgrade in upgrade.properties
# set DB_* and credentials
./upgrade.sh upgrade.properties
```

Apply all three schemas when a release touches more than one. Use a consistent dependency order if cross-schema FKs exist (typically idrepo → idmap → credential unless the hop docs say otherwise).

For rollback: set `ACTION=rollback` with the **same** `CURRENT_VERSION` / `UPGRADE_VERSION` pair as the upgrade that was applied (script name uses those two values).

---

## 5. Adding a new upgrade hop

1. Create both files under each affected schema’s `sql/`:
   - `{from}_to_{to}_upgrade.sql`
   - `{from}_to_{to}_rollback.sql`
2. Keep statements idempotent where practical; fail loudly on destructive mistakes.
3. Mirror structural changes into [`db_scripts/`](../db_scripts/AGENTS.md) so greenfield matches the new tip schema.
4. If the change is shipped as a MOSIP module release package, also update [`db_release_scripts/`](../db_release_scripts/AGENTS.md).
5. Never ship upgrade without rollback for production-bound DDL.

---

## 6. Agent rules

### Do

1. Always add **both** `_upgrade.sql` and `_rollback.sql` for every hop.
2. Match `CURRENT_VERSION` / `UPGRADE_VERSION` exactly to filenames.
3. Update all affected schemas in the same release.
4. Keep greenfield DDL (`db_scripts`) aligned with the latest upgraded tip.
5. Terminate/check active connections awareness — `upgrade.sh` terminates backends on the target DB before applying SQL.

### Do not

1. Skip rollback scripts.
2. Merge idrepo and idmap schemas or salt routing in upgrade SQL.
3. Edit older hop files to “fix forward” — add a new hop instead when the version is already released.
4. Run upgrade scripts without verifying `ACTION` and version strings first.

---

*Last updated: 2026-07-28.*
