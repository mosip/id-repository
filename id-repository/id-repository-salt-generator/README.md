# ID Repository Salt Generator

Salt Generator Job is a one-time job which is run to populate salts to be used to hash and encrypt data for ID Repository Identity and VID Services.
This job needs to be executed before deployment of ID Repository Identity and VID services. uin_hash_salt and uin_encrypt_salt tables in mosip_idrepo and mosip_idmap tables will be populated with salt values when Salt Generator job is executed. ID Repository applications won't function without salts populated in the above mentioned tables.

**Java command:**

```
java -Dspring.cloud.config.uri=<url> -Dspring.cloud.config.label=<label> -Dspring.cloud.config.name=<name> -Dspring.profiles.active=<profile> -jar id-repository-salt-generator.jar
```

## Default context-path and port
Refer [`bootstrap.properties`](src/main/resources/bootstrap.properties)
