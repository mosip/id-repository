# ID Repository Local Development

Run `id-repository-service` locally without the full MOSIP platform. Docker
Compose runs the dependencies, and the application runs on the host.

## Choose a setup

- **Quick setup:** Follow this README to start the standard local environment
  using the supplied scripts. Use this for normal development.
- **Manual setup:** Follow the [developer setup guide](LOCAL-DEV-SETUP.md) to
  configure and start the components step by step. Use this when you need to
  understand, customize, or troubleshoot the environment.

For automated API tests, see the [API test guide](../../api-test/README.md).

---

## Architecture

```text
Docker Compose (deps/ only)         Host (local machine)
──────────────────────────          ─────────────
Postgres :5455                      id-repository-service :8090
Config-server :51100
WireMock :8082  (IAM, BioSDK, …)
MinIO :9000
Keymanager :8088
websub-partner-ack :8085
```

Docker runs the dependencies. The ID Repository application runs directly on
your computer as a Java process. The local setup uses WireMock for
authentication; API calls require an authorization cookie.

---

## Prerequisites

JDK **21**, Maven **3.9.x**, Docker Desktop (Compose V2). Details: [`LOCAL-DEV-SETUP.md`](LOCAL-DEV-SETUP.md).

For a Docker Desktop memory limit of 6 GB, use
`./run-local-stack.sh up-low-mem`. The Java application also needs memory
outside Docker.

The host application also needs
[`kernel-ref-idobjectvalidator.jar`](deps/additional_jars/README.md). The
quick setup instructions explain how to prepare it for each platform.

Keep the [Key Manager keystore](keys/README.md); it must match the static
database seed.

Run all commands below from:

```text
id-repository/id-repository/local-dev-setup
```

(`id-repository` git clone → nested Maven tree `id-repository/` → `local-dev-setup/`.)

---

## Quick setup

Use these steps to start the standard local environment with the supplied
scripts. Commands are run from `id-repository/local-dev-setup/` inside the
cloned repository.

### Clone the repository

Skip this step if you already have the repository.

```bash
# Clone the consolidation branch (master/develop do not have local-dev-setup yet).
git clone --branch IDREPO-1583-consolidate-idrepo-services https://github.com/mosip/id-repository.git

# Enter the local setup directory.
cd id-repository/id-repository/local-dev-setup
```

After this lands on a release branch, clone that branch instead.

### Windows

On the first run, prepare the required validator JAR using Git Bash or WSL:

```bat
REM Prepare the required local files.
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

### macOS / Linux / Git Bash

```bash
# Prepare and start the Docker dependencies.
./run-local-stack.sh up

# Build and start ID Repository on the host, outside Docker.
./run-idrepo-local.sh run
```

The `up` command prepares the required files and starts the dependencies.
Leave the application running in its terminal.

### Verify

After the application starts, open a second terminal and run:

```bat
REM Check the application and dependencies.
run-idrepo-local.bat smoke
```

Or on macOS / Linux / Git Bash:

```bash
# Check the application and dependencies.
./run-idrepo-local.sh smoke
```

The smoke command checks both the application and its dependencies.

### Start with empty local data

**Warning:** `wipe-run` deletes the Docker volumes, including PostgreSQL and
MinIO data, before rebuilding and starting the application.

```bat
REM Delete local Docker volumes, then rebuild and start the application.
run-idrepo-local.bat wipe-run
```

Or:

```bash
# Delete local Docker volumes, then rebuild and start the application.
./run-idrepo-local.sh wipe-run
```

Keep `keys/mosip-idrepo-ks.p12`: it must match the static Key Manager seed.

---

## `run-idrepo-local.bat` / `.sh` commands

Both scripts accept the same first argument. No argument means `run`. Print the list with `help`.

```bat
run-idrepo-local.bat help
```

```bash
./run-idrepo-local.sh help
```

| Command | Maven | Docker / DB | Host JVM |
|---------|-------|-------------|----------|
| **`run`** (default) | `clean package -DskipTests` | unchanged | stop if running, then start `:8090` |
| **`start`** | none | unchanged | start existing jar |
| **`rebuild`** / `build` | `clean package -DskipTests` | unchanged | not started |
| **`stop`** | none | unchanged | kill `id-repository-service*.jar` |
| **`smoke`** | none | none | HTTP GETs (app must already be up) |
| **`test`** | `mvn test` (unit) | none | not started |
| **`sonar`** | `mvn verify -Psonar` | none | not started |
| **`up`** | none | start deps, **keep volumes** | not started |
| **`wipe`** / `cleardb` | none | `compose down -v` then `up` (empty DB) | stopped first |
| **`wipe-run`** | same as `run` | same as `wipe`, then start app | rebuild + start |

Aliases: `--no-build` → `start`, `--build` → `run`, `clear-db` / `reset` → `wipe`.

The `.bat` drives **Windows** JDK/Maven. The `.sh` is for macOS/Linux/Git Bash; `wipe` / `up` / `smoke` on the `.sh` call [`run-local-stack.sh`](run-local-stack.sh).

### `run` (default)

Stops any host JVM, packages `id-repository-service` (`-Dmaven.test.skip=true -Dgpg.skip=true`), waits for config-server at `http://localhost:51100/config`, then:

```text
java -Dloader.path=deps\additional_jars\kernel-ref-idobjectvalidator.jar
     -Dspring.profiles.active=default,local
     -Dserver.port=8090
     -jar id-repository-service-1.4.1-SNAPSHOT.jar
```

Logs: `.local/logs/` (not `logs/`). Use after pom or Java changes. Do **not** use `start` until that rebuild has finished. Scripts pick the fat jar (excluding `-sources` / `-javadoc`); never use a bare `id-repository-service-*.jar` glob.

### `start`

Same JVM flags, **no Maven**. Needs an existing fat
`id-repository-service/target/id-repository-service-*-SNAPSHOT.jar` (not
`-sources` / `-javadoc`). Use after a successful `run` / `rebuild` when only
Docker deps were restarted.

### `rebuild`

Maven package only. Prints the jar path. Use when you want the fat jar without binding the terminal to `:8090`. **Stop the host app first** — on Windows an in-use jar leaves a non-runnable plain jar after a failed `spring-boot:repackage`.

### `stop`

Kills the host `java` process whose command line contains `id-repository-service*.jar`. Does not stop Docker.

### `up`

Starts Compose deps in order (postgres + config-server, then keymanager / WireMock / MinIO / websub). **Volumes stay** — same UIN/VID/credential rows as last time. Does not start the host app.

Equivalent: `deps\restart-idrepo.bat` or `./run-local-stack.sh up`.

### `wipe`

1. Stop the host JVM (open DB connections).
2. `docker compose down -v --remove-orphans` under `deps/` — **deletes Postgres and MinIO volumes**.
3. Start deps again. Postgres re-runs [`deps/init.sql`](deps/init.sql) (schemas, salts 0–999, static keymgr seed, app users `*user` / `mosip123`).

**Does not start `:8090`.** PKCS12 under `keys/` is **kept**. Do not delete `keys/mosip-idrepo-ks.p12` or keymanager decrypt fails (`KER-KMA-004` / crypto errors).

Use before a clean api-test run, after `IDR-IDC-012` (UIN already exists), bad credential rows, or `init.sql` / DDL changes.

### `wipe-run`

`wipe`, then `run`. One command for empty DB + new jar + host app.

```bat
run-idrepo-local.bat wipe-run
```

```bash
./run-idrepo-local.sh wipe-run
```

### `smoke`

HTTP GET (must return success):

| Check | URL |
|-------|-----|
| id-repo health | `http://localhost:8090/actuator/health` |
| keymanager health | `http://localhost:8088/v1/keymanager/actuator/health` |
| idgenerator UIN | `http://localhost:8082/v1/idgenerator/uin` |
| RID | `http://localhost:8082/v1/ridgenerator/generate/rid/10001/10001` |
| idschema | `http://localhost:8082/v1/masterdata/idschema/latest?schemaVersion=0` |

Host app + deps must already be running. Windows: `curl.exe` (not PowerShell `curl`).

### `test`

Unit tests only:

```text
mvn -pl id-repository-service -am test -Dgpg.skip=true
```

This is **not** the TestNG api-test suite.

Do **not** run `test` or `sonar` while `run` / `rebuild` / `wipe-run` is packaging. Those commands `mvn clean` the same `id-repository-service/target` directory; Surefire then dies with `forked VM terminated` / exit code `-1` (often blamed on `KernelAuthSpringFactoriesFilteringClassLoaderTest`). Stop the overlapping Maven job and re-run `test` / `sonar` alone.

### `sonar`

```text
mvn -pl id-repository-service -am verify -Psonar -Dgpg.skip=true
```

Jacoco writes `id-repository-service/target/site/jacoco`. SonarCloud upload needs `SONAR_TOKEN` (and org/project as in the parent `sonar` profile). Without the token, verify/jacoco still run; the upload step fails.

---

## What does *not* clear the database

| Command | DB |
|---------|-----|
| `rebuild` / `run` / `start` / `test` / `sonar` | unchanged |
| `up` / `stop` / `./run-local-stack.sh down` | volumes **kept** |
| `wipe` / `wipe-run` / `./run-local-stack.sh wipe` | volumes **deleted**, `init.sql` re-runs |

`run` alone does **not** reset Postgres.

**Manual wipe:**

```bash
cd deps
docker compose down -v --remove-orphans
docker compose up -d --remove-orphans
# wait healthy, then: run-idrepo-local.bat run
```

---

## Call the APIs

AuthFilter is on. Swagger Try-it and curl without a token fail with `Authorization token not present`.

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

Swagger: `http://localhost:8090/idrepository/v1/identity/swagger-ui/index.html` — **Authorize** with the raw JWT. If Try-it still reports a missing token, use curl/Postman with the Cookie header.

UINs must come from `GET http://localhost:8082/v1/idgenerator/uin` — invented digits → `IDR-IDC-002`.

---

## API tests

For local and deployed-environment test instructions, see the
[API test guide](../../api-test/README.md).

---

## QA: test with a different Id Schema

Local Id Schema is served by WireMock. Switch it **before** (or restart after)
`run-idrepo-local.bat run`. Full detail:
[`deps/wiremock/idschema/README.md`](deps/wiremock/idschema/README.md).

| Variant | Handles | Use when |
|---------|---------|----------|
| `default` | `email`/`phone` string; `functionalId` array | Normal local / most api-test |
| `email-handle` | `email` array `[{value,tags}]`; `phone` string | Email array-handle api-test cases |
| `diff-handle` | `licenseNo` + `functionalId`; email/phone not handles | Diff-handle / licenseNo cases |

### Windows (cmd)

```bat
cd id-repository\id-repository\local-dev-setup

REM 1) Deps up (skip if already running)
run-idrepo-local.bat up

REM 2) Switch schema (recreates WireMock)
bash run-local-stack.sh idschema email-handle
REM    also: default | diff-handle

REM 3) Restart host so schema cache reloads
run-idrepo-local.bat stop
run-idrepo-local.bat run
```

### macOS / Linux / Git Bash

```bash
cd id-repository/id-repository/local-dev-setup
./run-local-stack.sh up
./run-local-stack.sh idschema email-handle   # or default | diff-handle
./run-idrepo-local.sh stop
./run-idrepo-local.sh run
```

### Verify, then test

```bat
curl.exe -s "http://localhost:8082/v1/masterdata/idschema/latest?schemaVersion=0"
```

Then use Swagger / Postman, or from repo root `api-test\run-local-smoke.bat`.

---

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
| `idschema` | Switch Id Schema: `idschema [default\|email-handle\|diff-handle]` |
| `status` / `logs` | `docker compose ps` / follow logs (default `keymanager-service`) |

After jar or `application-local.properties` changes, restart the **host** JVM (`stop` then `run` or `start`). Recreate WireMock only when stubs change:

```bash
cd deps
docker compose up -d --force-recreate --no-deps mock-service
# with low-mem caps:
docker compose -f docker-compose.yml -f docker-compose.low-mem.yml up -d --force-recreate --no-deps mock-service
```

---

## Ports

| Service | Port |
|---------|------|
| ID-Repository (host) | `8090` |
| PostgreSQL | `5455` |
| Config Server | `51100` |
| WireMock | `8082` |
| Key Manager | `8088` |
| MinIO | `9000` / `9001` |
| websub-partner-ack | `8085` |

## Swagger

The application must be running to access these pages.

| API | UI | SpringDoc group |
|-----|-----|-----------------|
| Identity | http://localhost:8090/idrepository/v1/identity/swagger-ui/index.html | `identity` (pre-selected) |
| VID | http://localhost:8090/idrepository/v1/swagger-ui/index.html | Select **`vid`** (includes `/vid/**` and `/draft/vid`) |
| Credential service | http://localhost:8090/v1/credentialservice/swagger-ui/index.html | `credential-service` (pre-selected) |
| Credential request | http://localhost:8090/v1/credentialrequest/swagger-ui/index.html | `credential-request` (pre-selected) |

---

## Layout

```text
local-dev-setup/
├── README.md                 ← this file
├── LOCAL-DEV-SETUP.md        ← detailed human guide
├── AGENTS.md                 ← agent rules
├── run-local-stack.sh        ← prep / up / wipe / smoke (bash)
├── run-idrepo-local.bat|.sh  ← host JVM + wipe / test / sonar
├── keys/                     ← local Key Manager keystore
└── deps/
    ├── docker-compose.yml    ← deps only
    ├── init.sql
    ├── additional_jars/      ← kernel-ref-idobjectvalidator.jar (loader.path)
    ├── mosip-config/
    ├── wiremock/mappings/    ← id-repository.json + idschema-latest.json
    ├── wiremock/__files/     ← idschema-latest-*.json (active schema body)
    ├── wiremock/idschema/    ← schema switch guide + bare schema sources
    ├── keymanager/           ← DDL + bootstrap (static key DML is inlined in init.sql)
    └── websub-partner-ack/
```

---

*Last updated: 2026-10-13.*
