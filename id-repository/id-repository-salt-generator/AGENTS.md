# id-repository-salt-generator

K8s **Job** (`helm/idrepo-saltgen`). Fills `uin_hash_salt` / `uin_encrypt_salt` in **idrepo and idmap**. Package `io.mosip.idrepository.saltgenerator.*`. Web: NONE; JVM exits.

IDA has its own `ida.uin_hash_salt` — unrelated.

Flow: `SaltGeneratorBootApplication` → `DatabaseRouter` (idrepo+idmap Hikari) → `SaltGenerator` → `SaltJdbcWriter` (`ON CONFLICT DO NOTHING`).

Config: `mosip.kernel.salt-generator.{start-sequence,end-sequence,chunk-size}`.

**Laptop:** do not run this Job — `local-dev-setup/deps/init.sql` seeds 0–999.

## Rules

- Keep salt code here. Reuse core `EnvUtil`, `IdRepoHikariDataSourceFactory`, `IdRepoLogger`.
- Stay idempotent. Never add this package to core or the HTTP JVM.
- Schema changes → `db_scripts` **and both** DBs, then rerun the Job in cluster.
