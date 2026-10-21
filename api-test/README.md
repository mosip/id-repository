# ID Repository API Test Rig

Functional API tests (REST Assured + TestNG) for identity, VID, drafts, auth-type status, and related **external** ID-Repository HTTP APIs.

| Level | `env.testLevel` | What runs |
|-------|-----------------|-----------|
| Smoke | `smoke` | Positive cases only |
| Full | `smokeAndRegression` | Positive + negative |

---

## Prerequisites

- JDK **21**
- Maven **3.9+**
- Lombok
- MOSIP Maven `settings.xml` in `~/.m2` ([copy](https://github.com/mosip/mosip-functional-tests/blob/master/settings.xml))

Windows: Git Bash optional. Linux: also put `settings.xml` under Maven `/conf` if you install Maven system-wide.

Clone:

```sh
git clone https://github.com/mosip/id-repository.git
cd id-repository
```

Paths below are from that **git clone root**. Nested Java/Maven lives in `id-repository/`; this rig lives in `api-test/`.

---

## 1. Run locally (laptop)

Local means: Docker deps + **host** `id-repository-service` on `:8090`. Tests do **not** use `Idrepo.properties` (server/QA). They use `Idrepo-local.properties` and WireMock IAM on `:8082`.

| Piece | URL | Role |
|-------|-----|------|
| ID-Repository | `http://localhost:8090` | Service under test (host JVM) |
| WireMock | `http://localhost:8082` | Local IAM, idgenerator, masterdata, PMS stubs |
| Key Manager | `http://localhost:8088` | Real keymanager container |
| PostgreSQL | `localhost:5455` | `mosip_idrepo`, `mosip_idmap`, `mosip_credential`, `mosip_keymgr` |

There is **no Keycloak**. Tokens come from WireMock.

Full stack notes: [`id-repository/local-dev-setup/README.md`](../id-repository/local-dev-setup/README.md) (detail: [`LOCAL-DEV-SETUP.md`](../id-repository/local-dev-setup/LOCAL-DEV-SETUP.md)).

### Step A — Build and start the service

**Linux / macOS / Git Bash:**

```sh
cd id-repository/id-repository/local-dev-setup
./run-local-stack.sh build
./run-local-stack.sh up
```

In a **second terminal** (keep it running):

```sh
cd id-repository/id-repository/local-dev-setup
./run-idrepo-local.sh
```

**Windows (cmd + Git Bash `bash` or WSL):**

```bat
cd id-repository\id-repository\local-dev-setup
bash run-local-stack.sh up
run-idrepo-local.bat
```

(`up` starts Docker deps; `run-idrepo-local.bat` builds with Windows JDK/Maven and starts `:8090`. Skip a separate `bash … build` unless Git Bash / WSL already has JDK 21.)

Second terminal is not required for the bat — keep that window open while the app runs.
Wait until the host app is up on `:8090`. Recreate WireMock after mapping changes:

```bat
cd id-repository\id-repository\local-dev-setup\deps
docker compose up -d --force-recreate --no-deps mock-service
```

**Clean DB (wipe volumes + re-init):**

```bat
cd id-repository\id-repository\local-dev-setup
bash run-local-stack.sh wipe
run-idrepo-local.bat
```
### Step B — Check the stack

```bat
curl.exe -s http://localhost:8090/actuator/health
curl.exe -s http://localhost:8082/__admin/health
curl.exe -s http://localhost:8088/v1/keymanager/actuator/health
curl.exe -s http://localhost:8082/v1/idgenerator/uin
```

All four should respond. UIN from idgenerator must be Verhoeff-valid (do not invent a UIN).

### Step C — Run api-test

Secrets default to local-dev values (`mosip123`, `local-dev-testrig-secret`, …). Optional override: copy `api-test/run-local.env.example` to gitignored `api-test/.env.local`.

Do **not** edit `Idrepo.properties` for local runs.

**Linux / macOS / Git Bash:**

```sh
cd api-test
./run-local-smoke.sh smoke                 # positives only
./run-local-smoke.sh                       # smokeAndRegression (default)
```

**Windows:**

```bat
cd api-test
run-local-smoke.bat smoke
run-local-smoke.bat
```

The script:

- Preflights `:8090`, `:8082`, `:8088` (skip with `SKIP_PREFLIGHT=1`)
- Rebuilds the fat jar if `src/main/resources` is newer than the jar (YAML/config changes)
- Writes console + `api-test/.local/logs/run-local-<level>-<timestamp>.log`
- Writes HTML reports to `api-test/testng-report/`

### Step D — Optional: Cursor / VS Code

Use **Run and Debug** (do **not** click the green ▶ on `MosipTestRunner.java` — that skips local JVM flags).

| Launch config | What it runs |
|---------------|----------------|
| MosipTestRunner - IDE (local AddIdentity) | `TC_IDRepo_AddIdentity_01` only |
| MosipTestRunner - IDE (local smoke) | `smoke` |
| MosipTestRunner - IDE (local full) | `smokeAndRegression` |

Workspace should include `api-test`. Configs already set `Idrepo-local.properties`, skip partner setup, and WireMock `:8082`.

---

## 2. Run against a server / QA environment

Use this for a deployed MOSIP env (not localhost). Edit **server** properties; do **not** use `run-local-smoke.*` or `Idrepo-local.properties`.

### Step A — Fill server properties

Edit `api-test/src/main/resources/config/Idrepo.properties`:

- `keycloak-external-url`, `db-server`, JDBC URLs
- Client secrets and DB passwords for that environment

Do not commit real secrets.

### Step B — Build

```sh
cd api-test
mvn clean install -Dgpg.skip=true -Dmaven.gitcommitid.skip=true
```

On Windows PowerShell, quote `-D` flags or Maven splits them:

```bat
mvn clean install "-Dgpg.skip=true" "-Dmaven.gitcommitid.skip=true"
```

### Step C — Run the jar

Replace `<env>` (e.g. `qa-java21`) and the gateway / IAM URLs:

```sh
cd api-test/target
java -Dmodules=idrepo \
  -Denv.user=api-internal.<env> \
  -Denv.endpoint=https://api-internal.<env>.mosip.net \
  -Denv.keycloak=https://iam.<env>.mosip.net \
  -Denv.testLevel=smokeAndRegression \
  -jar apitest-idrepo-*-jar-with-dependencies.jar
```

Windows:

```bat
cd api-test\target
java -Dmodules=idrepo -Denv.user=api-internal.<env> -Denv.endpoint=https://api-internal.<env>.mosip.net -Denv.keycloak=https://iam.<env>.mosip.net -Denv.testLevel=smokeAndRegression -jar apitest-idrepo-1.4.1-SNAPSHOT-jar-with-dependencies.jar
```

| Flag | Server example | Meaning |
|------|----------------|---------|
| `env.user` | `api-internal.qa-java21` | Report / run-context prefix |
| `env.endpoint` | `https://api-internal.<env>.mosip.net` | MOSIP internal gateway |
| `env.keycloak` | `https://iam.<env>.mosip.net` | Real Keycloak |
| `env.testLevel` | `smoke` or `smokeAndRegression` | Suite size |

Omit `-Didrepo.propertiesFile` so the runner loads `Idrepo.properties`.

Cluster install of this rig: [`deploy/idrepo-apitestrig/`](../deploy/idrepo-apitestrig/).

### Eclipse (server)

1. Import `api-test` as an existing Maven project.
2. **Run → Run Configurations → Java Application**, main class `io.mosip.testrig.apirig.idrepo.testrunner.MosipTestRunner`.
3. VM arguments (server):

```
-Dmodules=idrepo -Denv.user=api-internal.<env> -Denv.endpoint=https://api-internal.<env>.mosip.net -Denv.keycloak=https://iam.<env>.mosip.net -Denv.testLevel=smokeAndRegression
```

Local Eclipse run (if you must): use the same VM args as the Cursor local launch configs, plus the env vars from `run-local.env.example`. Prefer Cursor named configs on Windows.

---

## Reports

| Output | Path |
|--------|------|
| HTML TestNG report | `api-test/testng-report/` |
| Script console log | `api-test/.local/logs/run-local-<level>-<timestamp>.log` |
| Log4j | `api-test/.local/logs/mosip-api-test.log` |

Report counts: **T** total, **P** passed, **F** failed, **S** skipped (missing dependency), **I** ignored (schema / feature not supported), **KI** known issues.

On `smoke`, most YAML cases are skipped (not failed). A large skip count with a few passes is expected. Use `smokeAndRegression` for negatives.

---

## Local troubleshooting

| Symptom | Fix |
|---------|-----|
| Preflight: `:8090` down | Start `run-idrepo-local.bat` / `.sh` |
| Preflight: `:8082` down | `run-local-stack.sh up` (WireMock) |
| Preflight: `:8088` down | Wait for keymanager bootstrap, or recreate the container |
| `Unknown lifecycle phase ".skip=true"` | PowerShell split `-D`; quote `"-Dgpg.skip=true"` or use `run-local-smoke.bat` |
| Runner hits QA hosts | You used green ▶ on `MosipTestRunner` or omitted `Idrepo-local.properties` |
| `FileNotFoundException` `testCaseInterDependency.json` | File must exist under `api-test/src/main/resources/config/`; rebuild the jar |
| YAML change not picked up | `run-local-smoke` rebuilds when resources are newer than the jar; otherwise `mvn clean install` in `api-test` |
| `IDR-IDC-002` invalid UIN | Use `GET http://localhost:8082/v1/idgenerator/uin` |
| Auth / token failures | Recreate `mock-service`; testrig secret is `local-dev-testrig-secret` |
| `InvalidPathException` `http:\localhost:8082` | Use skip-partner launch / `run-local-smoke` (`-Didrepo.skipPartnerSetup=true`) |
| Empty shell `$BIOVALUE$` / later `IDR-IDS-009` | Mock SBI needs `Profile/Default/Registration/*.iso` under cwd (materialized from `src/main/resources/mds/resource/Profile`) and `Biometric Devices` under the AUTHCERTS/`authCertsPath` keystore path; runner seeds these when `skipPartnerSetup` is on |
| `IDR-IDC-012` Record already exists | Wipe local DB: `id-repository/local-dev-setup` → `bash run-local-stack.sh wipe` then restart host app |
| Empty tables in pgAdmin | Database `mosip_idrepo`, schema `idrepo` (not `postgres` / `public`) |
| Slack / `/home/mosip/testrig/report` errors | Cluster-only; ignore on a laptop |

---

## License

[Mozilla Public License 2.0](https://github.com/mosip/mosip-platform/blob/master/LICENSE)
