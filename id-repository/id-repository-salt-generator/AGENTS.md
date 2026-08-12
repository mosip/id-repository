# AGENTS.md — `id-repository-salt-generator`

> One-shot Kubernetes Job that populates `uin_hash_salt` and `uin_encrypt_salt` in **both** `mosip_idrepo` and `mosip_idmap`. **Not** part of the HTTP service or `id-repository-core` library.

---

## 1. Role

| Item | Value |
|------|-------|
| Deploy as | K8s Job (`helm/idrepo-saltgen`) |
| Package | `io.mosip.idrepository.saltgenerator.*` |
| Entry point | `SaltGeneratorBootApplication` |
| Depends on core | `EnvUtil`, `IdRepoLogger`, `IdRepoHikariDataSourceFactory` only |
| Web mode | `NONE` (batch exits after run) |

IDA and other MOSIP modules **do not** consume id-repo salt tables. IDA maintains its own `ida.uin_hash_salt`.

---

## 2. Package layout

```
io.mosip.idrepository.saltgenerator/
├── SaltGeneratorBootApplication.java     # main() — exits JVM after job
├── SaltGeneratorRunner.java              # CommandLineRunner
├── config/
│   └── SaltGeneratorConfiguration.java
├── constant/
│   └── SaltGeneratorConstant.java
├── service/
│   ├── SaltGenerator.java              # chunked batch logic
│   ├── SaltJdbcWriter.java             # JDBC batch inserts
│   ├── DatabaseRouter.java             # idrepo + idmap datasources
│   └── SaltRow.java
└── entity/                               # JPA entities (reference; writer uses raw SQL)
    ├── ISaltEntity.java
    ├── idrepo/IdentityHashSaltEntity, IdentityEncryptSaltEntity
    └── idmap/VidHashSaltEntity, VidEncryptSaltEntity
```

**Do not move this package into `id-repository-core`.** Core is the IDA-facing library; salt population is operational infrastructure only.

---

## 3. How it works

1. `SaltGeneratorBootApplication` starts non-web Spring context.
2. `DatabaseRouter` creates small Hikari pools for idrepo + idmap.
3. `SaltGeneratorRunner` invokes `SaltGenerator.start()`.
4. `SaltJdbcWriter` writes chunks with `ON CONFLICT DO NOTHING` (idempotent resume).
5. JVM exits via `SpringApplication.exit()`.

### Config keys

| Property | Purpose |
|----------|---------|
| `mosip.kernel.salt-generator.start-sequence` | First salt id |
| `mosip.kernel.salt-generator.end-sequence` | Last salt id |
| `mosip.kernel.salt-generator.chunk-size` | JDBC batch size (default 500) |

Set in config server / `bootstrap.properties`.

---

## 4. Build & run

```bash
cd id-repository
mvn install -pl id-repository-salt-generator -am -DskipTests=true
java -jar id-repository-salt-generator/target/id-repository-salt-generator-*.jar
```

Docker:

```bash
mvn package -pl id-repository-salt-generator -am -DskipTests=true
docker build -t id-repository-salt-generator id-repository-salt-generator
```

**Local laptop:** do not run this Job — `local-dev-setup/docker-compose` seeds salts in `init.sql` (0–999). See [`../local-dev-setup/AGENTS.md`](../local-dev-setup/AGENTS.md).

---

## 5. Helm

Chart: `helm/idrepo-saltgen` (repo root). Deploy as Job, not Deployment. Do not scale or run inside `id-repository-service` pods.

---

## 6. Agent rules (salt-generator-specific)

### Do

- Keep all salt-only code in this module under `io.mosip.idrepository.saltgenerator`.
- Reuse core utilities (`EnvUtil`, `IdRepoHikariDataSourceFactory`, `IdRepoLogger`) — do not duplicate.
- Keep job idempotent (`ON CONFLICT DO NOTHING`, resume from max id).

### Do not

- Add salt classes to `id-repository-core`.
- Run this job inside the HTTP service JVM.
- Change table schemas without coordinating `db_scripts/` and both DBs (idrepo + idmap).

---

## 7. Key files

| File | Purpose |
|------|---------|
| `SaltGeneratorBootApplication.java` | Entry + component scan |
| `service/SaltGenerator.java` | Generation loop |
| `service/SaltJdbcWriter.java` | JDBC writer |
| `service/DatabaseRouter.java` | Dual datasource |
| `src/main/resources/bootstrap.properties` | Config server bootstrap |
| `Dockerfile` | Container image |

---

*Last updated: 2026-08-07.*
