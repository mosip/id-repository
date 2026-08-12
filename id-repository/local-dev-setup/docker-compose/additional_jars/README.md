# additional_jars

`data-share-service` loads `kernel-auth-adapter` via Spring Boot `loader.path`.

Place **`kernel-auth-adapter.jar`** in this folder (gitignored). Without it, datashare exits on startup.

Version used by this repo’s Maven parent: **1.3.1**.

## After a local Maven build

From the **git repo root**, copy from your local Maven cache:

**Windows (PowerShell):**

```powershell
Copy-Item "$env:USERPROFILE\.m2\repository\io\mosip\kernel\kernel-auth-adapter\1.3.1\kernel-auth-adapter-1.3.1.jar" `
  id-repository\local-dev-setup\docker-compose\additional_jars\kernel-auth-adapter.jar
```

**macOS / Linux:**

```bash
cp ~/.m2/repository/io/mosip/kernel/kernel-auth-adapter/1.3.1/kernel-auth-adapter-1.3.1.jar \
  id-repository/local-dev-setup/docker-compose/additional_jars/kernel-auth-adapter.jar
```

If the file is missing from `.m2`, build first:

```bash
cd id-repository
mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true
```

Or download a matching release from Maven Central / MOSIP artifactory and rename it to `kernel-auth-adapter.jar`.
