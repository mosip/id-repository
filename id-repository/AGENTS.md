# id-repository/ (Maven parent)

Spring Boot 4.1.2 · Spring Cloud 2025.1.4 · versions only in parent POM.

```text
id-repository/
├─ id-repository-core/            lib io.mosip.idrepository.core.*
├─ id-repository-service/         HTTP: identity, vid, credential, pipeline, manager
├─ id-repository-salt-generator/  one-shot salt Job
└─ local-dev-setup/               Docker deps (own AGENTS.md)
```

Build: `mvn clean install` · service: `mvn -pl id-repository-service -am package -DskipTests "-Dgpg.skip=true"` (PowerShell: quote gpg).

## Rules

- Domain packages → service. Core = `core.*` only. Salt package → salt-generator only.
- One POM / module; no duplicate `<dependency>`; never commit `effective-*.xml`.
- `kernel-auth-adapter` once from parent `dependencyManagement`.
- Bean overriding off → `@Primary` / `@Qualifier`. Never scan `io.mosip.*`.
- Credential issuance in-process (`InProcessCredentialClient` / `InProcessIdentityClient`).
- No class-level `@Transactional` on `CredentialStatusManager`.
- Surefire: `<skipTests>${skipTests}</skipTests>` — CLI `-DskipTests` works.
