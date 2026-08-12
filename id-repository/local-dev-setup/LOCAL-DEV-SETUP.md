# ID-Repository Local Development Setup

Run the consolidated **id-repository-service** on your laptop **without** the full MOSIP platform. One Docker Compose stack starts Postgres, config-server, WireMock mocks, BioSDK, MinIO, keymanager, datashare, and id-repository.

> **Agents:** dependency rules, WireMock stubs, and failure modes → [`AGENTS.md`](AGENTS.md).

---

## What you get

| Service | Host URL / port |
|---------|-----------------|
| **ID-Repository** | `http://localhost:8090` |
| PostgreSQL | `localhost:5455` |
| Config Server | `http://localhost:51001/config` |
| WireMock (auth, PMS, UIN, WebSub, …) | `http://localhost:8082` |
| Local Keycloak (mint ~10y JWT into WireMock) | `http://localhost:8081/auth` (`admin`/`admin`) |
| BioSDK | `http://localhost:8083` |
| Key Manager | `http://localhost:8088/v1/keymanager` |
| Data Share | `http://localhost:8097/v1/datashare` |
| MinIO API / Console | `http://localhost:9000` / `http://localhost:9001` (`admin` / `minioadmin`) |

Auth filters are **disabled** in this stack (`mosip.auth.filter_disable=true`), so you can call APIs without Keycloak.

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Git | Latest | |
| Java (JDK) | **21** | `java -version` |
| Maven | **3.9.6+** | `mvn -v` |
| Docker Desktop | Latest | Compose V2 (`docker compose`) |
| curl | Usually preinstalled | Used by smoke checks / BioSDK prep |
| ~8 GB RAM free | Recommended | First image pull is large |

**HSM:** Key Manager uses a local PKCS12 file instead of SoftHSM — see [`keys/README.md`](keys/README.md).

**ZK encrypt keys:** On first stack start, one-shot [`keys-generator`](https://github.com/mosip/keymanager/tree/v1.4.1-rc.1/kernel/keys-generator) seeds `keymgr.data_encrypt_keystore` with **1000** Active keys and writes the KERNEL `IDENTITY_CACHE` secret into the PKCS12; then keymanager starts (so it loads that alias). `keymanager-init` also ensures **IDA** master + **IDA:PUBLIC_KEY** (required for `zkEncrypt`). `keys-generator-expiry` sets alias expiry to **10 years**. If you see `KER-KMA-004 No such alias`, recreate keymanager after keys-generator. If you see `KER-ZKC-002 No unique alias`, ensure IDA:PUBLIC_KEY exists (`getCertificate?applicationId=IDA&referenceId=PUBLIC_KEY`).

---

## Path layout (read this once)

After `git clone`, paths below are relative to the **git repo root**:

```text
id-repository/                          ← git clone root (you are here for most commands)
├── db_scripts/                         ← DDL used by compose init
└── id-repository/                      ← Maven parent
    ├── id-repository-service/          ← jar built into target/
    └── local-dev-setup/                ← this guide
        ├── keys/                       ← PKCS12 (gitignored)
        ├── prepare-biosdk-mock-lib.*
        └── docker-compose/
            ├── docker-compose.yml
            ├── mosip-config/           ← bundled Spring Cloud Config
            ├── additional_jars/        ← kernel-auth-adapter.jar (gitignored)
            └── wiremock/
```

Spring Cloud Config for local compose is **bundled** under `docker-compose/mosip-config/`. Do **not** edit an external `mosip-config` checkout for laptop runs.

---

## Quick start (happy path)

### Recommended: one script (Mac / Linux / Windows)

[`run-local-stack.sh`](run-local-stack.sh) works on **macOS**, **Linux**, and **Windows** (Git Bash or WSL). It prepares missing PKCS12 / auth-adapter / BioSDK zip, then drives Docker Compose.

```bash
git clone -b develop https://github.com/mosip/id-repository
cd id-repository/id-repository/local-dev-setup

chmod +x run-local-stack.sh          # macOS / Linux / WSL (once)
./run-local-stack.sh build           # Maven package (needs JDK 21 + Maven)
./run-local-stack.sh up              # prep + docker compose up -d
./run-local-stack.sh smoke           # health / UIN / idschema
```

**Windows (Git Bash):**

```bash
bash run-local-stack.sh build
bash run-local-stack.sh up
bash run-local-stack.sh smoke
```

| Command | What it does |
|---------|----------------|
| `./run-local-stack.sh` / `up` | Prep missing files + `docker compose up -d` |
| `restart` | Deps healthy, then recreate `id-repository-service` |
| `down` | Stop containers (keep DB volumes) |
| `wipe` | `down -v` then `up` (full DB re-init) |
| `prep` | PKCS12 + `kernel-auth-adapter.jar` + `mock-sdk.zip` only |
| `build` | `mvn -pl id-repository-service -am package -DskipTests` |
| `status` / `smoke` / `logs` | Status, HTTP checks, follow logs |
| `help` | Full usage |

First `up` can take several minutes (image pulls + DB init + key bootstrap). Prefer `restart` over `docker compose restart id-repository-service` alone.

---

### Manual steps (same result without the script)

Do these once, in order, from the **git repo root**.

#### 1. Clone

```bash
git clone -b develop https://github.com/mosip/id-repository
cd id-repository
```

#### 2. Build the service jar

Compose mounts `id-repository-service/target`. Build before the first `up`:

```bash
cd id-repository
mvn -pl id-repository-service -am clean package -DskipTests -Dgpg.skip=true
cd ..
```

> Full multi-module install (optional): `mvn clean install -Dgpg.skip=true` from `id-repository/`.

#### 3. One-time local files (not in git)

Or run `./run-local-stack.sh prep` from `local-dev-setup/` after `build`.

##### 3a. PKCS12 keystore for Key Manager

Create an empty store (Key Manager fills master keys on first boot via `keymanager-init`):

**Windows (PowerShell):**

```powershell
keytool -genkeypair -alias bootstrap -keyalg RSA -keysize 2048 -storetype PKCS12 `
  -keystore id-repository\local-dev-setup\keys\mosip-idrepo-ks.p12 `
  -storepass "qwerty@1234" -keypass "qwerty@1234" `
  -dname "CN=mosip-idrepo-local" -validity 3650
```

**macOS / Linux:**

```bash
keytool -genkeypair -alias bootstrap -keyalg RSA -keysize 2048 -storetype PKCS12 \
  -keystore id-repository/local-dev-setup/keys/mosip-idrepo-ks.p12 \
  -storepass 'qwerty@1234' -keypass 'qwerty@1234' \
  -dname 'CN=mosip-idrepo-local' -validity 3650
```

Password must be exactly `qwerty@1234`. Details: [`keys/README.md`](keys/README.md).

##### 3b. `kernel-auth-adapter.jar` for Data Share

Required under `docker-compose/additional_jars/` (gitignored). After the Maven build in step 2:

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

See [`docker-compose/additional_jars/README.md`](docker-compose/additional_jars/README.md).

##### 3c. BioSDK mock zip

Required before `biosdk-service` starts (~110 MB download, once):

**Windows:**

```cmd
cd id-repository\local-dev-setup
prepare-biosdk-mock-lib.bat
cd ..\..
```

**macOS / Linux:**

```bash
cd id-repository/local-dev-setup
chmod +x prepare-biosdk-mock-lib.sh
./prepare-biosdk-mock-lib.sh
cd ../..
```

This writes `docker-compose/wiremock/__files/mock-sdk.zip`.

#### 4. Start the stack

```bash
cd id-repository/local-dev-setup/docker-compose
docker compose up -d
```

Or from `local-dev-setup/`: `./run-local-stack.sh up`

First start can take several minutes. Wait until services are healthy:

```bash
docker compose ps
# or: ./run-local-stack.sh status
```

You want `healthy` / `running` for long-lived services. One-shots `minio-init` and `keymanager-init` should exit `0`.

After rebuilding the jar, prefer ordered restart:

```bash
./run-local-stack.sh restart
# or: docker-compose/restart-idrepo.bat  /  restart-idrepo.sh
```

#### 5. Smoke checks

```bash
./run-local-stack.sh smoke
```

Or manually:

```bash
curl -s http://localhost:8090/actuator/health
curl -s http://localhost:8088/v1/keymanager/actuator/health
curl -s http://localhost:8082/v1/idgenerator/uin
curl -s "http://localhost:8082/v1/masterdata/idschema/latest?schemaVersion=0"
```

| Check | Expect |
|-------|--------|
| id-repo / keymanager health | `"status":"UP"` |
| idgenerator UIN | JSON with a numeric `uin` |
| idschema | HTTP 200 with `schemaJson` |

Windows: use `curl.exe` if `curl` is an alias.

#### 6. Open Swagger / call APIs

| API | Swagger |
|-----|---------|
| Identity | http://localhost:8090/idrepository/v1/identity/swagger-ui/index.html |
| VID | http://localhost:8090/idrepository/v1/vid/swagger-ui/index.html |
| Credential service | http://localhost:8090/v1/credentialservice/swagger-ui/index.html |
| Credential request | http://localhost:8090/v1/credentialrequest/swagger-ui/index.html |

**Add Identity tips**

1. Fetch a **Verhoeff-valid** UIN from the local generator (do not invent random digits — that fails with `IDR-IDC-002`):

   ```bash
   curl -s http://localhost:8082/v1/idgenerator/uin
   ```

   Each call returns a **new** UIN (timestamp + counter, pattern-filtered). WireMock proxies to the `uin-generator` service.

2. Use that UIN in the Add Identity request body via Swagger (or your client).
3. Draft create with `?UIN=` only works if that UIN already exists in `idrepo.uin`. On an empty DB after wipe: Add Identity first, or omit the `UIN` query param so the generator path is used.

---

## Day-to-day commands

From `id-repository/local-dev-setup/` (cross-platform):

| Goal | Command |
|------|---------|
| Start / recreate stack | `./run-local-stack.sh up` |
| Rebuild jar then refresh id-repo | `./run-local-stack.sh build` then `./run-local-stack.sh restart` |
| Stop containers (keep DB data) | `./run-local-stack.sh down` |
| Wipe DB volumes + full re-init | `./run-local-stack.sh wipe` |
| Logs | `./run-local-stack.sh logs` |

Equivalent raw Compose (from `docker-compose/`): `docker compose up -d` / `down` / `down -v` / `logs -f id-repository-service`.

After `down -v`, `init.sql` runs again (DDL, local salts `0–999`, keymgr policy seed). `keymanager-init` regenerates ROOT / ID_REPO keys.

---

## Optional: run ID-Repository on the host (Maven / IDE)

Prefer the full compose stack (§ Quick start). Use host run only when debugging the app against the same Docker deps.

1. Keep **deps** up; stop the containerized app so port **8090** is free:

   ```bash
   docker compose stop id-repository-service
   ```

2. Use profiles **`default,local`** and config-server on the **published** host port **51001** (not 51000 — that is the port *inside* the container network). Profile `local` alone does not load `id-repository-default.properties` from the native config-server.

[`application-local.properties`](../id-repository-service/src/main/resources/application-local.properties) already points DB / keymanager / datashare / BioSDK / WireMock / MinIO at localhost published ports.

### Option A — Maven

From the **Maven parent** (`id-repository/id-repository`):

```bash
mvn -pl id-repository-service spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dspring.cloud.config.uri=http://localhost:51001/config -Dspring.profiles.active=default,local -Dspring.cloud.loadbalancer.enabled=false"
```

### Option B — JAR

JVM `-D` flags must come **before** `-jar`:

```bash
cd id-repository-service
java \
  -Dspring.cloud.config.uri=http://localhost:51001/config \
  -Dspring.profiles.active=default,local \
  -Dspring.cloud.loadbalancer.enabled=false \
  -jar target/id-repository-service-*.jar
```

### Option C — IntelliJ / VS Code

Open the Maven project at `id-repository/id-repository`, run main class:

`io.mosip.idrepository.IdRepositoryBootApplication`

VM options:

```text
-Dspring.profiles.active=default,local
-Dspring.cloud.config.uri=http://localhost:51001/config
-Dspring.cloud.loadbalancer.enabled=false
```

---

## Databases (pgAdmin / psql)

Do **not** look under database `postgres` → schema `public` (empty). Use:

| Database | Schema | App user | Password |
|----------|--------|----------|----------|
| `mosip_idrepo` | `idrepo` | `idrepouser` | `mosip123` |
| `mosip_idmap` | `idmap` | `idmapuser` | `mosip123` |
| `mosip_credential` | `credential` | `credentialuser` | `mosip123` |
| `mosip_keymgr` | `keymgr` | `keymgruser` | `mosip123` |

- Host: `localhost`, port: `5455`, superuser: `postgres` / `mosip123`
- DDL comes from repo [`db_scripts/`](../../../db_scripts) (+ vendored keymgr DDL under `docker-compose/keymgr/`)
- Local salts are seeded in `init.sql` — you do **not** run the salt-generator Job for this laptop stack

---

## Local config overview

| Purpose | Where |
|---------|--------|
| Host Maven / IDE (`spring.profiles.active=default,local`) | [`../id-repository-service/src/main/resources/application-local.properties`](../id-repository-service/src/main/resources/application-local.properties) |
| Containers (config-server) | [`docker-compose/mosip-config/`](docker-compose/mosip-config/) only |

Inter-service URLs inside compose use Docker DNS (`database`, `keymanager-service`, …). Edit bundled `mosip-config` only when changing the docker stack itself.

| Keystore | Value |
|----------|--------|
| File | `keys/mosip-idrepo-ks.p12` |
| Password | `qwerty@1234` |
| In container | `/home/mosip/config/mosip-idrepo-ks.p12` |

---

## Files in `local-dev-setup/`

| Path | Purpose |
|------|---------|
| `AGENTS.md` | Agent rules: deps, stubs, restart order, errors |
| `LOCAL-DEV-SETUP.md` | This human guide |
| `run-local-stack.sh` | **One script** for Mac / Linux / Windows (Git Bash): prep + compose |
| `keys/` | PKCS12 for keymanager (gitignored `.p12`) |
| `prepare-biosdk-mock-lib.bat` / `.sh` | Creates `mock-sdk.zip` once (also done by `run-local-stack.sh prep`) |
| `docker-compose/docker-compose.yml` | Full stack |
| `docker-compose/restart-idrepo.bat` / `.sh` | Deps healthy → recreate id-repo |
| `docker-compose/init.sql` | 4 DBs, DDL, salts, keymgr DML |
| `docker-compose/mosip-config/` | Bundled Spring Cloud Config |
| `docker-compose/additional_jars/` | `kernel-auth-adapter.jar` for datashare |
| `docker-compose/wiremock/` | Stubs + `__files` (UIN pool, idschema, BioSDK zip) |
| `docker-compose/keymgr/` | Key Manager DDL + policy CSV |

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Config 404 / connection refused on `:51000` | From the **host**, use **`http://localhost:51001/config`**. Port `51000` is only inside the Docker network. |
| `Could not resolve placeholder 'mosip.idrepo.crypto.refId.uin'` | Config-server was unreachable (optional import) or profile was only `local` (empty on native server). Use `default` / `default,local`, ensure config-server is healthy, then `./run-local-stack.sh restart`. |
| `id-repository-service` exits immediately | Missing jar under `id-repository-service/target/`. Rebuild (§2). |
| BioSDK fails / hangs on unzip | Run `prepare-biosdk-mock-lib` so `mock-sdk.zip` exists. Compose uses non-interactive `unzip -o`. |
| Datashare exits: missing `kernel-auth-adapter.jar` | Copy jar into `docker-compose/additional_jars/` (§3b). |
| Keymanager / encrypt `KER-KMS-*` | Confirm `keys/mosip-idrepo-ks.p12` + password `qwerty@1234`. Check `docker compose logs keymanager-init` completed. |
| `IDR-IDC-002` Invalid UIN | Use WireMock `GET /v1/idgenerator/uin` (Verhoeff-valid pool). |
| `IDR-IDC-007` on draft create | UIN not in DB — Add Identity first, or omit `?UIN=`. |
| `KER-WSC-101` WebSub | Ensure `wiremock/mappings/websub-hub.json` is loaded; restart `mock-service`. |
| 503 calling localhost deps from host app | Set `spring.cloud.loadbalancer.enabled=false` (already in `local` profile). |
| Empty tables in pgAdmin | Open the right DB + schema (e.g. `mosip_idrepo` → `idrepo`), not `postgres/public`. |
| Changed `init.sql` but DB unchanged | Init runs only on empty volume: `docker compose down -v` then `up -d`. |

More detail for agents: [`AGENTS.md`](AGENTS.md).

---

*Last updated: 2026-08-07.*
