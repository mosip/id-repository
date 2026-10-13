# db_upgrade_scripts — version hops

Existing DBs only. Paired `{from}_to_{to}_upgrade.sql` + `_rollback.sql`. Greenfield: `db_scripts/`. Release: `db_release_scripts/`.

`upgrade.sh` loads `sql/${CURRENT_VERSION}_to_${UPGRADE_VERSION}_${ACTION}.sql` (`ACTION` = `upgrade` or `rollback`). Versions in `upgrade.properties` must match filenames exactly.

Run: edit `upgrade.properties` (DB, versions, `ACTION`), then `./upgrade.sh` per affected schema. Typical order: idrepo → idmap → credential. Script terminates backends on the target DB first.

## Rules

- Always ship both upgrade and rollback. Never skip rollback.
- Keep `db_scripts` DDL aligned with the latest tip. Released hops are frozen — add a new hop, do not rewrite old ones.
- Never merge idrepo/idmap schemas or salt routing.
- Verify `ACTION` + version strings before running. Secrets via env, not git.
