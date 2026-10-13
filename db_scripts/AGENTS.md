# db_scripts — greenfield Postgres

Fresh install only (`deploy.sh` **drops** DB+role). Hops: `db_upgrade_scripts/`. Release: `db_release_scripts/`.

Order: deploy three schemas → saltgen Job → HTTP service.

| Schema | Holds |
|--------|-------|
| `mosip_idrepo` | UIN, handle, cred-request status, idrepo salts |
| `mosip_idmap` | VID + idmap salts |
| `mosip_credential` | Credential + `BATCH_*` |

Run: set `SU_USER_PWD` / `DBUSER_PWD`, edit `deploy.properties`, `./deploy.sh` in each schema folder.

## Rules

- New DDL in `<schema>/ddl/` **and** `\ir` in `ddl.sql`. Mirror in upgrade + release folders.
- Never merge idrepo/idmap salt DDL. Seed → DML (`DML_FLAG=1`), not DDL.
- After salt DDL: rerun `helm/idrepo-saltgen`. IDA does not use these salts.
- Never point `deploy.sh` at a live DB that must keep data.
