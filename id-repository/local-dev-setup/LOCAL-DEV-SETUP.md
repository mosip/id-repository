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

For resource requirements and the low-memory option, see the
[quick setup prerequisites](README.md#prerequisites).

---

## Choose a setup

For the standard script-driven setup, use the
[quick setup](README.md#quick-setup).

Continue below for the manual setup. It explains the individual build,
configuration, and startup steps for developers who need to customize or
troubleshoot the environment.

---

## Manual setup

Use these steps to build and start the components individually. If you
already have the repository, begin at step 2.

### 1. Clone the repository

```bash
# master/develop do not ship local-dev-setup yet — clone the consolidation branch
# (or the release branch once this lands).
git clone --branch IDREPO-1583-consolidate-idrepo-services https://github.com/mosip/id-repository.git
cd id-repository
```

### 2. Build the service

From the Git repository root:

```bash
# Enter the Maven parent directory.
cd id-repository

# Stop any host id-repository JVM first (Windows locks the fat jar during repackage).
# Prefer -Dmaven.test.skip=true (skips compile+run of tests); -DskipTests also works.
mvn -pl id-repository-service -am clean package -Dmaven.test.skip=true -Dgpg.skip=true
```

The second `id-repository` directory is the Maven parent inside the cloned
Git repository.

### 3. Prepare local files

**Keystore:** The default Key Manager configuration uses the committed
`keys/mosip-idrepo-ks.p12`, paired with the static seed in `deps/init.sql`.
Keep this file across database wipes. If it is missing, restore the matching
file; generating a new keystore will not work with the existing seed.
See the [keystore guide](keys/README.md) for the alternative configuration.

**Identity object validator:** The host application requires
`deps/additional_jars/kernel-ref-idobjectvalidator.jar` at runtime. Obtain it
as described in the [additional JARs guide](deps/additional_jars/README.md).

**WireMock:** The local dependency stack uses the supplied HTTP stubs; no
separate WireMock extension JAR is required.

### 4. Start the dependencies and application

From the Maven parent directory used in step 2, start the dependencies:

```bash
cd local-dev-setup/deps
docker compose up -d --remove-orphans
docker compose ps
```

Wait for the dependencies to become healthy. Then start the host application
using one of the options in [Host run](#host-run).

### 5. Smoke checks

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

### 5b. QA: switch Id Schema (optional)

For local QA against another handle layout (email array-handle, licenseNo, …),
switch WireMock’s schema, then **restart** the host app:

```bash
# From id-repository/local-dev-setup/
./run-local-stack.sh idschema email-handle   # or: default | diff-handle
./run-idrepo-local.sh stop && ./run-idrepo-local.sh run
```

Windows cmd:

```bat
bash run-local-stack.sh idschema email-handle
run-idrepo-local.bat stop
run-idrepo-local.bat run
```

Variants, verify curl, and api-test expectations:
[ID schema guide](deps/wiremock/idschema/README.md) and
[README — QA: test with a different Id Schema](README.md#qa-test-with-a-different-id-schema).

### 6. Swagger / Add Identity

For Swagger URLs, see [Swagger](README.md#swagger). For authentication
guidance, see [Call the APIs](README.md#call-the-apis).

1. Fetch a Verhoeff-valid UIN (do not invent digits — `IDR-IDC-002`):

   ```bash
   curl -s http://localhost:8082/v1/idgenerator/uin
   curl -s http://localhost:8082/v1/ridgenerator/generate/rid/10001/10001
   ```

2. Use that UIN in Add Identity.  
3. Draft `?UIN=` only if that UIN already exists in `idrepo.uin`. Empty DB → Add Identity first, or omit `UIN`.

---

## Local data

Docker Compose keeps PostgreSQL and MinIO data in volumes between restarts.
To reset the local environment, see
[Start with empty local data](README.md#start-with-empty-local-data).

Keep `keys/mosip-idrepo-ks.p12` when resetting the data. It must match the
static Key Manager seed.

---

## Host run

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
  -jar id-repository-service/target/id-repository-service-1.4.1-SNAPSHOT.jar
```

Do **not** use a bare `id-repository-service-*.jar` glob: Maven also builds
`-sources.jar` / `-javadoc.jar`, and `java` may pick those and fail with
`no main manifest attribute`. Prefer the explicit SNAPSHOT fat jar (or a glob
that excludes `-sources` / `-javadoc`, as `run-local-stack.sh` does).

Stop the host app before rebuilding — on Windows, an in-use fat jar leaves a
non-runnable plain jar after a failed `spring-boot:repackage`.

The application uses `application-local.properties` to reach dependencies on
localhost. For the script-driven startup, see the
[quick setup](README.md#quick-setup).

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
| `postman/` | Postman collection and environment for local APIs |
| `deps/keymanager/` | Key Manager DDL + bootstrap (`init.sql` holds static key DML) |

---

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
| `IDR-IDC-012` Record already exists | The UIN already exists. If you need a clean database, see [Start with empty local data](README.md#start-with-empty-local-data). |
| `IDR-IDC-007` on draft | UIN not in DB — Add Identity first or omit `?UIN=` |
| `KER-WSC-101` | Recreate `mock-service`; host `mosip.websub.url=http://localhost:8082` |
| 503 to localhost deps | `spring.cloud.loadbalancer.enabled=false` (already in `local`) |
| Empty tables in pgAdmin | Open the right DB + schema, not `postgres/public` |
| Changed `init.sql` but DB unchanged | Use `./run-local-stack.sh wipe` (or `docker compose down -v` then `up -d`) |

More detail: [`AGENTS.md`](AGENTS.md).

---

## Directory layout

Paths are shown relative to the cloned repository:

```text
id-repository/                         Git repository root
├── db_scripts/                        Database scripts
└── id-repository/                     Maven parent
    ├── id-repository-service/         Application source
    └── local-dev-setup/
        ├── README.md                  Quick setup
        ├── LOCAL-DEV-SETUP.md         This manual guide
        ├── keys/                      Local Key Manager keystore
        └── deps/
            ├── docker-compose.yml     Dependency containers
            ├── init.sql               Local database initialization
            ├── mosip-config/          Local configuration files
            ├── keymanager/            Key Manager bootstrap files
            └── wiremock/              Mock service files
```

The local Config Server uses the files in `deps/mosip-config/`; an external
`mosip-config` checkout is not needed for this setup.

*Last updated: 2026-08-24.*
