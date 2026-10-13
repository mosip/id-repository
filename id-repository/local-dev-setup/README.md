# ID-Repository local development

Run **id-repository-service** on your laptop: Docker for dependencies, JVM on the host.

| Guide | Audience |
|-------|----------|
| **This README** | Startup, wipe, and every `run-idrepo-local` / stack command |
| [`LOCAL-DEV-SETUP.md`](LOCAL-DEV-SETUP.md) | Prerequisites, DBs, troubleshooting |
| [`AGENTS.md`](AGENTS.md) | Agent rules, WireMock stubs, failure modes |

---

## Architecture

```text
Docker Compose (deps only)          Host
─────────────────────────           ────
Postgres :5455                      id-repository-service :8090
Config-server :51100
WireMock :8082  (IAM, BioSDK, …)
MinIO :9000
Keymanager :8088
websub-partner-ack :8085
```

There is **no** id-repo container. Auth uses WireMock JWTs (no Keycloak). The host JVM uses profiles `default,local` with **AuthFilter on** (`mosip.auth.filter_disable=false`). API calls need a cookie (see [Call the APIs](#call-the-apis)).

---

## Prerequisites

JDK **21**, Maven **3.9+**, Docker Desktop (Compose V2). Details: [`LOCAL-DEV-SETUP.md`](LOCAL-DEV-SETUP.md).

Host JVM also needs [`deps/additional_jars/kernel-ref-idobjectvalidator.jar`](deps/additional_jars/README.md). First `up` / `prep` fetches it (`bash run-local-stack.sh prep`). Keep [`keys/mosip-idrepo-ks.p12`](keys/README.md) — do not regenerate it after a DB wipe.

Run all commands below from:

```text
id-repository/id-repository/local-dev-setup
```

(`id-repository` git clone → nested Maven tree `id-repository/` → `local-dev-setup/`.)

---

## Clean startup

**Windows (cmd)** — `bash` on many PCs is WSL and does not inherit Windows `JAVA_HOME`. Prefer the `.bat`:

```bat
cd id-repository\id-repository\local-dev-setup
run-idrepo-local.bat up
run-idrepo-local.bat run
```

Fresh empty DB + rebuild + start:

```bat
run-idrepo-local.bat wipe-run
```

**macOS / Linux / Git Bash:**

```bash
chmod +x run-local-stack.sh run-idrepo-local.sh   # once
./run-local-stack.sh up
./run-idrepo-local.sh run
# fresh DB:
./run-idrepo-local.sh wipe-run
```

Leave the host JVM running in that terminal (`Ctrl+C` or `run-idrepo-local.bat stop`). Smoke and API tests use a **second** terminal.

Keymanager uses the static SQL seed in `deps/init.sql` (`KM_STATIC_SEED=true`). Wait until `docker compose ps` (or `run-idrepo-local.bat smoke`) looks healthy before calling identity APIs.

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
     -jar id-repository-service-*.jar
```

Logs: `.local/logs/` (not `logs/`). Use after pom or Java changes. Do **not** use `start` until that rebuild has finished.

### `start`

Same JVM flags, **no Maven**. Needs an existing `id-repository-service/target/id-repository-service-*.jar`. Use after a successful `run` / `rebuild` when only Docker deps were restarted.

### `rebuild`

Maven package only. Prints the jar path. Use when you want the fat jar without binding the terminal to `:8090`.

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
  -d "{\"id\":\"string\",\"version\":\"string\",\"requesttime\":\"2026-10-13T12:00:00.000Z\",\"metadata\":{},\"request\":{\"clientId\":\"mosip-idrepo-client\",\"secretKey\":\"QTGizTYN4US0XHOU\",\"appId\":\"idrepo\"}}"
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

Deps + host app up, then from git clone root `api-test/`:

```bash
./run-local-smoke.sh smoke    # Windows: run-local-smoke.bat smoke
```

See [`api-test/README.md`](../../api-test/README.md). Wipe first if leftover identities cause `IDR-IDC-012`.

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
| `status` / `logs` | `docker compose ps` / follow logs (default `keymanager-service`) |

### 6 GB Docker Desktop (low-mem)

1. Docker Desktop → Settings → Resources → Memory → **6 GB** → Apply & restart.
2. Stop other compose projects.
3. `./run-local-stack.sh up-low-mem` (or `wipe-low-mem` for a clean DB).
4. Start the host app with `run-idrepo-local.*` — its heap is **extra** laptop RAM (~1–2 GB), not the container budget.

Overlay: [`deps/docker-compose.low-mem.yml`](deps/docker-compose.low-mem.yml).

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

Swagger (host app up):

| API | UI |
|-----|-----|
| Identity | http://localhost:8090/idrepository/v1/identity/swagger-ui/index.html |
| VID | http://localhost:8090/idrepository/v1/vid/swagger-ui/index.html |
| Credential service | http://localhost:8090/v1/credentialservice/swagger-ui/index.html |
| Credential request | http://localhost:8090/v1/credentialrequest/swagger-ui/index.html |

---

## Layout

```text
local-dev-setup/
├── README.md                 ← this file
├── LOCAL-DEV-SETUP.md        ← detailed human guide
├── AGENTS.md                 ← agent rules
├── run-local-stack.sh        ← prep / up / wipe / smoke (bash)
├── run-idrepo-local.bat|.sh  ← host JVM + wipe / test / sonar
├── keys/                     ← PKCS12 (gitignored)
└── deps/
    ├── docker-compose.yml    ← deps only
    ├── init.sql
    ├── additional_jars/      ← kernel-ref-idobjectvalidator.jar (loader.path)
    ├── mosip-config/
    ├── wiremock/mappings/id-repository.json
    ├── keymanager/           ← DDL + bootstrap (static key DML is inlined in init.sql)
    └── websub-partner-ack/
```

---

*Last updated: 2026-10-13.*
