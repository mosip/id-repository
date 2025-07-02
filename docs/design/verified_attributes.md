## What are Verified Attributes?

Verified attributes refer to the user attributes which are verified by a trusted entity.

In MOSIP ID-Repo, both add-identity and update-identity endpoints takes the list of verified attributes as input. The same feature is enhanced by provisioning the input of verification metadata for each verified user attribute. We have introduced v2 version of add-identity and update-identity to take verification metadata for verified user attributes.

Input verification metadata is validated against a schema. verification metadata schema is maintained in the config server.  Each user attribute can be verified by multiple trust frameworks. Verification metadata is uniquely identified based on the trusted system name and the process followed to verify user attributes.
 

## Change of datatype in the v2 endpoints
Currently, verified attributes holds list of strings. Each element in the list is the field Id defined in the identity schema.

In v2 version of the create and update identity endpoint, verified attributes datatype is changed to List<VerificationMetadata>. Each element in the list is the verification metadata from a specific 
"trustFramework" and "verificationProcess". Also, the metadata in each element of the list should be compliant with the identity assurance verification 
metadata schema.

## Principles

1. Verification metadata must adhere to configured verification metadata schema - [verified_attributes_schema.json](../../id-repository/id-repository-identity-service/src/main/resources/verified_attributes_schema.json)
2. As we allow multiple verification metadata to be captured per user attribute, we should maintain unique verification metadata per trustFramework and verification process.
3. `verifiedAttributes` is stored as one of the field in the identity object json.
4. Credential issued to IDA should have `verifiedAttributes` only if it's allowed in the datashare policy.
5. `verifiedAttributes` is an optional field.
6. On create of identity object, validate verification metadata with schema configured. Only unique entries should be saved.
7. On update, replace value based on the trustFramework & verificationProcess.

	a. Update request to update user attribute and also contains verification metadata for the same user attribute. OR Update request only contains verification metadata for one or more user attributes.
		
		* If the saved identity object already contains same verification metadata, then replace the element with new metadata.
		* If the saved identity object doesnot contain same verification metadata, then append new metadata as a new element.		
	
	b. Update request to update user attribute but does not contain any verification metadata for the same user attribute.

		* If the saved identity object already contains verification metadata for the input user attribute, then remove all the verification metadata mapped to input user attribute.


## Sample request showcasing the verified attributes in the add/update identity request

   ```
   {
    "id": "mosip.id.create",
    "version": "v1",
    "rquesttime": "2025-07-01T17:01:23.202Z",
    "request": {
        "identity": {
            "IDSchemaVersion": 0.1,
            "fullName": [
                {
                    "language": "eng",
                    "value": "qwerty"
                }
            ]
        },
        "registrationId": "278474680010922202404180640",
        "uin": "3867315603"
        "verifiedAttributes": [
            {
                "trustFramework": "eidas",
                "verificationProcess": "online_video",
                "claims": [
                    "fullName"
                ],
                "metadata": {
                    "trust_framework": "eidas",
                    "time": "2025-06-29T06:03:22.339Z",
                    "assurance_level": "Gold",
                    "verification_process": "online_video"
                }
            }
        ]
    },
    "errors": []
   }
   ```

1. Fields inside the `verifiedAttributes->metadata` is configurable based on the schema defined in this property `mosip.idrepo.verified-attributes.schema-url`
2. `claims` defined inside the `verifiedAttributes` should match the field Ids in the `ID schema`. This validation can be part of the schema defined in `mosip.idrepo.verified-attributes.schema-url`. But default it is not added as its country specific requirement.
3. verifiedAttributes will shared with IDA only its part of the datashare policy under shareable KYC attributes.

