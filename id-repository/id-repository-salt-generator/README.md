# id-repository-salt-generator

One-shot batch job that populates `uin_hash_salt` and `uin_encrypt_salt` in **both** `mosip_idrepo` and `mosip_idmap`.

- **Not** part of the HTTP service — deploy as a Kubernetes Job (`helm/idrepo-saltgen`), not as a scaled Deployment.
- All salt logic: this module → `io.mosip.idrepository.saltgenerator.*`
- Shared library: `id-repository-core` → `io.mosip.idrepository.core.*` (EnvUtil, logging, Hikari helpers only)
- Entry point: `io.mosip.idrepository.saltgenerator.SaltGeneratorBootApplication`

## Build & run

```bash
cd id-repository
mvn install -pl id-repository-salt-generator -am -DskipTests=true
java -jar id-repository-salt-generator/target/id-repository-salt-generator-*.jar
```

Windows: `run-id-repository-saltgen-local.bat` from repo root (uses `local-run.env.bat`).

## Docker

```bash
mvn package -pl id-repository-salt-generator -am -DskipTests=true
docker build -t id-repository-salt-generator id-repository-salt-generator
```
