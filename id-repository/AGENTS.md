# Maven parent (`id-repository/`)

JDK 21, Spring Boot 4.1.0, Spring Cloud 2025.1.2. One `pom.xml` per module; versions live in the **parent**.

| Module | Role |
|--------|------|
| `id-repository-core` | Library: `io.mosip.idrepository.core.*` only (IDA API) |
| `id-repository-service` | HTTP deployable: identity, VID, credential, controllers, pipeline |
| `id-repository-salt-generator` | One-shot Job: salt tables only |

**Build:** `mvn clean install` · service-only: `mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true`

Local how-to: [`local-dev-setup/README.md`](local-dev-setup/README.md).

## Rules

- Domain (`identity`, `vid`, `credential`, `pipeline`, `manager`) stays in **service**. Core stays `core.*`.
- Salt package stays in salt-generator. Never run it inside the HTTP JVM.
- One POM per module. Never commit `effective-*.xml` or `help:effective-pom` dumps beside `pom.xml`. Do not duplicate the same `<dependency>` in one POM.
- `kernel-auth-adapter` once, from parent `dependencyManagement`.
- `spring.main.allow-bean-definition-overriding=false`; use `@Primary` / `@Qualifier`.
- Credential issuance is synchronous in-process (`InProcessCredentialClient` / `InProcessIdentityClient`).
- No class-level `@Transactional` on `CredentialStatusManager`.
- Do not scan `io.mosip.*`.
- Do not reintroduce Spring Batch credential jobs as a second HTTP deployment.
