# id-repository-service

HTTP deployable on :8090. All domain code lives here. Chart: `helm/identity`.
Entry `IdRepositoryBootApplication` (imports `IdRepoLibraryConfig`; excludes duplicate DS/cache/cred/VID configs). Scan via `HttpModeScanConfiguration`.

```text
Controllers (paths are external contracts)
├─ IdRepoController                      /idrepository/v1/identity
├─ IdRepoDraftController                 /idrepository/v1/identity/draft
├─ VidController                         /idrepository/v1/vid
├─ CredentialStoreController             /v1/credentialservice
├─ CredentialRequestGeneratorController  /v1/credentialrequest
└─ VidEventCallbackController            WebSub callback

Pipeline  credreq → credential → identity   pipeline.InProcessCredentialClient / InProcessIdentityClient
Status    manager.CredentialStatusManager   one tx per row
```

## Rules

- New REST = thin controller + `@PreAuthorize` matching core role DTOs.
- Import core wiring; never duplicate core `@Bean`s.
- Build: `mvn -pl id-repository-service -am package -DskipTests -Dgpg.skip=true`.
