# db_release_scripts — named release / revoke

Point-in-time `{version}_release.sql` / `{version}_revoke.sql`. Operator notes: [`README.MD`](README.MD) (WinSCP **text** mode, `LOG_PATH`).

`bash deploy.sh deploy.properties 1.2.1` · `bash revoke.sh deploy.properties 1.2.1` — repeat per schema.

## Rules

- Always ship matching `_release.sql` and `_revoke.sql`. Wire new tables into `ddl/` **and** the versioned SQL.
- Keep `db_scripts` + `db_upgrade_scripts` aligned. Never merge idrepo/idmap.
- Set `deploy.properties` (host, `MOSIP_DB_NAME`, flags); create `LOG_PATH` first. Trailing newline in `.properties`; no padded values.
- Passwords via `SU_USER_PWD` / `SYSADMIN_PWD` env. Check logs for `ERROR` (ignore `NOTICE` / `SKIPPING`).
- Do not change `.sh` deploy mechanics without DB-team review. Wrong version arg or DB name is destructive.
