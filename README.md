# ID Repository

[![Maven Package upon a push](https://github.com/mosip/id-repository/actions/workflows/push-trigger.yml/badge.svg?branch=release-1.3.x)](https://github.com/mosip/id-repository/actions/workflows/push-trigger.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=release-1.3.x&project=mosip_id-repository&metric=alert_status)](https://sonarcloud.io/dashboard?branch=release-1.3.x&id=mosip_id-repository)

## Overview

The **ID Repository** is the backbone of the MOSIP identity platform, responsible for the secure storage and lifecycle management of foundational identity data. It serves as the authoritative source for identity records, handling operations such as identity creation, updates, and retrieval.

This repository includes a collection of microservices that manage:
- **Identity Data:** Storage and retrieval of demographic and biometric data.
- **Credentials:** Management and issuance of verifiable credentials.
- **Virtual IDs (VID):** Generation and management of revocable virtual tokens for privacy protection.
- **Salt & Keys:** Security foundations for identity data encryption and hashing.

The module provides a comprehensive set of REST APIs to interact with these services, ensuring secure and standardized access to identity information.

## Services

The ID Repository contains the following services. For detailed code setup instructions for each service, please refer to their individual README files:

1. **[Credential Service](id-repository/credential-service/README.md)** - Service for handling credentials.
2. **[Identity Service](id-repository/id-repository-identity-service/README.md)** - Service for identity management.
3. **[VID Service](id-repository/id-repository-vid-service/README.md)** - Service for Virtual ID management.
4. **[Key Generator](id-repository/id-repository-salt-generator/README.md)** - Utility for generating salt/keys.
5. **[Credential Request Generator](id-repository/credential-request-generator/README.md)** - Generator for credential requests.
6. **[Core](id-repository/id-repository-core/README.md)** - Core library and shared components.

## Contribution & Community

• To learn how you can contribute code to this application, [click here](https://docs.mosip.io/1.2.0/community/code-contributions).

• If you have questions or encounter issues, visit the [MOSIP Community](https://community.mosip.io/) for support.

• For any GitHub issues: [Report here](https://github.com/mosip/id-repository/issues)

## License

This project is licensed under the [Mozilla Public License 2.0](LICENSE).