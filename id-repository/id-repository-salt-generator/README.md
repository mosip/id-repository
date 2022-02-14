# Salt Generator

## Overview
Refer [here](https://docs.mosip.io/1.2.0/modules/id-repository#salt-generator).

## Run locally
```
java -Dspring.cloud.config.uri=<url> -Dspring.cloud.config.label=<label> -Dspring.cloud.config.name=<name> -Dspring.profiles.active=<profile> -jar id-repository-salt-generator.jar
```

## Default context-path and port
Refer [`bootstrap.properties`](src/main/resources/bootstrap.properties)
