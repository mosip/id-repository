# id-repository-core

Published library. Package `io.mosip.idrepository.core.*` only. Consumed by IDA and the service. No controllers, no boot, no salt-generator, no `identity.*` / `vid.*` / `credential.*` domain.

## IDA — do not rename/remove without an IDA release

- DTOs: `CredentialRequestIdsDto`, `AuthtypeStatus`, `AuthTypeStatusEventDTO`, `RestRequestDTO`
- Constants: `IdRepoConstants`, `IdRepoErrorConstants`, `IDAEventType`
- Utils: `RestUtil`, `SaltUtil`, `RestRequestBuilder`, `IdRepoLogger`
- Exceptions: `RestServiceException`, `IdRepoRetryException`, `AuthenticationException`
- Also freeze: credential/Datashare payload shape, those JSON field names, error codes (e.g. `IDR-CRG-009`)

IDA does **not** read id-repo salt tables.

## Datasources

| PU | DB | Notes |
|----|----|-------|
| `@Primary` | `mosip_idrepo` | `IdRepoDataSourceConfig` — identity salts, Handle, CredentialRequestStatus |
| VID | `mosip_idmap` | VID salt repos live in **service** (`VidUinHashSaltRepo`) |
| Credential | `mosip_credential` | Wired from service |

Default `@Transactional` → PU1. Credential PU needs `credentialTransactionManager`. Mis-routing identity vs VID salts = silent crypto failure.

## Rules

- Keep `core.*` API stable. Shared DTOs, SPI, helpers, security, salt entities/repos belong here.
- `@Primary` / `@Qualifier` for shared beans (`IdRepoSecurityManager`, `RestRequestBuilder`).
- After changes: `mvn test -pl id-repository-core`
- Never add `saltgenerator.*` or HTTP/boot code here.
