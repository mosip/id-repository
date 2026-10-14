# db_release_scripts (release / revoke)

Point-in-time SQL. Operator notes: `README.MD` (WinSCP **text** mode, `LOG_PATH`).

```text
<schema>/
├─ deploy.properties             host, MOSIP_DB_NAME, flags
├─ ddl/
├─ sql/{version}_release.sql + {version}_revoke.sql
└─ deploy.sh · revoke.sh
Run: bash deploy.sh deploy.properties 1.2.1 · bash revoke.sh deploy.properties 1.2.1 (per schema)
```

## Rules

- Ship `_release.sql` and `_revoke.sql` together. New tables go in `ddl/` **and** the versioned SQL.
- Create `LOG_PATH` first. `.properties` need a trailing newline and no padded values.
- Passwords via `SU_USER_PWD` / `SYSADMIN_PWD`. In logs, look for `ERROR` (ignore `NOTICE` / `SKIPPING`).
- `.sh` mechanics change only with DB-team review. A wrong version or DB name is destructive.
