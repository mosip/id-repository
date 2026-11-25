# ID Repository

[![Maven Package upon a push](https://github.com/mosip/id-repository/actions/workflows/push-trigger.yml/badge.svg?branch=release-1.3.x)](https://github.com/mosip/id-repository/actions/workflows/push-trigger.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=release-1.3.x&project=mosip_id-repository&metric=alert_status)](https://sonarcloud.io/dashboard?branch=release-1.3.x&id=mosip_id-repository)

## Overview

The **ID Repository** is the backbone of the MOSIP identity platform, responsible for the secure storage and lifecycle management of foundational identity data. It serves as the authoritative source for identity records, handling operations such as identity creation, updates, and retrieval.

This repository contains source code and design documents for MOSIP [ID Repository](https://docs.mosip.io/1.2.0/modules/id-repository), which is the server-side module to manage ID lifecycle. The module provides a comprehensive set of REST APIs to interact with identity services, ensuring secure and standardized access to identity information.

## Features

- **Identity Data:** Storage and retrieval of demographic and biometric data.
- **Credentials:** Management and issuance of verifiable credentials.
- **Virtual IDs (VID):** Generation and management of revocable virtual tokens for privacy protection.
- **Salt & Keys:** Security foundations for identity data encryption and hashing.

## Modules

Identity, VID, credential request, and credential store run in **one** HTTP service. The salt generator stays a separate one-shot Job. See each module README and the [service consolidation design](docs/design/service-consolidation.md).

- **[ID Repository Service](id-repository/id-repository-service/README.md):**
  Single HTTP deployable (port `8090`) for identity, draft, VID, credential store, and credential request. Prefixes: `/idrepository/v1/identity`, `/idrepository/v1`, `/v1/credentialservice`, `/v1/credentialrequest`.
- **[Core](id-repository/id-repository-core/README.md):**
  Shared library (`io.mosip.idrepository.core.*`) used by ID Authentication and other MOSIP modules.
- **[Salt Generator](id-repository/id-repository-salt-generator/README.md):**
  One-shot Job (`helm/idrepo-saltgen`) that populates `uin_hash_salt` / `uin_encrypt_salt` in `mosip_idrepo` and `mosip_idmap`.

## Codebase Relocation

The credential feeder has been moved to the [mosip-utilities](https://github.com/mosip/mosip-utilities) repository.

## Database

See [DB guide](db_scripts/README.md) for database setup and migration details.

## Installation

### Prerequisites

The project requires:
- **JDK:** 21.0.3
- **Maven:** 3.9.6
- **kernel-auth-adapter** is a Maven dependency of the service; it does not need to be added to the build path manually.
- **ID object validator:** `kernel-ref-idobjectvalidator.jar` must be on the loader path (`-Dloader.path`). Cluster images download it from the `artifactory-share` ConfigMap; for local runs see [additional jars](id-repository/local-dev-setup/deps/additional_jars/README.md).
- **Biometric SDK:** the service calls a BioSDK service over HTTP (`biosdk-client`). Point it at a running BioSDK service or the [Mock SDK](https://github.com/mosip/mosip-mock-services/tree/master/mock-sdk).

#### For Kubernetes Deployment

* Set KUBECONFIG variable to point to existing K8 cluster kubeconfig file:
    ```text
    export KUBECONFIG=~/.kube/<k8s-cluster.config>
    ```

### Local Setup for Development

For prerequisites, setup, startup, and verification instructions, see the
[local development guide](id-repository/local-dev-setup/LOCAL-DEV-SETUP.md).

## Deployment

For Kubernetes deployment instructions, see the
[deployment guide](deploy/idrepo/README.md). Before installing on an environment that ran the separate services, update `mosip.idrepo.vid.url`, `mosip.idrepo.credrequest.generator.url`, and `mosip.idrepo.credential.service.url` to `http://identity.idrepo` — see the [consolidation design](docs/design/service-consolidation.md#6-service-urls-in-mosip-config).

## Configuration

Refer to the [configuration guide](docs/configuration.md).

## APIs

- One service, port **8090**: `http://localhost:8090` locally, `http://identity.idrepo` in cluster, `https://<api-internal-host>` via gateway
- Prefixes: `/idrepository/v1/identity` (identity + draft) · `/idrepository/v1` (VID) · `/v1/credentialservice` · `/v1/credentialrequest`
- Swagger ("Select a definition" switches domains, Authorize header `Authorization`):
  - http://localhost:8090/idrepository/v1/identity/swagger-ui/index.html
  - http://localhost:8090/idrepository/v1/swagger-ui/index.html
  - http://localhost:8090/v1/credentialservice/swagger-ui/index.html
  - http://localhost:8090/v1/credentialrequest/swagger-ui/index.html
- OpenAPI JSON: same paths with `/v3/api-docs` instead of `/swagger-ui/index.html`
- Health: http://localhost:8090/idrepository/v1/identity/actuator/health (also `info`, `prometheus`)
- Static specs: [`api-docs`](api-docs) · published docs: [MOSIP API docs](https://mosip.github.io/documentation/)
- Full endpoint list: [id-repository-service README](id-repository/id-repository-service/README.md#endpoints)

## Testing

API tests for this module are in [`api-test`](api-test/README.md); `deploy/idrepo-apitestrig` runs them on a cluster. Shared test utilities come from the [Functional Tests repo](https://github.com/mosip/mosip-functional-tests).

## Contribution & Community

• To learn how you can contribute code to this application, [click here](https://docs.mosip.io/1.2.0/community/code-contributions).

• If you have questions or encounter issues, visit the [MOSIP Community](https://community.mosip.io/) for support.

• For any GitHub issues: [Report here](https://github.com/mosip/id-repository/issues)

## License

This project is licensed under the [Mozilla Public License 2.0](LICENSE).
