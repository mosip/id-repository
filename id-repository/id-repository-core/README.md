# ID Repository Core

## Overview

Shared Maven library aligned with upstream [`id-repository-core`](https://github.com/mosip/id-repository/tree/develop/id-repository/id-repository-core). Contains **`io.mosip.idrepository.core.*` only** — DTOs, constants, security, repositories, entities, validators, and utilities consumed by IDA and other MOSIP modules.

Domain business logic (`identity`, `vid`, `credential`) lives in **`id-repository-service`**, not here.

Salt population lives in **`id-repository-salt-generator`**.

## Package layout

| Package | Purpose |
|---------|---------|
| `io.mosip.idrepository.core.constant.*` | Shared constants (`IdRepoConstants`, `RestServicesConstants`, …) |
| `io.mosip.idrepository.core.dto.*` | Request/response DTOs (IDA-facing) |
| `io.mosip.idrepository.core.entity.*` | Shared JPA entities (`CredentialRequestStatus`, `UinHashSalt`, …) |
| `io.mosip.idrepository.core.repository.*` | Shared repositories |
| `io.mosip.idrepository.core.config.*` | Primary datasource, cache, Hikari helpers |
| `io.mosip.idrepository.core.util.*` | `EnvUtil`, `RestUtil`, `SaltUtil`, … |
| `io.mosip.idrepository.core.security.*` | `IdRepoSecurityManager` |
| `io.mosip.idrepository.core.validator.*` | `BaseIdRepoValidator`, `IdRepoValidationMessageHelper` |

## Build

```bash
cd id-repository
mvn install -pl id-repository-core -am
```
