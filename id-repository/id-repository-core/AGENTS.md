# id-repository-core

Published library, `io.mosip.idrepository.core.*` only. Used by IDA and the service. No controllers, boot, salt-generator or domain (`identity` / `vid` / `credential`).

```text
IDA contract (no rename/remove without an IDA release)
├─ DTOs        CredentialRequestIdsDto, AuthtypeStatus, AuthTypeStatusEventDTO, RestRequestDTO
├─ Constants   IdRepoConstants, IdRepoErrorConstants, IDAEventType
├─ Utils       RestUtil, SaltUtil, RestRequestBuilder, IdRepoLogger
├─ Exceptions  RestServiceException, IdRepoRetryException, AuthenticationException
└─ Payloads    credential / Datashare shape, JSON field names, error codes (IDR-CRG-009 …)

Persistence units
├─ @Primary    mosip_idrepo      IdRepoDataSourceConfig: identity salts, Handle, CredentialRequestStatus
├─ VID         mosip_idmap       VID salt repos live in service (VidUinHashSaltRepo)
└─ Credential  mosip_credential  wired from service; needs credentialTransactionManager
```

## Rules

- Default `@Transactional` → idrepo PU. Routing identity vs VID salts wrongly = silent crypto failure.
- Keep `core.*` API stable. Shared DTOs, SPI, helpers, security, salt entities/repos belong here.
- Shared beans (`IdRepoSecurityManager`, `RestRequestBuilder`) use `@Primary` / `@Qualifier`.
- Test after changes: `mvn test -pl id-repository-core`.
