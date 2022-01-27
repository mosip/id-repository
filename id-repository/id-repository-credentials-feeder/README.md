## id-repository-credentials-feeder

Credentials Feeder Job is a one-time job which is run to request credentials for the specified list of partners. The partner list is specified as VM argument as comma separated partner IDs.

id-repository-credentials-feeder will take mosip_idrepo DB configurations from ID Repository properties file.

**application.properties**

```
# Chunk Size to read Credential Requests from DB table. Default value is set to 10.
idrepo-credential-feeder-chunk-size=<chunkSize>

```


**Java command:**

```
java -Dspring.cloud.config.uri=<url> -Dspring.cloud.config.label=<label> -Dspring.cloud.config.name=<name> -Dspring.profiles.active=<profile> -Donline-verification-partner-ids=<olv_partner_ids> -Dskip-requesting-existing-credentials-for-partners=true -jar id-repository-credentials-feeder.jar
```

This generic job takes below VM arguments: 
* `online-verification-partner-ids` is comma separated list of Online_Verification_Partner partner IDs to which credential needs to be requested.
* `skip-requesting-existing-credentials-for-partners` is for optimization. Set this to `false` if credentials to be requested again even if the request entry already exist for partner. Set this to `true` to skip existing credential requests for the partner found in the chunk of query result. By default it is set to `false` if this property is not specified.

**Build and Deployment commands:**

```
docker run -it -d -p 8092:8092 -e active_profile_env={profile}  -e spring_config_label_env= {branch} -e spring_config_url_env={config_server_url} -e spring_config_name_env= {config_server_name} -Donline-verification-partner-ids={olv_partner_ids} -Dskip-requesting-existing-credentials-for-partners=true docker-registry.mosip.io:5000/id-repository-credentials-feeder

```
## Default context-path and port
Refer [`bootstrap.properties`](src/main/resources/bootstrap.properties)