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

## Services

Identity, VID, credential request, and credential store now run in **one** HTTP service. The salt generator stays a separate one-shot Job. For detailed setup instructions, refer to each module's README:

1. **[ID Repository Service](id-repository/id-repository-service/README.md)** - The single HTTP deployable (port `8090`). It contains identity and draft, VID, credential request generator, and credential store. Callers use the same URL prefixes as before:

   | Domain | URL prefix | Database |
   |---|---|---|
   | Identity and draft | `/idrepository/v1/identity` | `mosip_idrepo` |
   | VID | `/idrepository/v1` (`/vid`, `/draft/vid`, …) | `mosip_idmap` |
   | Credential store | `/v1/credentialservice` | `mosip_credential` |
   | Credential request | `/v1/credentialrequest` | `mosip_credential` |

2. **[Salt Generator](id-repository/id-repository-salt-generator/README.md)** - One-shot Job that fills `uin_hash_salt` and `uin_encrypt_salt` in `mosip_idrepo` and `mosip_idmap`. Deployed only as the `helm/idrepo-saltgen` Job, never inside the HTTP service.
3. **[Core](id-repository/id-repository-core/README.md)** - Published library (`io.mosip.idrepository.core.*`) with shared DTOs, entities, and utilities used by this service and by IDA.

The separate `id-repository-identity-service`, `id-repository-vid-service`, `credential-service`, and `credential-request-generator` modules no longer exist. See the [service consolidation design](docs/design/service-consolidation.md) for what changed, the three-database layout, and the config URLs an environment must update (`mosip.idrepo.vid.url`, `mosip.idrepo.credrequest.generator.url`, `mosip.idrepo.credential.service.url` → `http://identity.idrepo`).

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

### Local Setup (for Development or Contribution)

The quickest path is [local-dev-setup](id-repository/local-dev-setup/README.md). Docker Compose starts Postgres, config server, WireMock, MinIO, and keymanager, and `run-idrepo-local.bat` / `run-idrepo-local.sh` runs the service on the host at `http://localhost:8090`. The steps below are for running against your own config server.

1. **Make sure the config server is running.** For detailed instructions on setting up and running the config server, refer to the [MOSIP Config Server Setup Guide](https://docs.mosip.io/1.2.0/modules/module-configuration).

   **Note:** Refer to the MOSIP Config Server Setup Guide for setup, and ensure the properties mentioned in the configuration section are taken care of. Replace the properties with your own configurations (e.g., DB credentials, IAM credentials, URL).

2. **Clone the repository:**

   ```text
   git clone https://github.com/mosip/id-repository.git
   cd id-repository
   ```

3. **Build the project:**

   ```text
   cd id-repository
   mvn clean install -Dmaven.javadoc.skip=true -Dgpg.skip=true
   ```

   In PowerShell, quote the flags: `"-Dmaven.javadoc.skip=true" "-Dgpg.skip=true"`.

4. **Start the application:**
    - Click the Run button in your IDE, or
    - Run via command:

   ```text
   java -Dspring.profiles.active=<profile> \
        -Dspring.cloud.config.uri=<config-url> \
        -Dspring.cloud.config.label=<config-label> \
        -Dloader.path=<path-to-kernel-ref-idobjectvalidator.jar> \
        -jar id-repository-service/target/id-repository-service-<version>.jar
   ```

   **Example:**
    - _profile_: `env` (extension used on configuration property files)
    - _config-label_: `master` (git branch of config repo)
    - _config-url_: `http://localhost:51000` (URL of the config server)

   The service loads the `application`, `id-repository`, `credential-service`, and `credential-request` property documents from the config server.

5. **Verify Swagger is accessible:** http://localhost:8090/idrepository/v1/identity/swagger-ui/index.html (other links under [APIs](#apis)).

### Local Setup with Docker (Easy Setup for Demos)

#### Option 1: Pull from Docker Hub

Recommended for users who want a quick, ready-to-use setup — testers, students, and external users.

Pull the latest pre-built images from Docker Hub using the following commands:

```text
docker pull mosipid/id-repository-service:<version>
docker pull mosipid/id-repository-salt-generator:<version>
```

Releases before the consolidation published four separate images (`id-repository-identity-service`, `id-repository-vid-service`, `credential-service`, `credential-request-generator`). Do not mix those with `id-repository-service` in one environment.

#### Option 2: Build Docker Images Locally

Recommended for contributors or developers who want to modify or build the services from source.

1. **Clone and build the project:**

   ```text
   git clone https://github.com/mosip/id-repository.git
   cd id-repository/id-repository
   mvn clean install -Dmaven.javadoc.skip=true -Dgpg.skip=true
   ```

2. **Build the two Docker images:**

   ```text
   docker build -t id-repository-service id-repository-service
   docker build -t id-repository-salt-generator id-repository-salt-generator
   ```

#### Running the Services

Run the salt generator once and let it finish before starting the service:

```text
docker run --rm --name id-repository-salt-generator id-repository-salt-generator
docker run -d -p 8090:8090 --name id-repository-service id-repository-service
```

Both containers need the config server URL, profile, and label (see [Local Setup](#local-setup-for-development-or-contribution)).

#### Verify Installation

```text
docker ps
```

Access the service at `http://localhost:8090`. Health: `http://localhost:8090/idrepository/v1/identity/actuator/health`.

## Deployment

### Kubernetes

To deploy ID Repository services on a Kubernetes cluster, refer to the [Sandbox Deployment Guide](https://docs.mosip.io/1.2.0/deploymentnew/v3-installation).

### Pre-requisites

* Set KUBECONFIG variable to point to existing K8 cluster kubeconfig file:
    ```text
    export KUBECONFIG=~/.kube/<k8s-cluster.config>
    ```

### Install

Installs the `idrepo-saltgen` Job (waits for it to complete), then the `identity` chart, which runs `id-repository-service`:

```text
cd deploy/idrepo
./install.sh
```

Before installing on an environment that ran the separate services, update the service URLs in `mosip-config` as described in the [consolidation design](docs/design/service-consolidation.md#6-service-urls-in-mosip-config). Otherwise calls to `vid.idrepo`, `credential.idrepo`, or `credentialrequest.idrepo` fail DNS resolution.

### Delete

```text
cd deploy/idrepo
./delete.sh
```

### Restart

```text
cd deploy/idrepo
./restart.sh
```

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