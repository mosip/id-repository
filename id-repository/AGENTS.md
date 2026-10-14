# id-repository/ (Maven parent)

Spring Boot 4.1.0, Spring Cloud 2025.1.2. Versions only in the parent POM.

```text
id-repository/
├─ id-repository-core/            lib io.mosip.idrepository.core.*
├─ id-repository-service/         HTTP: identity, vid, credential, pipeline, manager
├─ id-repository-salt-generator/  one-shot salt Job
└─ local-dev-setup/               Docker deps (own AGENTS.md)
```

Build: `mvn clean install` · service: `mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true` (PowerShell: quote `"-Dgpg.skip=true"`).

## Rules

- Domain packages → service. Core = `core.*` only. Salt package → salt-generator only.
- One POM per module, no duplicate `<dependency>`, never commit `effective-*.xml`.
- `kernel-auth-adapter` once, from parent `dependencyManagement`.
- Bean overriding is off → `@Primary` / `@Qualifier`. Never scan `io.mosip.*`.
- Credential issuance is synchronous in-process (`InProcessCredentialClient` / `InProcessIdentityClient`). No Spring Batch credential job as a second deployment.
- No class-level `@Transactional` on `CredentialStatusManager`.
