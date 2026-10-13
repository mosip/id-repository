# id-repository-service

HTTP deployable on **8090**. Hosts identity, VID, credential, credreq — **domain lives here**, not in core.

Entry: `IdRepositoryBootApplication` (imports `IdRepoLibraryConfig`; excludes duplicate DS/cache/cred/VID configs). Scan via `HttpModeScanConfiguration` — not `io.mosip.*`.

| Controller | Path (contract) |
|------------|-----------------|
| `IdRepoController` | `/idrepository/v1/identity` |
| `IdRepoDraftController` | `/idrepository/v1/identity/draft` |
| `VidController` | `/idrepository/v1/vid` |
| `CredentialStoreController` | `/v1/credentialservice` |
| `CredentialRequestGeneratorController` | `/v1/credentialrequest` |
| `VidEventCallbackController` | WebSub callback |

Pipeline: credreq → credential → identity via `pipeline.InProcessCredentialClient` / `InProcessIdentityClient`. Status: `manager.CredentialStatusManager` (per-row txs — not class-level `@Transactional`).

Local how-to: [`../local-dev-setup/README.md`](../local-dev-setup/README.md). Chart: `helm/identity`.

## Rules

- New REST = thin controller + `@PreAuthorize` aligned with core role DTOs. Entities/repos/services stay in this module.
- Import core wiring; do not duplicate core `@Bean`s.
- Salt-generator is not on this classpath.
- Paths above are external contracts.

**Build:** `mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true`
