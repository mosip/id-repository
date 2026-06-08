# ID Repository — Agent Guide
This file provides guidance to AI agents when working with code in this repository.

## Project Overview

**ID Repository** is the authoritative identity store in the MOSIP (Modular Open Source Identity Platform) platform. It is responsible for secure storage and lifecycle management of foundational identity data (UIN-linked demographic + biometric data), Virtual IDs (VIDs), and verifiable credential issuance.

MOSIP ID Lifecycle: Registration Processor creates/updates identities via Identity Service → VID Service generates revocable tokens for privacy → Credential Service issues credentials (auth, eKYC, QR, eUIN) to partners → ID Authentication consumes these for online verification.

---

## Repository Layout

```
id-repository/                        ← repo root
├── id-repository/                    ← Maven multi-module project
│   ├── pom.xml                       ← parent POM (id-repository-parent)
│   ├── id-repository-core/           ← shared library (DTOs, entities, utils, SPIs)
│   ├── id-repository-identity-service/  ← identity CRUD service (port 8090)
│   ├── id-repository-vid-service/    ← VID lifecycle service (port 8091)
│   ├── credential-request-generator/ ← batch: triggers credential issuance (port 8092)
│   ├── credential-service/           ← credential issuance service
│   └── id-repository-salt-generator/ ← one-time batch: populate encryption salts
├── api-test/                         ← functional API tests (RestAssured + YAML)
├── db_scripts/                       ← PostgreSQL init scripts
├── db_release_scripts/               ← release DB scripts
├── db_upgrade_scripts/               ← DB migration scripts
├── deploy/                           ← Kubernetes deployment shell scripts
├── helm/                             ← Helm charts
└── docs/                             ← design docs and configuration guide
```

> The credential feeder (`id-repository-credentials-feeder`) has been moved to [mosip-utilities](https://github.com/mosip/mosip-utilities).

---

## Build System

| Item | Value |
|------|-------|
| Language | Java 21 (JDK 21.0.3) |
| Build tool | Maven 3.9.6 |
| Spring Boot | 2.0.2.RELEASE |
| Spring Cloud Config | 2.0.0.RELEASE |
| Spring Batch | 4.0.1.RELEASE |
| Packaging | Executable JARs via `spring-boot-maven-plugin` |

**Standard build command:**
```bash
mvn clean install -Dmaven.javadoc.skip=true -Dgpg.skip=true
```

**Skip tests:**
```bash
mvn clean install -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true
```

**Run a specific service locally:**
```bash
java -Dspring.profiles.active=<profile> \
     -Dspring.cloud.config.uri=<config-url> \
     -Dspring.cloud.config.label=<config-label> \
     -jar id-repository/<service-dir>/target/<service>.jar
```

Swagger UI: `http://localhost:<port>/v1/<service>/swagger-ui/index.html`

---

## Modules

### id-repository-core
Shared library consumed by all services. Not deployed standalone.

Key packages under `io.mosip.idrepository.core`:

| Package | Contents |
|---------|----------|
| `dto` | Request/response DTOs |
| `entity` | JPA entities |
| `repository` | Spring Data repositories |
| `spi` | Service Provider Interfaces |
| `manager` | Domain managers (credential, anonymous profile, etc.) |
| `util` | Utilities (TokenIDGenerator, CryptoUtil, etc.) |
| `security` | Auth filters and handlers |
| `config` | Spring configuration beans |
| `exception` | Custom exceptions |
| `httpfilter` | HTTP request/response filters |
| `constant` | Enums and constants |

---

### id-repository-identity-service
Manages the full lifecycle of UIN-linked identity records.

- **Port:** 8090
- **Context path:** `/idrepository/v1/identity`
- **Main class:** `IdRepoBootApplication`
- **Database:** `mosip_idrepo`

Key packages under `io.mosip.idrepository.identity`:

| Package | Contents |
|---------|----------|
| `controller` | REST endpoints (add, update, retrieve identity; update UIN status) |
| `service` | Business logic |
| `helper` | Biometric extraction, object store integration |
| `provider` | Biometric SDK provider |
| `validator` | Identity schema validation |
| `config` | Beans, datasource setup |
| `entity` | Identity, document entities |
| `repository` | JPA repositories |

**Core flow:** Registration Processor calls Identity Service → Key Manager encrypts data → Biometric SDK extracts templates → data stored in `mosip_idrepo` + Object Store → WebSub event published for downstream consumers.

**Local dev dependency:** requires `kernel-auth-adapter.jar` and a Biometric SDK jar (or mock-sdk) on the classpath.

---

### id-repository-vid-service
Creates and manages Virtual IDs — revocable tokens mapped to UINs for privacy protection.

- **Port:** 8091
- **Context path:** `/idrepository/v1/vid`
- **Main class:** `VidBootApplication`
- **Database:** `mosip_idmap`

Key packages under `io.mosip.idrepository.vid`:

| Package | Contents |
|---------|----------|
| `controller` | REST endpoints (create, update, revoke VID; retrieve UIN by VID) |
| `service` | VID generation and policy enforcement |
| `provider` | External provider integrations |
| `validator` | VID request validation |
| `entity` | VID entities |
| `repository` | JPA repositories |

VID policies (perpetual, temporary, one-time) are configured externally via `mosip-vid-policy.json`.

---

### credential-request-generator
Spring Batch application that initiates credential issuance workflows.

- **Port:** 8092
- **Context path:** `/idrepository/v1/credentialrequest`
- **Type:** Spring Batch job

Key packages under `io.mosip.credential.request.generator`:

| Package | Contents |
|---------|----------|
| `batch` | Job and step configuration |
| `service` | Credential request logic |
| `integration` | WebSub, partner service clients |
| `controller` | REST endpoints for job control |
| `api` | External API adapters |
| `entity` | Credential request entities |

---

### credential-service
Issues verifiable credentials to authorized partners.

**Five default credential types:**

| Type | Purpose |
|------|---------|
| `auth` | Online Verification Partners (authentication / eKYC) |
| `qrcode` | QR code credentials |
| `euin` | Electronic UIN card |
| `reprint` | Reprint partner credentials |
| `vercred` | W3C Verifiable Credentials |

---

### id-repository-salt-generator
One-time Spring Batch job to populate encryption/hashing salt values in `mosip_idrepo` and `mosip_idmap` databases. Run once per environment during initial setup.

---

## Code Conventions

**Package structure** (consistent across all services):
```
controller/    REST API layer
service/       Business logic
entity/        JPA domain objects
repository/    Spring Data interfaces
dto/           Request/response objects
validator/     Input validation
config/        Spring @Configuration classes
exception/     Custom exceptions
util/          Helpers and utilities
constant/      Enums and string constants
httpfilter/    Servlet filters
interceptor/   HandlerInterceptors
```

**Naming:**
- Service classes: `*ServiceImpl` implementing a `*Service` interface
- Controllers: `*Controller`
- Entities match table names in snake_case
- REST context path pattern: `/idrepository/v1/{module-name}`

**Frameworks / libraries:**
- Lombok for boilerplate reduction — use `@Data`, `@Slf4j`, etc.
- Springfox Swagger 2 + SpringDoc OpenAPI for API docs
- JUnit 4 + Mockito 3 + PowerMock for unit tests
- H2 in-memory DB for tests; PostgreSQL for runtime

---

## Testing

**Framework:** JUnit 4 + Mockito 3 + PowerMock

**Test location:** mirrors source structure under `src/test/java/io/mosip/idrepository/<module>/`

**JVM flags required for tests** (already in parent POM surefire config):
```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/sun.security.jca=ALL-UNNAMED
```

**Sonar coverage exclusions** (do not write tests for these — excluded by convention):
- `**/constant/**`, `**/config/**`, `**/httpfilter/**`
- `**/dto/**`, `**/entity/**`, `**/repository/**`
- `**/*BootApplication.java`

**Functional / API tests** live in `api-test/` — see `api-test/CLAUDE.md` for full details on YAML test cases, HBS template generation, and handle mutation logic.

Run unit tests only:
```bash
mvn test -pl id-repository/id-repository-identity-service
```

---

## Configuration

Services use **Spring Cloud Config** — a running config server is required for local development.

Bootstrap properties per service (`src/main/resources/bootstrap.properties`):
```properties
server.port=<port>
server.servlet.path=/idrepository/v1/<module>
spring.cloud.config.uri=<config-server-url>
spring.cloud.config.name=application,id-repository
```

Key external config files (in config repo, not this repo):
- `application-default.properties`
- `id-repository-default.properties`
- `credential-request-default.properties`
- `identity-mapping.json` — maps identity schema fields
- `mosip-vid-policy.json` — VID type policies

---

## Databases

| Database | Used by |
|----------|---------|
| `mosip_idrepo` | Identity Service — demographic data, biometric file references |
| `mosip_idmap` | VID Service, Salt Generator — VID mappings, encryption salts |

Scripts: `db_scripts/` (init), `db_upgrade_scripts/` (migrations).

---

## Integration Points

| System | How |
|--------|-----|
| Registration Processor | Calls Identity Service REST APIs to create/update UINs |
| ID Authentication | Consumes credential events via WebSub |
| Resident Services | Calls VID Service to generate/revoke VIDs |
| Key Manager | Encrypts/decrypts identity data |
| Biometric SDK | Extracts biometric templates during identity storage |
| WebSub | Event bus for UIN lifecycle events (update, auth-lock, etc.) |
| Object Store | Stores biometric and document files |
| Partner Management Service | Resolves credential partner policies |

---

## Deployment

**Kubernetes (production):**
```bash
export KUBECONFIG=~/.kube/<cluster.config>
cd deploy && ./install.sh   # deploy
cd deploy && ./restart.sh   # restart
cd deploy && ./delete.sh    # teardown
```

Helm charts are in `helm/`.

**Docker (quick demo):**
```bash
docker pull mosipid/id-repository-identity-service:<version>
docker pull mosipid/id-repository-vid-service:<version>
docker pull mosipid/credential-service:<version>
docker pull mosipid/credential-request-generator:<version>
docker pull mosipid/id-repository-salt-generator:<version>
```

---

## Key Files for Common Tasks

| Task | Files to look at |
|------|-----------------|
| Add/change an Identity API endpoint | `id-repository-identity-service/src/main/java/.../controller/IdRepoController.java` |
| Change identity business logic | `id-repository-identity-service/src/main/java/.../service/IdRepoService.java` |
| Modify VID lifecycle | `id-repository-vid-service/src/main/java/.../service/VidService.java` |
| Change credential types or issuance | `credential-service/src/main/java/.../service/` |
| Shared DTOs or entities | `id-repository-core/src/main/java/io/mosip/idrepository/core/` |
| DB schema changes | `db_scripts/mosip_idrepo/` or `db_scripts/mosip_idmap/` |
| Config properties reference | `docs/configuration.md` |

---

## CI/CD & Quality

- **CI:** GitHub Actions — `.github/workflows/push-trigger.yml` triggers Maven build on push
- **Code quality:** SonarCloud (`mosip_id-repository` project)
- **Publishing:** Snapshots published to Sonatype; GPG signing enabled for Maven Central (skip with `-Dgpg.skip=true` locally)
- **License:** Mozilla Public License 2.0

---

## Common Pitfalls

- **Config server must be running** before starting any service locally — services fail fast if Spring Cloud Config is unreachable.
- **Biometric SDK jar is required** for Identity Service to start; use [mock-sdk](https://github.com/mosip/mosip-mock-services/tree/master/mock-sdk) for local dev.
- **Salt generator must run once** before Identity or VID services can encrypt/hash data.
- **Do not GPG-sign locally** — always pass `-Dgpg.skip=true` unless publishing to Maven Central.
- **Module versions must stay in sync** — id-repository-core version must match what identity/VID services declare as a dependency.