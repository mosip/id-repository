# AGENTS.md — `db_release_scripts/`

> Point-in-time release and revoke DDL per MOSIP module version (e.g. `1.2.1_release.sql` / `1.2.1_revoke.sql`).  
> Parent guide: [repo root `AGENTS.md`](../AGENTS.md).  
> Related: [`db_scripts/AGENTS.md`](../db_scripts/AGENTS.md), [`db_upgrade_scripts/AGENTS.md`](../db_upgrade_scripts/AGENTS.md).  
> Operator notes: [`README.MD`](README.MD) (WinSCP encoding, log dirs, property keys).

---

## 1. Purpose

Package database changes for a **named MOSIP release**: apply (`deploy.sh`) or undo (`revoke.sh`) versioned SQL on an existing database. Complements:

| Folder | When |
|--------|------|
| `db_scripts` | Empty DB create |
| `db_upgrade_scripts` | Version-to-version hops |
| `db_release_scripts` | Release/revoke packaging for a module version |

When schema changes ship, update **all three** DB folders as needed.

---

## 2. Layout

```
db_release_scripts/
├── README.MD
├── mosip_idrepo/
│   ├── deploy.sh              # apply release SQL (version arg)
│   ├── revoke.sh              # revoke release SQL (version arg)
│   ├── deploy.properties
│   ├── ddl/                   # table DDL bundled for the release
│   └── sql/                   # {version}_release.sql / {version}_revoke.sql
├── mosip_idmap/
│   └── ...
└── mosip_credential/
    └── ...
```

### Versioned SQL naming

| Action | Example |
|--------|---------|
| Release | `sql/1.2.1_release.sql` |
| Revoke | `sql/1.2.1_revoke.sql` |

Pass the version (e.g. `1.2.1`) as the second argument to `deploy.sh` / `revoke.sh`.

---

## 3. Properties (`deploy.properties`)

Key variables (see [`README.MD`](README.MD) for full list):

| Variable | Meaning |
|----------|---------|
| `DB_SERVERIP` / `DB_PORT` | Postgres endpoint |
| `SU_USER` | Superuser (password via `SU_USER_PWD` env) |
| `SYSADMIN_USER` | Sysadmin role used by scripts |
| `DEFAULT_DB_NAME` | Usually `postgres` |
| `MOSIP_DB_NAME` | `mosip_idrepo` / `mosip_idmap` / `mosip_credential` |
| `BASEPATH` | Path to scripts on the DB deploy host |
| `LOG_PATH` | Directory for deployment logs (must exist) |
| `ALTER_SCRIPT_FLAG` / `REVOKE_SCRIPT_FLAG` | `0`/`1` whether alter/revoke SQL runs |
| `ALTER_SCRIPT_FILENAME` / `REVOKE_SCRIPT_FILENAME` | Filenames under `sql/` (legacy property names; versioned files still live under `sql/`) |

Ensure a single trailing empty line in `.properties` and no leading/trailing spaces on values.

```bash
export SU_USER_PWD=<postgres-password>
export SYSADMIN_PWD=<sysadmin-password>
```

---

## 4. How to run

Prerequisites: `psql` on the deploy host; log directory created; scripts copied with **text** transfer mode if using WinSCP (see README).

```bash
cd db_release_scripts/mosip_idrepo
# edit deploy.properties
bash deploy.sh deploy.properties 1.2.1

# revoke only if needed
bash revoke.sh deploy.properties 1.2.1
```

Repeat for `mosip_idmap` and `mosip_credential` when those schemas are part of the release. Check logs under `LOG_PATH` for `ERROR` (ignore expected `NOTICE` / `SKIPPING`).

---

## 5. Adding release DDL

1. Add new/changed table scripts under `<schema>/ddl/` when the release introduces tables.
2. Add `sql/{version}_release.sql` with alters / `\ir` includes for the release.
3. Add matching `sql/{version}_revoke.sql` that undoes the release safely.
4. Keep greenfield [`db_scripts`](../db_scripts/AGENTS.md) and hop scripts [`db_upgrade_scripts`](../db_upgrade_scripts/AGENTS.md) aligned.
5. Do not change shell scripts unless the deployment tooling itself needs a fix — coordinate with DB team for script engine changes.

---

## 6. Agent rules

### Do

1. Always ship matching `_release.sql` and `_revoke.sql` for a version.
2. Wire new feature DDL into both `ddl/` and the versioned release SQL.
3. Update `deploy.properties` (host, paths, flags) before `deploy.sh` / `revoke.sh`.
4. Create `LOG_PATH` before deploy; validate logs after.
5. Keep schemas separate — never merge idrepo and idmap.

### Do not

1. Skip revoke scripts for a release that has release SQL.
2. Modify `.sql` / `.sh` casually without DB-team review when changing deploy mechanics.
3. Transfer scripts via WinSCP in binary mode (breaks line endings / encoding).
4. Point release deploy at the wrong `MOSIP_DB_NAME` or version argument.

---

*Last updated: 2026-07-28.*
