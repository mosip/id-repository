# id-repository-salt-generator

K8s Job (`helm/idrepo-saltgen`). Fills salts in idrepo **and** idmap, then the JVM exits (no web). Package `io.mosip.idrepository.saltgenerator.*`.

```text
SaltGeneratorBootApplication
└─ DatabaseRouter (idrepo + idmap Hikari)
   └─ SaltGenerator
      └─ SaltJdbcWriter (ON CONFLICT DO NOTHING)
Config  mosip.kernel.salt-generator.{start-sequence,end-sequence,chunk-size}
```

## Rules

- Laptop: never run it; `local-dev-setup/deps/init.sql` seeds salts 0–999.
- Reuse core `EnvUtil`, `IdRepoHikariDataSourceFactory`, `IdRepoLogger`. Stay idempotent.
- Salt schema change → `db_scripts` for both DBs, then rerun the Job in the cluster.
