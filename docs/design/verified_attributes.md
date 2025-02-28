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
