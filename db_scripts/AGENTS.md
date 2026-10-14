# db_scripts (greenfield)

Fresh install only: `deploy.sh` **drops** the DB and role. Order: three schemas → saltgen Job → HTTP service.

```text
mosip_idrepo/      UIN, handle, credential-request status, idrepo salts
mosip_idmap/       VID, idmap salts
mosip_credential/  credential, BATCH_*
└─ each: ddl/*.sql + ddl.sql (\ir list) + db/role/grants/drop SQL + deploy.properties + deploy.sh
```

Run per schema: set `SU_USER_PWD` / `DBUSER_PWD`, edit `deploy.properties`, `./deploy.sh`.

## Rules

- New DDL file in `<schema>/ddl/` **and** an `\ir` line in `ddl.sql`.
- Seed data → DML (`DML_FLAG=1`), never DDL.
- After salt DDL, rerun `helm/idrepo-saltgen`.
- Never point `deploy.sh` at a DB whose data must survive.
