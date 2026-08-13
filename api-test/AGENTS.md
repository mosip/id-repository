# AGENTS.md — `api-test/`

> Functional API test rig for MOSIP ID-Repository (`apitest-idrepo`).  
> Parent guide: [repo root `AGENTS.md`](../AGENTS.md).  
> Human walkthrough: [`README.md`](README.md).  
> Service under test: [`id-repository/AGENTS.md`](../id-repository/AGENTS.md).  
> Local stack (optional): [`id-repository/local-dev-setup/AGENTS.md`](../id-repository/local-dev-setup/AGENTS.md).  
> Cluster install of this rig: [`deploy/AGENTS.md`](../deploy/AGENTS.md) (`deploy/idrepo-apitestrig/`).

---

## 1. Purpose

End-to-end **external API** tests (REST Assured + TestNG) for identity, VID, drafts, auth-type status, and related id-repo endpoints.

| Level | Meaning |
|-------|---------|
| **Smoke** | Positive paths only |
| **Regression** (`smokeAndRegression`) | Positive + negative |

Covers only **public** id-repo HTTP contracts — not internal beans or DB unit tests.

---

## 2. Layout

```text
api-test/
├── README.md
├── CLAUDE.md                         # Deep notes: HBS, handles, IdRepoArrayHandle
├── pom.xml                           # artifact apitest-idrepo
├── application.properties
├── Biometric Devices/                # Device certs / digital IDs for bio tests
├── resource/Profile/                 # ISO biometric samples (Auth / Registration)
└── src/main/
    ├── java/io/mosip/testrig/apirig/idrepo/
    │   ├── testscripts/              # AddIdentity, UpdateIdentity*, SimplePost, …
    │   └── utils/                    # IdRepoUtil, IdRepoArrayHandle
    └── resources/
        ├── config/Idrepo.properties  # Env URLs, Keycloak, client secrets
        └── idRepository/             # YAML scenarios + .hbs templates per feature
```

Artifact / runner: `apitest-idrepo` → `MosipTestRunner` (`io.mosip.testrig.apirig.idrepo`).

---

## 3. Key files

| Path | Role |
|------|------|
| `config/Idrepo.properties` | Server/QA env URLs, Keycloak, client secrets |
| `config/Idrepo-local.properties` | Local docker-compose URLs (no secrets; use `run-local-smoke`) |
| `run-local-smoke.bat` / `.sh` | Local compose runner; arg `smoke` or `smokeAndRegression` (default); tees console + `logs/run-local-*.log` |
| `testscripts/AddIdentity.java` | Add identity; email/phone token replace → `IdRepoArrayHandle` |
| `testscripts/UpdateIdentityForArrayHandles.java` | Update identity handle negatives |
| `utils/IdRepoUtil.java` | Schema-field skip (`requiredSchemaFields`) |
| `utils/IdRepoArrayHandle.java` | `selectedHandles` mutations for negatives |
| `resources/idRepository/**/*.yml` | Test cases |
| `resources/idRepository/**/*.hbs` | Request/response templates |

Deeper handle / HBS / duplicate-chain rules: [`CLAUDE.md`](CLAUDE.md).

---

## 4. Build & run

**Prereqs:** JDK 21, Maven 3.9+, Lombok, MOSIP `settings.xml` in `~/.m2` (see README).

```bash
cd api-test
# Edit src/main/resources/config/Idrepo.properties (secrets + env hosts)

mvn clean install -Dgpg.skip=true -Dmaven.gitcommitid.skip=true

cd target
java -jar apitest-idrepo-*-jar-with-dependencies.jar \
  -Dmodules=idrepo \
  -Denv.user=api-internal.<env> \
  -Denv.endpoint=<base_url> \
  -Denv.testLevel=smokeAndRegression
```

Cluster: `deploy/idrepo-apitestrig/install.sh` after id-repo is deployed.

Against **local docker-compose** (does **not** modify server `Idrepo.properties`):

```bash
# 1) Ensure compose is up; recreate WireMock to pick up apitest-proxy-* mappings
cd id-repository/local-dev-setup/docker-compose
docker compose up -d --force-recreate --no-deps mock-service auth-token-bridge
python keycloak/bootstrap_and_mint_token.py   # ensures mosip-testrig-client + realm-management roles

# 2) Copy run-local.env.example to .env.local and fill local-dev secrets
# 3) Build + run
cd ../../../api-test
./run-local-smoke.sh                      # default: smokeAndRegression
./run-local-smoke.sh smoke                # smoke only
# Windows: run-local-smoke.bat [smoke|smokeAndRegression]
# Console output is also written to api-test/logs/run-local-<testLevel>-<timestamp>.log
```

Manual equivalent:

```bash
cd api-test
mvn clean install -Dgpg.skip=true -Dmaven.gitcommitid.skip=true
# export local-dev client secrets (or use .env.local from run-local.env.example), then:
java -Didrepo.propertiesFile=Idrepo-local.properties \
  -Dmodules=idrepo \
  -Denv.user=api-internal.local \
  -Denv.endpoint=http://localhost:8082 \
  -Denv.testLevel=smokeAndRegression \
  -jar target/apitest-idrepo-*-jar-with-dependencies.jar
```

`env.endpoint` is the **WireMock gateway** (`:8082`), which proxies idrepo / keymanager / datashare and stubs remaining health actuators. Prefer `smoke` first when debugging.

---

## 5. Agent rules

### Do

1. Update YAML + HBS under `src/main/resources/idRepository/` when identity/VID/credential **external** request/response shapes change.
2. Keep AddIdentity token order: `$EMAILVALUE$` / `$PHONENUMBERFORIDENTITY$` **before** `IdRepoArrayHandle.replaceArrayHandleValues` (see CLAUDE.md).
3. Use `requiredSchemaFields` so country schemas without a field **skip** instead of fail.
4. Keep duplicate-handle chain order (`_save_withdublicatevalue` → `_withdublicatevalue` → …).
5. After API contract changes, also align OpenAPI under `api-docs/` and Java AGENTS if paths/topics change.
6. Fill secrets only in local/untracked overrides or via `run-local-smoke` env — do not commit real `Idrepo.properties` secrets.
7. For local compose runs use `-Didrepo.propertiesFile=Idrepo-local.properties` (or `run-local-smoke.*`) so server/QA `Idrepo.properties` stays unchanged.

### Do not

1. Change REST paths or WebSub topics in the service without updating these tests.
2. Break IDA/partner-facing response shapes “just for the test rig.”
3. Call `getJSONArray` on handles without `instanceof JSONArray` (phone may be a plain string).
4. Put more-specific `testCaseName.contains(...)` patterns after shorter substrings in `IdRepoArrayHandle` (wrong branch runs silently).
5. Treat this module as a substitute for `id-repository-service` unit tests.
6. Edit `Idrepo.properties` for local compose — use `Idrepo-local.properties` + `run-local-smoke.*` instead.

---

## 6. Related guides

| Need | Guide |
|------|-------|
| Service APIs / packages | [`id-repository/AGENTS.md`](../id-repository/AGENTS.md) |
| Laptop compose + local IAM | [`local-dev-setup/AGENTS.md`](../id-repository/local-dev-setup/AGENTS.md) |
| K8s apitestrig install | [`deploy/AGENTS.md`](../deploy/AGENTS.md) |
| OpenAPI YAML | [`../api-docs/`](../api-docs/) |
| Handle / HBS deep dive | [`CLAUDE.md`](CLAUDE.md) |

---

*Last updated: 2026-08-11.*
