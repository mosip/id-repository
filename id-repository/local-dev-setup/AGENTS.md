# AGENTS.md — Local docker-compose (`id-repository/local-dev-setup/`)

> Agent guide for running **id-repository-service** locally with mocks (Postgres, config-server, WireMock, BioSDK, MinIO, keymanager, datashare).  
> Human walkthrough: [`LOCAL-DEV-SETUP.md`](LOCAL-DEV-SETUP.md).  
> Java/Maven: [`../AGENTS.md`](../AGENTS.md). Infra (DB/Helm/deploy): [repo root `AGENTS.md`](../../AGENTS.md).

---

## Guide index (this folder)

| Area | Path | Notes |
|------|------|-------|
| Agent rules (this file) | `AGENTS.md` | Compose deps, stubs, pitfalls |
| Human setup guide | [`LOCAL-DEV-SETUP.md`](LOCAL-DEV-SETUP.md) | Clone → build → up |
| Compose stack | [`docker-compose/docker-compose.yml`](docker-compose/docker-compose.yml) | Full stack on published ports |
| DB init | [`docker-compose/init.sql`](docker-compose/init.sql) | 4 DBs + DDL + local salts |
| Bundled config | [`docker-compose/mosip-config/`](docker-compose/mosip-config/) | Config-server native files |
| WireMock stubs | [`docker-compose/wiremock/`](docker-compose/wiremock/) | Auth, PMS, idschema, UIN, WebSub, BioSDK zip |
| Ordered restart | [`docker-compose/restart-idrepo.bat`](docker-compose/restart-idrepo.bat) / [`.sh`](docker-compose/restart-idrepo.sh) | Deps healthy → then id-repo |
| Mint IAM JWT | [`docker-compose/mint-local-iam-token.bat`](docker-compose/mint-local-iam-token.bat) / [`.sh`](docker-compose/mint-local-iam-token.sh) | Refresh WireMock auth stubs from local Keycloak |
| Local Keycloak | [`docker-compose/keycloak/`](docker-compose/keycloak/) | Realm import + `bootstrap_and_mint_token.py` |
| PKCS12 keys | [`keys/`](keys/) | Keymanager HSM substitute |
| BioSDK zip prep | `prepare-biosdk-mock-lib.bat` / `.sh` | Writes `wiremock/__files/mock-sdk.zip` |
| Cross-platform runner | [`run-local-stack.sh`](run-local-stack.sh) | Mac / Linux / Windows (Git Bash): `up` `restart` `prep` `build` `smoke` |

---

## Stack map

| Service | Compose name | Host port | Notes |
|---------|--------------|-----------|-------|
| PostgreSQL | `database` | `5455` | `mosip_idrepo`, `mosip_idmap`, `mosip_credential`, `mosip_keymgr` |
| Config Server | `config-server` | `51001`→`51000` | Mounts `./mosip-config` |
| WireMock | `mock-service` | `8082` | Auth, PMS, masterdata, idgenerator (proxied), `/hub/`, BioSDK zip |
| Local Keycloak | `keycloak` | `8081`→`8080` | Realm `mosip`; ~10y access tokens; admin `admin`/`admin` |
| Keycloak init | `keycloak-init` | — | One-shot: roles + client + 10y lifespan (no static JWT file) |
| Auth token bridge | `auth-token-bridge` | — | Live `clientidsecretkey` → Keycloak; WireMock proxies here |
| UIN / VID generator | `uin-generator` | — | Timestamp + Verhoeff; WireMock proxies `/v1/idgenerator/uin` and `/vid` |
| BioSDK | `biosdk-service` | `8083`→`9099` | Needs `mock-sdk.zip`; custom entrypoint forces `unzip -o` |
| MinIO | `minio` | `9000`/`9001` | + one-shot `minio-init` |
| Key Manager | `keymanager-service` | `8088` | PKCS12; Spring Security auto-config excluded locally |
| Key bootstrap | `keymanager-init` | — | One-shot: ROOT + ID_REPO master keys |
| Keys generator | `keys-generator` | — | One-shot: [keys-generator](https://github.com/mosip/keymanager/tree/v1.4.1-rc.1/kernel/keys-generator) fills `data_encrypt_keystore` (≥1000) + IDENTITY_CACHE SecretKey in PKCS12 **before** keymanager starts |
| Keys expiry fix | `keys-generator-expiry` | — | Sets IDENTITY_CACHE / ID_REPO / ROOT alias expiry to 10 years; keymanager `depends_on` this |
| Data Share | `datashare-service` | `8097` | Needs `additional_jars/kernel-auth-adapter.jar` |
| ID-Repository | `id-repository-service` | `8090` | Mounts `id-repository-service/target` jar |

### Databases (pgAdmin / psql)

Do **not** look under `postgres` → `public` (empty). Use:

| Database | Schema | Tables of interest |
|----------|--------|--------------------|
| `mosip_idrepo` | `idrepo` | `uin`, `uin_draft`, salts, biometrics/docs |
| `mosip_idmap` | `idmap` | VID + salts |
| `mosip_credential` | `credential` | Credential + Spring Batch |
| `mosip_keymgr` | `keymgr` | Key policy / store |

User/password for app roles: `*user` / `mosip123` (see `init.sql`).

---

## Agent rules

### Do

1. Build jar before compose:  
   `cd id-repository && mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true`
2. One-time files (gitignored) before first `up` — see [`LOCAL-DEV-SETUP.md`](LOCAL-DEV-SETUP.md):  
   - `keys/mosip-idrepo-ks.p12` (password `qwerty@1234`)  
   - `docker-compose/additional_jars/kernel-auth-adapter.jar` (1.3.1)  
   - BioSDK: `prepare-biosdk-mock-lib.bat` / `.sh` → `wiremock/__files/mock-sdk.zip`
3. Start / recreate stack with dependency order:  
   `docker compose up -d` **or** `docker-compose/restart-idrepo.bat`  
   **Never** rely on `docker compose restart id-repository-service` alone — it skips `depends_on` health waits.
4. Keep `id-repository-service` `depends_on` on **healthy** deps (database, config, mock, minio+init, keymanager+init, datashare, biosdk).
5. Point local overrides only in this tree (`mosip-config/`, WireMock, compose JVM `-D…`). Do not edit an external `mosip-config` checkout for laptop runs.
6. Host → config-server uses **`http://localhost:51001/config`** (compose `51001→51000`). Inside containers: `http://config-server:51000/config`.
7. After `docker compose down -v`, re-run `up -d` so `init.sql` re-applies (DDL + salts + keymgr seed).
8. For Add Identity / JMeter: fetch UIN from `GET http://localhost:8082/v1/idgenerator/uin` — WireMock proxies to `uin-generator` (timestamp + counter, **Verhoeff-valid**, pattern-filtered). Plain random digits fail `IDR-IDC-002`.
9. Draft create with `?UIN=` requires that UIN already in `idrepo.uin`. Empty DB after wipe → omit UIN (generator path) or Add Identity first.

### Do not

1. Expect tables under database `postgres` / schema `public`.
2. Deploy salt-generator as a long-lived container here — local salts are seeded in `init.sql` (0–999).
3. Put `{cipher}`-only values in local overrides without a working decrypt key.
4. Leave BioSDK on stock `configure_biosdk.sh` unzip without `-o` — restarts hang on interactive replace prompts.
5. Use **websub-mock** (`wiremock/websub-mock/server.py`) for hub subscribe/publish with signed `notifyStatus` delivery — required for credential request final status (`KER-WSC-106` if signature missing).
6. Point `keycloak.internal.url` / `keycloak.external.url` at **local** `http://keycloak:8080` (not `iam.dev.mosip.net`) so OIDC issuer/JWKS match live JWTs. Keep audience claim validation **on**.
7. Do **not** put a static JWT in `auth-client-token.json` — it must proxy to `auth-token-bridge`.
8. Do **not** edit `ValidateTokenHelper` / other kernel-auth-adapter shadows for local IAM — use `auth.server.admin.oidc.userinfo.url=http://auth-token-bridge:8086/v1/oidc/userinfo` (Cookie → Bearer adapter).

---

## Local Keycloak / live auth JWT

```powershell
cd id-repository\local-dev-setup\docker-compose
.\mint-local-iam-token.bat
# Admin UI: http://localhost:8081/auth  (admin / admin)
# Client: mosip-idrepo-client / QTGizTYN4US0XHOU  (~10y accessTokenLifespan)
```

Auth path: `KEYBASEDTOKENAPI` → WireMock → `auth-token-bridge` → Keycloak `client_credentials` (fresh JWT each call).

---

## Startup / restart recipes

Prefer the cross-platform script (Git Bash on Windows):

```bash
cd id-repository/local-dev-setup
./run-local-stack.sh build
./run-local-stack.sh up
./run-local-stack.sh smoke
./run-local-stack.sh restart   # after jar rebuild
./run-local-stack.sh wipe      # down -v + up
```

Raw Compose:

```powershell
cd id-repository\local-dev-setup\docker-compose

# Full stack (respects depends_on)
docker compose up -d

# Ordered id-repo recreate (deps healthy first)
.\restart-idrepo.bat

# Wipe DB volumes + full recreate
docker compose down -v
docker compose up -d
```

Smoke:

```powershell
# or: ./run-local-stack.sh smoke
curl.exe -s http://localhost:8090/actuator/health
curl.exe -s http://localhost:8088/v1/keymanager/actuator/health
curl.exe -s http://localhost:8082/v1/idgenerator/uin
curl.exe -s "http://localhost:8082/v1/masterdata/idschema/latest?schemaVersion=0"
```

---

## WireMock stubs (must-have)

| Mapping | Purpose |
|---------|---------|
| `auth-client-token.json` | **Proxy** → `auth-token-bridge` → live Keycloak JWT (not hardcoded) |
| `keycloak-oidc-*.json` | **Proxy** → `keycloak:8080` (token / userinfo / certs) |
| `auth-validate-token.json` | Static validate stub (authmanager shape) |
| `masterdata-idschema-latest.json` | Identity JSON schema (`schemaJson`) |
| `idgenerator-uin.json` | Proxies to `uin-generator` (timestamp + Verhoeff-valid unique UIN) |
| `websub-mock` (`:8085`) | Hub mock: subscribe + HMAC deliver; on `*/CREDENTIAL_ISSUED` auto-acks `CREDENTIAL_STATUS_UPDATE` with **STORED** |
| `websub-hub.json` | Legacy WireMock `/hub` accept-only stub (unused when `mosip.websub.url` points at websub-mock) |
| `biosdk-download.json` | Serves `mock-sdk.zip` |
| PMS / audit mappings | Partners, policies, audit |

Rebuild idschema body from schema file:

```powershell
cd wiremock
python build-idschema-stub.py
# optional: --from-dev-export path\to\dev-idschema.json
```

---

## Keymanager local notes

- Auth adapter + CSRF caused encrypt failures historically; compose runs keymanager with Spring Security auto-config **excluded** and `mosip.auth.filter_disable=true`.
- `keymanager-init` must complete successfully before datashare / id-repo (ROOT + ID_REPO certs).
- PKCS12: `keys/mosip-idrepo-ks.p12`, password `qwerty@1234`.

---

## init.sql pitfalls

| Issue | Fix |
|-------|-----|
| Salt `varchar(36)` vs base64 sha256 (44 chars) | `left(encode(digest(...),'base64'), 36)` |
| `credential-fk.sql` `:dbuname` | `\set dbuname credentialuser` before `\i` |
| Fresh volume required after init changes | `docker compose down -v` then `up -d` |

---

## Common errors → fix

| Symptom | Cause | Action |
|---------|--------|--------|
| `IDR-IDC-002` Invalid UIN | Non-Verhoeff / filtered UIN | Use `GET /v1/idgenerator/uin` (uin-generator via WireMock) |
| `IDR-IDC-007` draft create | UIN not in DB | Add Identity first, or omit `UIN` query param |
| BioSDK `replace … [y/n]? NULL` | Interactive unzip on restart | Compose entrypoint clears dir + `unzip -o` |
| `KER-WSC-106` notifyStatus | Missing `x-hub-signature` | Use websub-mock delivery, or POST with `x-hub-signature: SHA256=<hmac>` secret=`test` |
| `KER-WSC-101` WebSub | Hub unreachable | Ensure `websub-mock` healthy; `mosip.websub.url=http://websub-mock:8085` |
| Encrypt `KER-KMS-*` / CSRF | Keymanager security / missing keys | Check keymanager health + `keymanager-init` logs |
| `mosip.idrepo.crypto.refId.uin` unresolved | Config-server not loaded (`optional` import + connection refused) or profile `local` only | Ensure config-server healthy; compose waits + non-optional import; use profile `default` / `default,local`; rebuild jar |
| id-repo exits immediately | Missing jar under `target/` | Rebuild service module |
| Empty tables in UI | Wrong DB/schema | Open `mosip_idrepo` → `idrepo` |

---

## JMeter / API testing (local)

- Base URL: `http://localhost:8090` (context paths `/idrepository/v1/identity`, `/idrepository/v1/vid`, `/v1/credentialservice`, `/v1/credentialrequest`).
- Auth: local JWT cookie against WireMock validate stubs; or `mosip.auth.filter_disable=true` on id-repo.
- Prefer 1 thread / 1 loop for sanity after stack recreate.

---

*Last updated: 2026-08-11.*
