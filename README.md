
[![Maven Package upon a push](https://github.com/mosip/id-repository/actions/workflows/push-trigger.yml/badge.svg?branch=master)](https://github.com/mosip/id-repository/actions/workflows/push-trigger.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=master&project=mosip_id-repository&metric=alert_status)](https://sonarcloud.io/dashboard?branch=master&id=mosip_id-repository)

# ID Repository

## Overview
This repository contains source code and design documents for MOSIP [ID Repository](https://docs.mosip.io/1.2.0/modules/id-repository) which is the server-side module to manage ID lifecycle.  The modules exposes API endpoints.  

## Database
See [DB guide](db_scripts/README.md)

## Build & run (for developers)
The project requires JDK 1.11. 
1. To build jars:
    ```
    $ cd id-repository
    $ mvn clean install 
    ```
1. To skip JUnit tests and Java Docs:
    ```
    $ mvn install -DskipTests=true -Dmaven.javadoc.skip=true
    ```
1. To build Docker for a service:
    ```
    $ cd <service folder>
    $ docker build -f Dockerfile
    ```

1. As a developer, to run a service jar individually:
    ```
    `java -Dspring.profiles.active=<profile> -Dspring.cloud.config.uri=<config-url> -Dspring.cloud.config.label=<config-label> -jar <jar-name>.jar`
    ```
    Example:  
        _profile_: `env` (extension used on configuration property files*)    
        _config_label_: `master` (git branch of config repo*)  
        _config-url_: `http://localhost:51000` (Url of the config server*)  
	
	\* Refer to [kernel-config-server](https://github.com/mosip/commons/tree/master/kernel/kernel-config-server) for details

2. kernel-auth-adapter.jar needs to added to the build path to run the service.
	
NOTE: To run identity service, Biometric SDK implementation jar or [Mock SDK](https://github.com/mosip/mosip-mock-services/tree/master/mock-sdk) needs to be added to the build path.

## Deploy
To deploy ID Repository on Kubernetes cluster using Dockers refer to [mosip-infra](https://github.com/mosip/mosip-infra/tree/1.2.0-rc2/deployment/v3)


## Configuration
Refer to the [configuration guide](docs/configuration.md).

## Test
Automated functaionl tests available in [Functional Tests repo](https://github.com/mosip/mosip-functional-tests)

## APIs
API documentation is available [here](https://mosip.github.io/documentation/).

## License
This project is licensed under the terms of [Mozilla Public License 2.0](https://github.com/mosip/mosip-platform/blob/master/LICENSE)

