# ID-Repository Local Development Setup

Run the consolidated **id-repository-service** on your laptop without the full MOSIP platform. Docker Compose starts Postgres, config-server, WireMock, MinIO, and keymanager. **id-repository-service runs on the host** via `run-idrepo-local.bat` / `run-idrepo-local.sh`.

> **Quick start:** [`README.md`](README.md) (clean Docker + host startup / wipe).  
> **Agents:** stubs, restart order, failure modes → [`AGENTS.md`](AGENTS.md).

---

## What you get

| Service | Host URL / port |
|---------|-----------------|
| **ID-Repository** | `http://localhost:8090` |
| PostgreSQL | `localhost:5455` |
| Config Server | `http://localhost:51100/config` |
| WireMock (IAM, PMS, UIN/VID/RID, WebSub, Data Share, BioSDK) | `http://localhost:8082` |
| Key Manager | `http://localhost:8088/v1/keymanager` |
| MinIO API / Console | `http://localhost:9000` / `9001` (`admin` / `minioadmin`) |

Auth filter is on (`mosip.auth.filter_disable=false`). Local IAM is static WireMock JWTs/JWKS (no Keycloak).

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Git | Latest | |
| Java (JDK) | **21** | `java -version` |
| Maven | **3.9.6+** | `mvn -v` |
| Docker Desktop | Latest | Compose V2 (`docker compose`) |
| curl | Usually preinstalled | Use `curl.exe` on Windows PowerShell |
| ~8 GB RAM free | Recommended | Or 6 GB Docker + low-mem overlay |

**HSM:** Key Manager uses PKCS12 — [`keys/README.md`](keys/README.md).  
**ZK / master keys:** Default is **static SQL inlined in** [`deps/init.sql`](deps/init.sql) (keymgr policy/alias/store/`data_encrypt_keystore`). [`bootstrap.sh`](deps/keymanager/bootstrap.sh) skips `keys-generator` / `generateMasterKey` when `KM_STATIC_SEED=true`. Keep [`keys/mosip-idrepo-ks.p12`](keys/README.md) (password `1234`) matched to that seed across wipes. Fallback: `KM_STATIC_SEED=false` + `keys-generator.jar` via `./run-local-stack.sh prep`. `KER-KMA-004` → recreate `keymanager-service`.

---

## Path layout

Relative to the **git repo root**:

```text
id-repository/                          ← git clone root
├── db_scripts/
└── id-repository/                      ← Maven parent
    ├── id-repository-service/
    └── local-dev-setup/                ← this guide
        ├── README.md                   ← clean startup
        ├── keys/                       ← PKCS12 (gitignored)
        └── deps/
            ├── docker-compose.yml
            ├── mosip-config/           ← bundled Spring Cloud Config
            ├── keymanager/             ← DDL + static seed + bootstrap (jar optional)
            └── wiremock/
                ├── mappings/id-repository.json   ← all HTTP stubs
                └── __files/EMPTY-FILE            ← mount placeholder only
```

Do **not** edit an external `mosip-config` checkout for laptop runs.

---

## Quick start

Prefer the short path in [`README.md`](README.md). Summary:

[`run-local-stack.sh`](run-local-stack.sh) works on macOS, Linux, and Windows (Git Bash / WSL).

```bash
git clone -b develop https://github.com/mosip/id-repository
cd id-repository/id-repository/local-dev-setup

chmod +x run-local-stack.sh run-idrepo-local.sh   # once (macOS / Linux / WSL)
./run-local-stack.sh build
./run-local-stack.sh up              # prep + Docker deps
./run-idrepo-local.sh                # rebuild jar, then start on :8090
# other terminal:
./run-local-stack.sh smoke
```

**Windows (cmd):**

```bat
bash run-local-stack.sh build
bash run-local-stack.sh up
run-idrepo-local.bat
```

On many PCs `bash` is **WSL**, which does not inherit Windows `JAVA_HOME`. Prefer:

```bat
cd id-repository\id-repository\local-dev-setup
bash run-local-stack.sh up
run-idrepo-local.bat
```

(`up` = Docker only; the `.bat` builds with Windows JDK/Maven and starts `:8090`.)

`bash run-local-stack.sh build` works with Git Bash + Windows JDK, or WSL after the script finds a JDK under `/mnt/c/Program Files/Java`.

| Command | What it does |
|---------|----------------|
| `up` | Prep missing files + start Docker deps |
| `up-low-mem` / `LOW_MEM=1 up` | Same with [`docker-compose.low-mem.yml`](deps/docker-compose.low-mem.yml) (~3 GB deps) |
| `restart` / `restart-low-mem` | Ordered recreate of Docker deps |
| `run` / `run-idrepo-local.*` | Rebuild then start host app (`--no-build` to skip Maven) |
| `down` | Stop containers (keep volumes) |
| `wipe` / `wipe-low-mem` | `down -v` then `up` (clean DB; use `wipe-low-mem` to keep caps) |
| `prep` | PKCS12 (+ keys-generator.jar only if no static seed) |
| `build` | `mvn -pl id-repository-service -am package -DskipTests` |
| `status` / `smoke` / `logs` | Status, HTTP checks, Docker logs |

First `up` can take several minutes. After jar changes, restart the host JVM; use `restart` only when Docker deps need recreate.

### 6 GB Docker memory

1. Docker Desktop → Settings → Resources → Memory → **6 GB** → Apply & restart.  
2. Stop other compose projects.  
3. `./run-local-stack.sh up-low-mem` (or `wipe-low-mem`).  
4. Start host app separately — its heap is **outside** the Compose `mem_limit` budget (~1–2 GB more on the laptop).

The overlay caps Postgres, config-server, WireMock (larger heap for inlined BioSDK stubs), websub-partner-ack, keymanager, and MinIO. Plain `wipe` / `restart` drop the overlay unless you use `wipe-low-mem` / `restart-low-mem` or `LOW_MEM=1`.

---

## Manual steps (same result)

From the **git repo root**.

### 1. Clone and build

```bash
git clone -b develop https://github.com/mosip/id-repository
cd id-repository/id-repository
mvn -pl id-repository-service -am clean package -DskipTests -Dgpg.skip=true
```

### 2. One-time local files

Or: `./run-local-stack.sh prep` from `local-dev-setup/`.

**PKCS12** (password must be `1234`):

```bash
keytool -genkeypair -alias bootstrap -keyalg RSA -keysize 2048 -storetype PKCS12 \
  -keystore id-repository/local-dev-setup/keys/mosip-idrepo-ks.p12 \
  -storepass '1234' -keypass '1234' \
  -dname 'CN=mosip-idrepo-local' -validity 3650
```

Only needed when `KM_STATIC_SEED=false`. With static seed, use the committed-pair PKCS12 (password `1234`) and do not replace it.

**WireMock:** no jar build. All stubs are in `wiremock/mappings/id-repository.json` (IAM, idgenerator, WebSub, Data Share, BioSDK).

**kernel-ref-idobjectvalidator.jar:** required at runtime (`-Dloader.path`), not a Maven dependency. Docker wget from `kernel_ref_idobjectvalidator_url`; laptop: `deps/additional_jars/`. `bash run-local-stack.sh prep` fetches it. See [`additional_jars/README.md`](deps/additional_jars/README.md).

### 3. Start deps, then host app

```bash
cd id-repository/local-dev-setup/deps
docker compose up -d --remove-orphans
# wait until healthy: docker compose ps
```

```bash
cd ../   # local-dev-setup/
./run-idrepo-local.sh
# Windows: run-idrepo-local.bat [--no-build]
```

### 4. Smoke checks

```bash
./run-local-stack.sh smoke
# or:
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

### 5. Swagger / Add Identity

| API | Swagger |
|-----|---------|
| Identity | http://localhost:8090/idrepository/v1/identity/swagger-ui/index.html |
| VID | http://localhost:8090/idrepository/v1/vid/swagger-ui/index.html |
| Credential service | http://localhost:8090/v1/credentialservice/swagger-ui/index.html |
| Credential request | http://localhost:8090/v1/credentialrequest/swagger-ui/index.html |

1. Fetch a Verhoeff-valid UIN (do not invent digits — `IDR-IDC-002`):

   ```bash
   curl -s http://localhost:8082/v1/idgenerator/uin
   curl -s http://localhost:8082/v1/ridgenerator/generate/rid/10001/10001
   ```

2. Use that UIN in Add Identity.  
3. Draft `?UIN=` only if that UIN already exists in `idrepo.uin`. Empty DB → Add Identity first, or omit `UIN`.

---

## Day-to-day

From `id-repository/local-dev-setup/`:

| Goal | Command |
|------|---------|
| Start Docker deps | `./run-local-stack.sh up` |
| Start ID-Repository (host) | `./run-idrepo-local.sh` or `run-idrepo-local.bat` |
| Existing jar only | `run-idrepo-local.bat --no-build` |
| Recreate Docker deps | `./run-local-stack.sh restart` |
| Stop (keep DB) | `./run-local-stack.sh down` |
| Wipe DB + re-init | `./run-local-stack.sh wipe` |
| Logs | `./run-local-stack.sh logs` |

### Clear all data and start from scratch

Use this after api-test leftovers (for example `IDR-IDC-012 Record already exists`), schema/init changes, or a corrupted local DB.

```bash
cd id-repository/local-dev-setup
./run-local-stack.sh wipe          # docker compose down -v + up (re-runs init.sql)
./run-idrepo-local.sh              # restart host id-repo on :8090
```

**Windows (cmd):**

```bat
bash run-local-stack.sh wipe
run-idrepo-local.bat
```

What `wipe` does:

- Removes Compose **volumes** (Postgres data, MinIO data, and other named volumes)
- Starts Docker deps again so `init.sql` reloads DDL, salts `0–999`, and keymgr seed
- Keymanager bootstrap re-seeds ZK / master keys when the service becomes healthy

Then re-run api-test (`api-test/run-local-smoke.*`).

**Notes:**

- `wipe` does **not** delete `keys/mosip-idrepo-ks.p12`. Keep it unless you intentionally want a new PKCS12 and a full keymanager re-bootstrap.
- `./run-local-stack.sh down` only stops containers and **keeps** DB data. Use `wipe` (or `docker compose down -v`) when you need a clean database.
- Report URLs may show `http://localhost:8082/...` because `env.endpoint` is WireMock; identity / VID / credential still go to `:8090` via `mosip_components_base_urls` in `Idrepo-local.properties`.

After `down -v`, `init.sql` re-runs (DDL, salts `0–999`, keymgr seed). Start the host app again after deps are healthy.

---

## Host run (Maven / IDE)

Profiles **`default,local`**. Config URI on the host: **`http://localhost:51100/config`** (not `:51000`).  
[`application-local.properties`](../id-repository-service/src/main/resources/application-local.properties) points deps at localhost.

**Maven** (from `id-repository/id-repository`):

```bash
mvn -pl id-repository-service spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dspring.cloud.config.uri=http://localhost:51100/config -Dspring.profiles.active=default,local -Dspring.cloud.loadbalancer.enabled=false"
```

**JAR** (`-D` flags before `-jar`):

```bash
cd id-repository-service
java \
  -Dspring.cloud.config.uri=http://localhost:51100/config \
  -Dspring.profiles.active=default,local \
  -Dspring.cloud.loadbalancer.enabled=false \
  -jar target/id-repository-service-*.jar
```

**IDE:** main class `io.mosip.idrepository.IdRepositoryBootApplication` with the same three `-D` flags.

---

## Databases

| Database | Schema | App user | Password |
|----------|--------|----------|----------|
| `mosip_idrepo` | `idrepo` | `idrepouser` | `mosip123` |
| `mosip_idmap` | `idmap` | `idmapuser` | `mosip123` |
| `mosip_credential` | `credential` | `credentialuser` | `mosip123` |
| `mosip_keymgr` | `keymgr` | `keymgruser` | `mosip123` |

Host `localhost:5455`, superuser `postgres` / `mosip123`. DDL from [`db_scripts/`](../../../db_scripts). Local salts are in `init.sql` — no salt-generator Job.

---

## Local config

| Purpose | Where |
|---------|--------|
| Host (`default,local`) | [`application-local.properties`](../id-repository-service/src/main/resources/application-local.properties) |
| Containers | [`deps/mosip-config/`](deps/mosip-config/) |
| Keystore | `keys/mosip-idrepo-ks.p12` / password `1234` |

| Path | Purpose |
|------|---------|
| `README.md` | Clean startup / wipe |
| `AGENTS.md` | Agent rules |
| `LOCAL-DEV-SETUP.md` | This guide |
| `run-local-stack.sh` | Prep + Compose |
| `keys/` | PKCS12 |
| `deps/wiremock/` | Stubs in `mappings/id-repository.json`; `__files/EMPTY-FILE` only |
| `postman/` | Postman collection + environment for local APIs (from api-test report + JMeter) |
| `deps/keymanager/` | Key Manager DDL + bootstrap (`init.sql` holds static key DML) |

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Config refused on `:51000` | Host uses **`http://localhost:51100/config`** |
| Placeholder `mosip.idrepo.crypto.refId.uin` | Use profiles `default,local`; ensure config-server healthy; `./run-local-stack.sh restart` |
| Nothing on `:8090` | Start host app: `run-idrepo-local.*` |
| BioSDK init fails | Recreate `mock-service`, or set `mosip.biosdk.default.service.url` to an external BioSDK |
| `IDR-IDS-009` / empty CBEFF (~150 B in MinIO) | AddIdentity stored a shell CBEFF — ensure api-test `$BIOVALUE$` / Mock SBI Profile ISOs are present (see api-test README) |
| `KER-KMS-*` | Check PKCS12 + password; `docker compose logs keymanager-service` |
| `IDR-IDC-002` | Use `GET /v1/idgenerator/uin` |
| `IDR-IDC-012` Record already exists | UIN already in DB (re-run / pool reuse). Wipe and restart: see [Clear all data and start from scratch](#clear-all-data-and-start-from-scratch) |
| `IDR-IDC-007` on draft | UIN not in DB — Add Identity first or omit `?UIN=` |
| `KER-WSC-101` | Recreate `mock-service`; host `mosip.websub.url=http://localhost:8082` |
| 503 to localhost deps | `spring.cloud.loadbalancer.enabled=false` (already in `local`) |
| Empty tables in pgAdmin | Open the right DB + schema, not `postgres/public` |
| Changed `init.sql` but DB unchanged | Use `./run-local-stack.sh wipe` (or `docker compose down -v` then `up -d`) |

More detail: [`AGENTS.md`](AGENTS.md).

---

*Last updated: 2026-08-24.*
