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

The ID Repository contains the following services. For detailed code setup instructions for each service, please refer to their individual README files:

1. **[Credential Service](id-repository/credential-service/README.md)** - Service for handling credentials.
2. **[Identity Service](id-repository/id-repository-identity-service/README.md)** - Service for identity management.
3. **[VID Service](id-repository/id-repository-vid-service/README.md)** - Service for Virtual ID management.
4. **[Key Generator](id-repository/id-repository-salt-generator/README.md)** - Utility for generating salt/keys.
5. **[Credential Request Generator](id-repository/credential-request-generator/README.md)** - Generator for credential requests.
6. **[Core](id-repository/id-repository-core/README.md)** - Core library and shared components.

## Codebase Relocation

The credential feeder has been moved to the [mosip-utilities](https://github.com/mosip/mosip-utilities) repository.

## Database

See [DB guide](db_scripts/README.md) for database setup and migration details.

## Installation

### Prerequisites

The project requires:
- **JDK:** 21.0.3
- **Maven:** 3.9.6
- **kernel-auth-adapter.jar** needs to be added to the build path to run the services
- **Biometric SDK:** To run the Identity Service, a Biometric SDK implementation jar or [Mock SDK](https://github.com/mosip/mosip-mock-services/tree/master/mock-sdk) needs to be added to the build path

#### For Kubernetes Deployment

* Set KUBECONFIG variable to point to existing K8 cluster kubeconfig file:
    ```text
    export KUBECONFIG=~/.kube/<k8s-cluster.config>
    ```

### Local Setup (for Development or Contribution)

1. **Make sure the config server is running.** For detailed instructions on setting up and running the config server, refer to the [MOSIP Config Server Setup Guide](https://docs.mosip.io/1.2.0/modules/module-configuration).

   **Note:** Refer to the MOSIP Config Server Setup Guide for setup, and ensure the properties mentioned in the configuration section are taken care of. Replace the properties with your own configurations (e.g., DB credentials, IAM credentials, URL).

2. **Clone the repository:**

   ```text
   git clone https://github.com/mosip/id-repository.git
   cd id-repository
   ```

3. **Build the project:**

   ```text
   mvn clean install -Dmaven.javadoc.skip=true -Dgpg.skip=true
   ```

4. **Start the application:**
    - Click the Run button in your IDE, or
    - Run via command:

   ```text
   java -Dspring.profiles.active=<profile> \
        -Dspring.cloud.config.uri=<config-url> \
        -Dspring.cloud.config.label=<config-label> \
        -jar <jar-name>.jar
   ```

   **Example:**
    - _profile_: `env` (extension used on configuration property files)
    - _config-label_: `master` (git branch of config repo)
    - _config-url_: `http://localhost:51000` (URL of the config server)

5. **Verify Swagger is accessible at:** `http://localhost:<port>/v1/<service>/swagger-ui/index.html`

### Local Setup with Docker (Easy Setup for Demos)

#### Option 1: Pull from Docker Hub

Recommended for users who want a quick, ready-to-use setup — testers, students, and external users.

Pull the latest pre-built images from Docker Hub using the following commands:

```text
docker pull mosipid/id-repository-identity-service:<version>
docker pull mosipid/id-repository-vid-service:<version>
docker pull mosipid/credential-service:<version>
docker pull mosipid/credential-request-generator:<version>
docker pull mosipid/id-repository-salt-generator:<version>
```

#### Option 2: Build Docker Images Locally

Recommended for contributors or developers who want to modify or build the services from source.

1. **Clone and build the project:**

   ```text
   git clone https://github.com/mosip/id-repository.git
   cd id-repository
   mvn clean install -Dmaven.javadoc.skip=true -Dgpg.skip=true
   ```

2. **Navigate to each service directory and build the Docker image:**

   ```text
   cd id-repository/<service-directory>
   docker build -t <service-name> .
   ```

#### Running the Services

Start each service using Docker:

```text
docker run -d -p <port>:<port> --name <service-name> <service-name>
```

#### Verify Installation

Check that all containers are running:

```text
docker ps
```

Access the services at `http://localhost:<port>` using the port mappings for each service.

## Deployment

### Kubernetes

To deploy ID Repository services on a Kubernetes cluster, refer to the [Sandbox Deployment Guide](https://docs.mosip.io/1.2.0/deploymentnew/v3-installation).

### Pre-requisites

* Set KUBECONFIG variable to point to existing K8 cluster kubeconfig file:
    ```text
    export KUBECONFIG=~/.kube/<k8s-cluster.config>
    ```

### Install

```text
cd deploy
./install.sh
```

### Delete

```text
cd deploy
./delete.sh
```

### Restart

```text
cd deploy
./restart.sh
```

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