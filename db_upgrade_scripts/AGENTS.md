# db_upgrade_scripts (version hops)

Existing DBs only. `upgrade.sh` runs `sql/${CURRENT_VERSION}_to_${UPGRADE_VERSION}_${ACTION}.sql` (`ACTION` = `upgrade` | `rollback`) and first terminates backends on the target DB.

```text
<schema>/
├─ upgrade.properties   DB, CURRENT_VERSION, UPGRADE_VERSION, ACTION
├─ upgrade.sh
└─ sql/{from}_to_{to}_upgrade.sql + _rollback.sql
Order: idrepo → idmap → credential
```

## Rules

- Every hop ships upgrade **and** rollback.
- Released hops are frozen: add a new hop, never rewrite an old one.
- `db_scripts` DDL must equal the latest tip.
- Check `ACTION` and version strings (must match filenames) before running. Secrets via env.
