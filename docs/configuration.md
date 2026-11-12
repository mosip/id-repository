# ID Repository Configuration Guide

## Overview
The guide here lists down some of the important properties that may be customised for a given installation. Note that the listing here is not exhaustive, but a checklist to review properties that are likely to be different from default. If you would like to see all the properites, then refer to the files listed below.

## Configuration files
ID Repository uses the following configuration files

```
application-default.properties
id-repository-default.properties
credential-request-default.properties
credential-service-default.properties
identity-mapping.json
mosip-vid-policy-schema.json
mosip-vid-policy.json
mosip-cbeff.xsd
credentialdata.mvel
CredentialType.json
mosip-context.json
cred-v1.jsonld
odrl.jsonld
vccontext.jsonld
```

See [Module Configuration](https://docs.mosip.io/1.2.0/modules/module-configuration) for location of these files.

The single `id-repository-service` process loads all four property documents (`spring.cloud.config.name=application,id-repository,credential-service,credential-request`). A key defined in more than one of them has one value for every API in the process; the service-specific documents override `application-default.properties`.

## Service URLs
Identity, VID, credential store, and credential request are served by the `identity` Service in the `idrepo` namespace. Set these base URLs in `application-default.properties` and in any active profile file:

```
mosip.idrepo.identity.url=http://identity.idrepo
mosip.idrepo.vid.url=http://identity.idrepo
mosip.idrepo.credrequest.generator.url=http://identity.idrepo
mosip.idrepo.credential.service.url=http://identity.idrepo
```

The service calls its own VID and credential request APIs through these URLs (for example draft VID on publish draft). The old `vid.idrepo`, `credential.idrepo`, and `credentialrequest.idrepo` hosts no longer exist. See the [consolidation design](design/service-consolidation.md#6-service-urls-in-mosip-config).

## DB
* `mosip.idrepo.db.url`
* `mosip.idrepo.db.port`
* `mosip.credential.service.database.hostname`
* `mosip.credential.service.database.port`

Point the above to your DB and port.  Default is set to point to in-cluster Postgres installed with sandbox.

## VID Configurations
* `mosip-vid-policy.json`: VID policies based on which VID is created.

## Salt Generator Configurations
** application.properties **

```
mosip.kernel.salt-generator.chunk-size=<chunkSize>
mosip.kernel.salt-generator.start-sequence=<startSeq>
mosip.kernel.salt-generator.end-sequence=<endSeq>
```

## Biometric-SDK configurations
```
mosip.biosdk.default.service.url
mosip.biometric.sdk.providers.finger.mosip-ref-impl-sdk-client.classname
mosip.biometric.sdk.providers.iris.mosip-ref-impl-sdk-client.classname
mosip.biometric.sdk.providers.face.mosip-ref-impl-sdk-client.classname
```

## ID Object Reference Validator Configurations
* `mosip.kernel.idobjectvalidator.referenceValidator` : Class name of the referenceValidator. Commenting or removing this property will disable reference validator.

## Misc properties
* `mosip.idrepo.datetime.future-time-adjustment` : Configuration for +/- time period adjustment in minutes for the request time validation, so that the requests originating from a system that is not in time-sync will be accepted for the time period.
* `mosip.idrepo.active-async-thread-count` : number of async threads created in IDRepo services. This count is divided into 4 thread groups configured in IdRepoConfig.class