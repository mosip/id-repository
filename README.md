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

- **[ID Repository Service](id-repository/id-repository-service/README.md):**
  Provides identity, VID, credential, and credential-request APIs.
- **[Core](id-repository/id-repository-core/README.md):**
  Shared library used by ID Authentication and other MOSIP modules.
- **[Salt Generator](id-repository/id-repository-salt-generator/README.md):**
  One-shot job that populates salts in the ID Repository and ID mapping databases.

## Codebase Relocation

The credential feeder has been moved to the [mosip-utilities](https://github.com/mosip/mosip-utilities) repository.

## Database

See [DB guide](db_scripts/README.md) for database setup and migration details.

## Installation

### Prerequisites

The project requires:
- **JDK:** 21.0.3
- **Maven:** 3.9.6

#### For Kubernetes Deployment

* Set KUBECONFIG variable to point to existing K8 cluster kubeconfig file:
    ```text
    export KUBECONFIG=~/.kube/<k8s-cluster.config>
    ```

### Local Setup for Development

For prerequisites, setup, startup, and verification instructions, see the
[local development guide](id-repository/local-dev-setup/README.md).

## Deployment

For Kubernetes deployment instructions, see the
[deployment guide](deploy/idrepo/README.md).

## Configuration

Refer to the [configuration guide](docs/configuration.md).

## APIs

API documentation is available [here](https://mosip.github.io/documentation/).

## Testing

Automated functional tests are available in the [Functional Tests repo](https://github.com/mosip/mosip-functional-tests).

## Contribution & Community

• To learn how you can contribute code to this application, [click here](https://docs.mosip.io/1.2.0/community/code-contributions).

• If you have questions or encounter issues, visit the [MOSIP Community](https://community.mosip.io/) for support.

• For any GitHub issues: [Report here](https://github.com/mosip/id-repository/issues)

## License

This project is licensed under the [Mozilla Public License 2.0](LICENSE).
