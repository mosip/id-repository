# ID-Repository Local Development Setup

Run `id-repository-service` on your computer without the full MOSIP platform.
Docker Compose runs the dependencies, and the Java application runs on the host.

## What you get

| Service | Host URL / port |
|---------|-----------------|
| **ID-Repository** | `http://localhost:8090` |
| PostgreSQL | `localhost:5455` |
| Config Server | `http://localhost:51100/config` |
| WireMock (IAM, PMS, UIN/VID/RID, WebSub, Data Share, BioSDK) | `http://localhost:8082` |
| Key Manager | `http://localhost:8088/v1/keymanager` |
| MinIO API / Console | `http://localhost:9000` / `9001` (`admin` / `minioadmin`) |
| WebSub partner acknowledgment | `localhost:8085` |

### Local API authentication

`mosip.auth.filter_disable=false`

This setting controls whether ID Repository checks who is calling its APIs
and what they are allowed to do. The local setup uses `false` so you can
test authentication and role permissions using WireMock test tokens.

| Value | What happens |
|-------|--------------|
| `false` | The authentication filter is enabled. Protected APIs require a valid token in the `Authorization` cookie. Endpoints that require a role also check that the caller has that role. |
| `true` | The authentication filter is disabled and controller role checks are skipped. Requests can reach the API without a token. |

The local value is set in
[`application-local.properties`](../id-repository-service/src/main/resources/application-local.properties).
See [Call the APIs](#call-the-apis) for how to get and use a WireMock test token.

## Prerequisites

Install these tools before starting. Run the commands in a terminal to check
that they are available.

| Tool | Requirement | Command to verify |
|------|-------------|-------------------|
| Git | Installed | `git --version` |
| JDK | Version 21 | `java -version` |
| Maven | Version 3.9.6 or later | `mvn -version` |
| Docker | Docker Desktop with Compose V2 | `docker --version` and `docker compose version` |
| curl | Installed | `curl --version` |

Start Docker Desktop before starting the dependencies. On Windows, use
`curl.exe` if PowerShell treats `curl` as an alias.

The local Key Manager uses a **PKCS12 keystore file** instead of an external
HSM. The repository includes the keystore that matches the local database
seed. Keep `keys/mosip-idrepo-ks.p12` when resetting the database; replacing
it will prevent Key Manager from decrypting the seeded keys. See the
[keystore guide](keys/README.md) for details.

For a Docker Desktop memory limit of 6 GB, use `./run-local-stack.sh up-low-mem`
to start the dependencies with the low-memory overlay (about 3 GB). The host
Java process needs memory outside Docker.

The host application requires the
[identity object validator JAR](deps/additional_jars/README.md). Quick setup
prepares it with the supplied scripts. For manual setup, follow the linked JAR
instructions.

### Network requirements

First-time setup needs Internet access to clone the repository, pull Docker images, and download Maven dependencies. When the identity object validator JAR is missing, the setup script also clones the `issue-1633` branch of `mosip-ref-impl` and builds the JAR.

After the images and build artifacts are available locally, you can start the local stack and run smoke checks offline. Builds that need to download dependencies or refresh snapshots require Internet access.

## Choose a setup

- [Quick setup](#quick-setup) starts the standard local environment with the
  supplied scripts on Windows, macOS, or Linux.
- [Manual setup](#manual-setup) walks through the build, dependency startup,
  and host JVM commands for customization and troubleshooting.

### Clone the repository

For local development, start with `develop`. This is the branch where MOSIP
integrates ongoing work. See the
[MOSIP Branching Strategy](https://github.com/mosip/mosip/wiki/Branching-Strategy)
for when to use a feature or release branch.

```bash
git clone --branch develop https://github.com/mosip/id-repository.git
cd id-repository
```

If you already cloned the repository, use `git switch develop` from the
repository root. Both setup options start from the repository root.

### Quick setup

#### 1. Enter the local setup directory

Start from the [Git repository root](#clone-the-repository).

```bash
cd id-repository/local-dev-setup
```

#### 2. Start the dependencies and application

Follow the instructions for your platform.

##### 2a. Windows

On the first run, prepare the required validator JAR using Git Bash or WSL:

```bash
bash run-local-stack.sh prep
```

Then start the dependencies and application from Windows Command Prompt:

```bat
REM Start the Docker dependencies.
run-idrepo-local.bat up

REM Build and start ID Repository on the host, outside Docker.
run-idrepo-local.bat run
```

The batch file uses the Windows JDK and Maven. Leave the application running
in its terminal.

##### 2b. macOS / Linux / Git Bash

```bash
# Prepare and start the Docker dependencies.
./run-local-stack.sh up

# Build and start ID Repository on the host, outside Docker.
./run-idrepo-local.sh run
```

The `up` command prepares the required files and starts the dependencies.
Leave the application running in its terminal.

#### 3. Verify the setup

After the application starts, open a second terminal in the local setup
directory and run:

```bat
REM Check the application and dependencies.
run-idrepo-local.bat smoke
```

Or on macOS / Linux / Git Bash:

```bash
# Check the application and dependencies.
./run-idrepo-local.sh smoke
```

When the script prints `All smoke checks passed.`, continue to
[After smoke checks](#after-smoke-checks). If a check fails, note the
check name and see [Troubleshooting](#troubleshooting).

### Manual setup

#### 1. Build the service

Start from the [Git repository root](#clone-the-repository).

```bash
# Enter the Maven parent directory.
cd id-repository

# Build the service and its required modules.
mvn -pl id-repository-service -am clean package -DskipTests -Dgpg.skip=true
```

The second `id-repository` directory is the Maven parent inside the cloned
Git repository.

#### 2. Prepare local files

##### 2a. Check the Key Manager keystore

The default Key Manager configuration uses the committed
`keys/mosip-idrepo-ks.p12`, paired with the static seed in `deps/init.sql`.
If it is missing, restore the matching file.
See the [keystore guide](keys/README.md) for the alternative configuration.

##### 2b. Prepare the identity object validator

The host application requires
`deps/additional_jars/kernel-ref-idobjectvalidator.jar` at runtime. Obtain it
as described in [Prerequisites](#prerequisites).

**WireMock:** The local dependency stack uses the supplied HTTP stubs; no
separate WireMock extension JAR is required.

#### 3. Start the dependencies and application

##### 3a. Start the Docker dependencies

From the Maven parent directory used in step 1, start the dependencies:

```bash
cd local-dev-setup/deps
docker compose up -d --remove-orphans
docker compose ps
```

Wait for the dependencies to become healthy.

##### 3b. Start the host application

After building the service and starting the dependencies, return from `deps/`
to the Maven parent directory and run the application:

```bash
cd ../..
java \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens java.base/java.io=ALL-UNNAMED \
  -Dloader.path="$PWD/local-dev-setup/deps/additional_jars/kernel-ref-idobjectvalidator.jar" \
  -Dspring.cloud.config.uri=http://localhost:51100/config \
  -Dspring.config.import=configserver:http://localhost:51100/config \
  -Dspring.cloud.config.label=develop \
  -Dspring.profiles.active=default,local \
  -Dspring.cloud.config.override-none=true \
  -Dspring.cloud.loadbalancer.enabled=false \
  -Dserver.port=8090 \
  -jar id-repository-service/target/id-repository-service-*.jar
```

The application uses `application-local.properties` to reach dependencies on
localhost.

#### 4. Smoke checks

```bash
curl -s http://localhost:8090/actuator/health
curl -s http://localhost:8088/v1/keymanager/actuator/health
curl -s http://localhost:8082/v1/idgenerator/uin
curl -s http://localhost:8082/v1/ridgenerator/generate/rid/10001/10001
curl -s "http://localhost:8082/v1/masterdata/idschema/latest?schemaVersion=0"
```

| Check | Expect |
|-------|--------|
| id-repo / keymanager health | `"status":"UP"` |
| idgenerator UIN | JSON with a numeric `uin` |
| ridgenerator RID | HTTP 200 with a generated RID |
| idschema | HTTP 200 with `schemaJson` |

## After smoke checks

Follow these steps after either Quick setup or Manual setup.

### Run JUnit tests

Open a separate terminal at the Git repository root and run the service's
JUnit unit tests with Maven.

```bash
cd id-repository
mvn -pl id-repository-service -am test -Dgpg.skip=true
```

Wait until ID Repository has started and the smoke checks pass. Leave the app
running and use a second terminal for the JUnit tests or Sonar. If you start
another build while the tests are running, it can delete files the tests need.
If that happens, let the build finish, then rerun the tests.

### Run the API test rig

See [Local ID schema](#local-id-schema) for the default handle types and the steps to switch schemas for handle-specific tests.

Keep the local application and Docker dependencies running. The TestNG rig
sends HTTP requests to the running application. From the Git repository
root, run the local API smoke suite.

**macOS / Linux / Git Bash**

```bash
cd api-test
./run-local-smoke.sh smoke
```

**Windows Command Prompt**

```bat
cd api-test
run-local-smoke.bat smoke
```

For the full regression suite and report locations, see the
[API test guide](../../api-test/README.md#step-c--run-api-test).

### Call the APIs

Use [Swagger](#swagger) or the Postman collection below to try individual API
requests. Requests to protected endpoints require a WireMock test token.
Swagger Try-it and curl report `Authorization token not present` when the
token is missing.

**1. JWT from WireMock** (`mosip-idrepo-client`, role `ID_REPOSITORY`):

```bat
curl.exe -s -X POST "http://localhost:8082/v1/authmanager/authenticate/clientidsecretkey" ^
  -H "Content-Type: application/json" ^
  -d "{\"id\":\"string\",\"version\":\"string\",\"requesttime\":\"2026-10-13T12:00:00.000Z\",\"metadata\":{},\"request\":{\"clientId\":\"mosip-idrepo-client\",\"secretKey\":\"local-secret\",\"appId\":\"idrepo\"}}"
```

Copy `response.token`.

**2. Send it as cookie** `Authorization=<jwt>` (not `Bearer`):

```bat
curl.exe -s "http://localhost:8090/idrepository/v1/vid/uin/<UIN>" -H "Cookie: Authorization=<token>"
```

Postman: collection [`postman/Id-Repository-Local.postman_collection.json`](postman/Id-Repository-Local.postman_collection.json) → **00 - Auth (WireMock) → Client token (idrepo)**.

In [Swagger](#swagger), select **Authorize** and enter the raw JWT. If Try-it still reports a missing token, use curl/Postman with the Cookie header.

When adding an identity, use a UIN returned by
`GET http://localhost:8082/v1/idgenerator/uin`. An invented UIN can cause
`IDR-IDC-002`. For a draft request, include `?UIN=` only if that UIN already
exists in `idrepo.uin`. With an empty database, add the identity first or
omit the parameter.

#### Swagger

The application must be running to access these pages.

| API | UI |
|-----|-----|
| Identity | http://localhost:8090/idrepository/v1/identity/swagger-ui/index.html |
| VID | http://localhost:8090/idrepository/v1/swagger-ui/index.html (select **MOSIP ID-Repository VID** if needed) |
| Credential service | http://localhost:8090/v1/credentialservice/swagger-ui/index.html |
| Credential request | http://localhost:8090/v1/credentialrequest/swagger-ui/index.html |

## Local ID schema

WireMock serves the active schema from `deps/wiremock/__files/idschema-latest-active.json`. The default response contains schema `1001`, version `0.1`. The response stores the schema as an escaped JSON string in `response.schemaJson`. WireMock serves the active schema for every `schemaVersion` query value.

In the default schema, `email` and `phone` are string handles and `functionalId` is an array handle. Email array-handle cases are skipped with the default schema and become eligible when `email` is an array of `{value, tags}` objects.

1. From `local-dev-setup`, run `./run-local-stack.sh idschema email-handle`.
   Use `diff-handle` for the license-number variant or `default` to restore
   the original schema. On Windows, use `bash run-local-stack.sh` with the
   same arguments.
   If you started the stack with `up-low-mem`, set `LOW_MEM=1` when switching
   or restoring the schema so WireMock uses the same Compose overlay.
2. Verify that WireMock returns the expected schema.

   ```bash
   curl -fsS "http://localhost:8082/v1/masterdata/idschema/latest?schemaVersion=0"
   ```

   Inspect `response.schemaJson` for the expected handle definitions.
3. From `local-dev-setup`, run `./run-idrepo-local.sh stop` and then `./run-idrepo-local.sh start` to reload the host app’s schema cache. On Windows, use `run-idrepo-local.bat` with the same commands.

To roll back, run `./run-local-stack.sh idschema default` and restart the host app.

## Run and debug in an IDE (optional)

Use this setup to pause at breakpoints, inspect variables, and step through
the service when an API request runs. After changing the code, restart the
debug session to run the updated version.

1. Keep the Docker dependencies running. If ID Repository is already running
   from a terminal, press `Ctrl+C` there to free port `8090`.
2. Open the Maven parent directory (the `id-repository/` folder inside the
   Git repository) in your IDE. Import it as a Maven project and use JDK 21.
3. Check that `local-dev-setup/deps/additional_jars/kernel-ref-idobjectvalidator.jar`
   exists. If needed, follow the [validator JAR preparation instructions](deps/additional_jars/README.md).
   Add this JAR to the runtime classpath using the instructions for your IDE.

### IntelliJ IDEA

1. Open **File → Project Structure → Modules**. Select `id-repository-service`,
   open **Dependencies**, then choose **+ → JARs or directories** and select
   `kernel-ref-idobjectvalidator.jar`.
2. Open **Run → Edit Configurations** and create an **Application** configuration.
   Use JDK 21, set the main class to
   `io.mosip.idrepository.bootstrap.IdRepositoryLauncher`, select
   `id-repository-service` for the module classpath, and set the working
   directory to the Maven parent directory.
3. Under **Modify options**, enable **Add VM options** and enter the options
   below. Set **Shorten command line** to **@argFiles (Java 9+)** so the
   launcher can see the individual classpath entries.
4. Set a breakpoint and select **Debug**.

```text
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
-Dspring.cloud.config.uri=http://localhost:51100/config
-Dspring.config.import=configserver:http://localhost:51100/config
-Dspring.cloud.config.label=develop
-Dspring.profiles.active=default,local
-Dspring.cloud.config.override-none=true
-Dspring.cloud.loadbalancer.enabled=false
-Dserver.port=8090
```

For details on these configuration fields, see the
[IntelliJ IDEA Application configuration guide](https://www.jetbrains.com/help/idea/run-debug-configuration-java-application.html).

### Visual Studio Code

Open the Maven parent directory in VS Code with the Extension Pack for Java
and JDK 21. Create `.vscode/launch.json` or add the configuration below to its
existing `configurations` array.

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Debug ID Repository (local)",
      "request": "launch",
      "mainClass": "io.mosip.idrepository.bootstrap.IdRepositoryLauncher",
      "projectName": "id-repository-service",
      "cwd": "${workspaceFolder}",
      "shortenCommandLine": "argfile",
      "classPaths": [
        "$Auto",
        "${workspaceFolder}/local-dev-setup/deps/additional_jars/kernel-ref-idobjectvalidator.jar"
      ],
      "vmArgs": [
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "-Dspring.cloud.config.uri=http://localhost:51100/config",
        "-Dspring.config.import=configserver:http://localhost:51100/config",
        "-Dspring.cloud.config.label=develop",
        "-Dspring.profiles.active=default,local",
        "-Dspring.cloud.config.override-none=true",
        "-Dspring.cloud.loadbalancer.enabled=false",
        "-Dserver.port=8090"
      ]
    }
  ]
}
```

Set a breakpoint, select **Debug ID Repository (local)** in **Run and Debug**,
and press **F5**. See the
[VS Code Java debugging guide](https://code.visualstudio.com/docs/java/java-debugging).

### Verify the debug session

Once the service starts on port `8090`, run the
[smoke checks](#3-verify-the-setup) in a separate terminal. Then use
[Swagger or Postman](#call-the-apis) to send a request that reaches your
breakpoint.

## Reset local data (optional)

A fresh setup already initializes the local databases.

Docker Compose keeps PostgreSQL and MinIO data between restarts. Use a reset
when you need an empty database for API tests, need to remove bad local
credential rows, or need to rerun database initialization after an
`init.sql` or DDL change.

**Warning.** Both reset options delete the PostgreSQL and MinIO Docker
volumes. PostgreSQL reruns
[`deps/init.sql`](deps/init.sql) on startup. Keep the matching
[Key Manager keystore](#prerequisites); a mismatched keystore can cause
`KER-KMA-004` and other crypto errors.

### Reset using a script

Use `wipe` to reset the data and restart only the Docker dependencies. Use
`wipe-run` to also rebuild and start ID Repository.

```bat
REM Delete local Docker volumes, then rebuild and start the application.
run-idrepo-local.bat wipe-run
```

Or:

```bash
# Delete local Docker volumes, then rebuild and start the application.
./run-idrepo-local.sh wipe-run
```

### Reset manually with Docker Compose

Stop ID Repository, then run the following commands from `local-dev-setup/`
to delete the volumes and restart the Docker dependencies.

```bash
cd deps
docker compose down -v --remove-orphans
docker compose up -d --remove-orphans
```

Wait for the dependencies to become healthy before restarting the host app.

## Local ID Repository script commands

Use these scripts to manage ID Repository during local development. Run
`run` after changing Java code or `pom.xml` to rebuild and start the service.
Use `start` to launch an existing build, `smoke` to check the running service
and its dependencies, or `test` to run the unit tests.

Use `run-idrepo-local.bat` in Windows Command Prompt or
`./run-idrepo-local.sh` on macOS, Linux, or in Git Bash. Both scripts use
the same commands and default to `run` when you omit the command.

From `local-dev-setup/`, list the available commands with `help`.

**Windows Command Prompt**

```bat
run-idrepo-local.bat help
```

**Linux, macOS, or Git Bash**

```bash
./run-idrepo-local.sh help
```

### Additional information

Use this reference when you need details about a script command.

#### What each command does

Use this table to see whether a command builds the code or runs tests,
changes Docker dependencies or local data, or starts, stops, or checks
the ID Repository application running on your computer.

| Command | Maven | Docker / DB | Host JVM |
|---------|-------|-------------|----------|
| **`run`** (default) | `clean package -Dmaven.test.skip=true` | unchanged | stop if running, then start `:8090` |
| **`start`** | none | unchanged | start existing jar |
| **`rebuild`** / `build` | `clean package -Dmaven.test.skip=true` | unchanged | not started |
| **`stop`** | none | unchanged | kill `id-repository-service*.jar` |
| **`smoke`** | none | none | HTTP GETs (app must already be up) |
| **`test`** | `mvn test` (unit) | none | not started |
| **`sonar`** | `mvn verify -Psonar` | none | not started |
| **`up`** | none | start deps, **keep volumes** | not started |
| **`wipe`** / `cleardb` | none | `compose down -v` then `up` (empty DB) | stopped first |
| **`wipe-run`** | same as `run` | same as `wipe`, then start app | rebuild + start |

Aliases: `--no-build` → `start`, `--build` → `run`, `clear-db` / `reset` → `wipe`.

The `.bat` drives **Windows** JDK/Maven. The `.sh` is for macOS/Linux/Git Bash; `wipe` / `up` / `smoke` on the `.sh` call [`run-local-stack.sh`](run-local-stack.sh).

#### `run` (default)

Stops the running ID Repository JVM, packages `id-repository-service` (`-Dmaven.test.skip=true -Dgpg.skip=true`), waits for config-server at `http://localhost:51100/config`, then:

```text
java -Dloader.path=deps\additional_jars\kernel-ref-idobjectvalidator.jar
     -Dspring.profiles.active=default,local
     -Dserver.port=8090
     -jar id-repository-service-*.jar
```

Logs are under `.local/logs/`. Use after pom or Java changes. Do **not** use `start` until that rebuild has finished.

#### `start`

Same JVM flags, **no Maven**. Needs an existing `id-repository-service/target/id-repository-service-*.jar`. Use after a successful `run` / `rebuild` when only Docker deps were restarted.

#### `rebuild`

Maven package only. Prints the jar path. Use when you want the fat jar without binding the terminal to `:8090`.

#### `stop`

Kills the host `java` process whose command line contains `id-repository-service*.jar`. Does not stop Docker.

#### `up`

Starts Compose deps in order (postgres + config-server, then keymanager / WireMock / MinIO / websub). **Volumes stay** — same UIN/VID/credential rows as last time. Does not start the host app.

Equivalent: `deps\restart-idrepo.bat` or `./run-local-stack.sh up`.

#### `smoke`

Runs the HTTP GETs under [Smoke checks](#4-smoke-checks). Start the host app
and dependencies first. On Windows, use `curl.exe` to run checks by hand.

#### `test`

Runs the [JUnit unit tests](#run-junit-tests).

#### `sonar`

```text
mvn -pl id-repository-service -am verify -Psonar -Dgpg.skip=true
```

Jacoco writes `id-repository-service/target/site/jacoco`. SonarCloud upload needs `SONAR_TOKEN` (and org/project as in the parent `sonar` profile). Without the token, verify/jacoco still run; the upload step fails.

## Other stack commands (`run-local-stack.sh`)

Use Git Bash / WSL / macOS / Linux. Windows cmd can still `bash run-local-stack.sh <cmd>` for Docker-only steps, or stay on the `.bat` for `up` / `wipe` / `smoke`.

| Command | Effect |
|---------|--------|
| `up` / `up-low-mem` | Prep + start Docker deps (low-mem overlay ≈ 3 GB) |
| `restart` / `restart-low-mem` | Ordered recreate of deps |
| `down` | Stop containers, **keep** volumes |
| `wipe` / `wipe-low-mem` | `down -v` then `up` |
| `prep` | PKCS12 + validator jar (`keys-generator.jar` only if init.sql has no static seed) |
| `build` | `mvn package` skip tests (WSL may shell out to Windows Maven) |
| `status` / `logs` | `docker compose ps` / follow logs (default `keymanager-service`) |

After jar or `application-local.properties` changes, restart the **host** JVM (`stop` then `run` or `start`). Recreate WireMock only when stubs change:

```bash
cd deps
docker compose up -d --force-recreate --no-deps mock-service
# with low-mem caps:
docker compose -f docker-compose.yml -f docker-compose.low-mem.yml up -d --force-recreate --no-deps mock-service
```

## Databases

| Database | Schema | App user | Password |
|----------|--------|----------|----------|
| `mosip_idrepo` | `idrepo` | `idrepouser` | `mosip123` |
| `mosip_idmap` | `idmap` | `idmapuser` | `mosip123` |
| `mosip_credential` | `credential` | `credentialuser` | `mosip123` |
| `mosip_keymgr` | `keymgr` | `keymgruser` | `mosip123` |

Host `localhost:5455`, superuser `postgres` / `mosip123`. DDL from [`db_scripts/`](../../db_scripts/). Local salts 0–999 are seeded by [`deps/init.sql`](deps/init.sql).

## Local config

| Purpose | Where |
|---------|--------|
| Host (`default,local`) | [`application-local.properties`](../id-repository-service/src/main/resources/application-local.properties) |
| Containers | [`deps/mosip-config/`](deps/mosip-config/) |
| Keystore | `keys/mosip-idrepo-ks.p12` / password `1234` |

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Config refused on `:51000` | Host uses **`http://localhost:51100/config`** |
| Placeholder `mosip.idrepo.crypto.refId.uin` | Use profiles `default,local`; ensure config-server healthy; `./run-local-stack.sh restart` |
| Nothing on `:8090` | Start host app: `run-idrepo-local.*` |
| BioSDK init fails | Recreate `mock-service`, or set `mosip.biosdk.default.service.url` to an external BioSDK |
| `IDR-IDS-009` / empty CBEFF (~150 B in MinIO) | Check that the identity request contains valid biometric data. |
| `KER-KMS-*` | Check PKCS12 + password; `docker compose logs keymanager-service` |
| `IDR-IDC-002` | Use `GET /v1/idgenerator/uin` |
| `IDR-IDC-012` Record already exists | The UIN already exists. If you need a clean database, see [Reset local data (optional)](#reset-local-data-optional). |
| `IDR-IDC-007` on draft | UIN not in DB — Add Identity first or omit `?UIN=` |
| `KER-WSC-101` | Recreate `mock-service`; host `mosip.websub.url=http://localhost:8082` |
| 503 to localhost deps | `spring.cloud.loadbalancer.enabled=false` (already in `local`) |
| Empty tables in pgAdmin | Open the right DB + schema, not `postgres/public` |
| Changed `init.sql` but DB unchanged | See [Reset local data (optional)](#reset-local-data-optional) to rerun initialization. |

More detail: [`AGENTS.md`](AGENTS.md).

## Directory layout

```text
local-dev-setup/
├── LOCAL-DEV-SETUP.md        ← quick and manual setup
├── AGENTS.md                 ← agent rules
├── run-local-stack.sh        ← prep / up / wipe / smoke (bash)
├── run-idrepo-local.bat|.sh  ← host JVM + wipe / test / sonar
├── postman/                  ← local API collection and environment
├── keys/                     ← local Key Manager keystore
└── deps/
    ├── docker-compose.yml    ← deps only
    ├── init.sql
    ├── additional_jars/      ← kernel-ref-idobjectvalidator.jar (loader.path)
    ├── mosip-config/
    ├── wiremock/             ← mappings/id-repository.json and __files/EMPTY-FILE
    ├── keymanager/           ← DDL + bootstrap (static key DML is inlined in init.sql)
    └── websub-partner-ack/
```

The local Config Server uses the files in `deps/mosip-config/`; an external
`mosip-config` checkout is not needed for this setup.
