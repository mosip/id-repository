## What are Verified Attributes?

Verified attributes refer to the user attributes which are verified by a trusted entity.

In MOSIP ID-Repo, both add-identity and update-identity endpoints takes the list of verified attributes as input. The same feature is enhanced by provisioning the input of verification metadata for each verified user attribute. We have introduced v2 version of add-identity and update-identity to take a Map as input instead of just list of user attributes.

Input verification metadata is validated against a schema. verification metadata schema is maintained in the config server.  Each user attribute can be verified by multiple trust frameworks. To avoid duplicate verification metadata for a user attribute, specific fields in the metadata are choosen to check uniqueness. Fields used for uniquness check is configurable, default fields are "trust_framework" and "verification_process".
 

## Change of datatype in the v2 endpoints
Currently, verified attributes holds list of strings. Each element in the list is the field Id defined in the identity schema.

In v2 version of the create and update identity endpoint, verified attributes datatype is changed to Map. Key in the map is the field Id as 
defined in the identity schema Value in the map holds list of map. Each element in the value list is the verification metadata from a specific 
"trust_framework" and "verification_process". Also, the element in the value list should be compliant with the identity assurance verification 
metadata schema.

## Principles

1. Verification metadata must adhere to configured verification metadata schema - https://github.com/mosip/esignet/blob/release-1.5.1-temp/esignet-service/src/main/resources/verified_claims_request_schema.json  (based on Identity Assurance 1.0 Spec)
2. As we allow multiple verification metadata to be captured per user attribute, we should maintain unique verification metadata. Fields used for uniqueness check is configurable. By default, trust_framework & verification_process are considered for uniqueness check.
3. `verifiedAttributes` is stored as one of the field in the identity object json.
4. Credential issued to IDA should have `verifiedAttributes` only if it's allowed in the datashare policy.
5. `verifiedAttributes` is an optional field.
6. On create of identity object, validate verification metadata with schema defined in identity assurance spec. Only unique entries should be saved.
7. On update, replace value based on the field ID and the trust_framework & verification_process.

	a. Update request to update user attribute and also contains verification metadata for the same user attribute. OR Update request only contains verification metadata for one or more user attributes.
		
		* If the saved identity object already contains same verification metadata, then replace the element with new metadata.
		* If the saved identity object doesnot contain same verification metadata, then append new metadata as a new element.		
	
	b. Update request to update user attribute but does not contain any verification metadata for the same user attribute.

		* If the saved identity object already contains same verification metadata, then remove all the verification metadata mapped to input user attribute.
