# ID Repository Salt Generator

Salt Generator Job is a one-time job which is run to populate salts to be used to hash and encrypt data. This generic job takes below details as input and generates and populates salts in the given schema and table.

Salt generator requires data source details such as url, username, password, driverClassName to be provided as input in form of property key alias. The value of data source details should be provided in module specific properties as below.

```
<key-alias>.url=<url>
<key-alias>.username=<username>
<key-alias>.password=<password>
<key-alias>.driverClassName=<driverClassName>
```

Salt generator supports schema name and table name to be provided as input in form of direct value or key of the property which contains the value stored in config server.

** application.properties **

```
mosip.kernel.salt-generator.chunk-size=<chunkSize>
mosip.kernel.salt-generator.start-sequence=<startSeq>
mosip.kernel.salt-generator.end-sequence=<endSeq>
```

** module wise required properties **

```
mosip.kernel.salt-generator.db.key-alias=<property key alias providing the details for datasource such as url, username, password, driverClassName>
mosip.kernel.salt-generator.schemaName=<schemaName/property key containing the schemaName>
mosip.kernel.salt-generator.tableName=<tableName/property key containing the tableName>
```

**Java command:**

```
java -Dspring.cloud.config.uri=<url> -Dspring.cloud.config.label=<label> -Dspring.cloud.config.name=<name> -Dspring.profiles.active=<profile> -Dmosip.kernel.salt-generator.schemaName=<schemaName/property key containing the schemaName> -Dmosip.kernel.salt-generator.tableName=<tableName/property key containing the tableName> -jar kernel-salt-generator.jar
```