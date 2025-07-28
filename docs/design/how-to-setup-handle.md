## How to set up handles in MOSIP ID system?

This document guides to set up handle support from the point of registration to authentication.

1. Choose a field which is unique per resident to use as a handle. For example, emailId, functional Ids like taxId, healthId.
2. Mark the chosen field as a handle in the MOSIP ID schema.
    * New attribute `handle` is introduced and supported in the ID schema.
    * `handle` attribute accepts boolean value.
    * `handle` is an optional field, if absent is it considered to be false.
3. Send the handle field name in the `selectedHandles` field to addIdentity and updateIdentity endpoint.
4. To avoid conflict between different handle fields with the same value, it is suggested to configure unique postfix for each handle field.
   * if no postfix is configured, by default fieldId is used as postfix with @ as a separator.
   * Below property should be configured with the postfix for each handle field.
     `mosip.identity.fieldid.handle-postfix.mapping` in `id-repository-default.properties`
5. Configure the handle field value regex to differentiate between UIN, VID, or handle as individualId.
   * UIN and VID are differentiated based on the length. But the same will not hold valid for handles to validate during authentication.
   * Below property should be configured with the regex for each handle field. If not configured, IDA throws error during individual ID validation.
     `mosip.ida.handle-types.regex` in `id-authentication-default.properties`


## Let's consider a use case:

1. "Utopia" country decides to use taxId as a handle.
2. Update ID schema to mark taxId field as a handle and publish the ID schema.
   ```
      {
	    "$schema": "http://json-schema.org/draft-07/schema#",
	    "description": "MOSIP Sample identity",
	    "additionalProperties": false,
	    "title": "MOSIP identity",
	    "type": "object",
	    "definitions": {
		 "simpleType": {
			"uniqueItems": true,
			"additionalItems": false,
			"type": "array",
			"items": {
				"additionalProperties": false,
				"type": "object",
				"required": [
					"language",
					"value"
				],
				"properties": {
					"language": {
						"type": "string"
					},
					"value": {
						"type": "string"
					}
				}
			}
		 },
		 "biometricsType": {
			"additionalProperties": false,
			"type": "object",
			"properties": {
				"format": {
					"type": "string"
				},
				"version": {
					"type": "number",
					"minimum": 0
				},
				"value": {
					"type": "string"
				}
			}
		 }
	  },
	  "properties": {
		 "identity": {
			"additionalProperties": false,
			"type": "object",
			"required": [
				"IDSchemaVersion",
				"fullName",
				"gender",
				"taxId",
				"individualBiometrics"
			],
			"properties": {
                "taxId": {
					"bioAttributes": [],
					"fieldCategory": "pvt",
					"format": "",
					"fieldType": "default",
					"type": "string",
                    "handle": true
				},
				"gender": {
					"bioAttributes": [],
					"fieldCategory": "pvt",
					"format": "",
					"fieldType": "default",
					"$ref": "#/definitions/simpleType"
				},
				"individualBiometrics": {
					"bioAttributes": [
						"face"
					],
					"fieldCategory": "pvt",
					"format": "none",
					"fieldType": "default",
					"$ref": "#/definitions/biometricsType"
				},
				"fullName": {
					"bioAttributes": [],
					"validators": [{
						"validator": "^(?=.{3,50}$).*",
						"arguments": [],
						"type": "regex"
					}],
					"fieldCategory": "pvt",
					"format": "none",
					"fieldType": "default",
					"$ref": "#/definitions/simpleType"
				},
				"IDSchemaVersion": {
					"bioAttributes": [],
					"fieldCategory": "none",
					"format": "none",
					"type": "number",
					"fieldType": "default",
					"minimum": 0
				},
				"UIN": {
					"bioAttributes": [],
					"fieldCategory": "none",
					"format": "none",
					"type": "string",
					"fieldType": "default"
				},
				"selectedHandles" : {
                    "fieldCategory": "none",
                    "format": "none",
                    "type": "array",
                    "items" : { "type" : "string" },
                    "fieldType": "default"
                }
			}
        }
      }
   }
   ```
   
3. Update the logic to support `selectedHandles` in the registration clients. 
   * Refer ARC or desktop reg-client documents to update the required configurations to support selectedHandles.
    
4. Configure `mosip.identity.fieldid.handle-postfix.mapping` in `id-repository-default.properties` as below.
   `mosip.identity.fieldid.handle-postfix.mapping={"taxId" : "@tax"}`

    * if this isn't configured, ID-repo addIdentity & updateIdentity endpoints append lowercased fieldId as postfix. In this usecase "@taxid" is used.
    * So if the taxId is AE782341OII, AE782341OII@tax is used to check for duplicates in the handle table, if none found, then the handle value is saved.
    * Once save is successful in UIN and handle table, an entry is saved in credential_request_status table.
    * Credential issuance job, finds the list of VIDs, handles(with postfix) for the stored requestID in credential_request_status table. Each of them is issued as separate ID credential to IDA.

5. Configure `mosip.ida.handle-types.regex` in `id-authentication-default.properties` as below.
   `mosip.ida.handle-types.regex={ '@tax' : '^[0-9A-Z]{,11}@tax$' }`

    * if this is not configured, IDA will try to validate provided individualId as UIN or VID based on the length and checksum.
    * In the above configuration, key is the postfix, and the value is actually regex used to validate the input individual ID in the authentication request.
    * if the input individualID not match any of the configured regex, nor the UIN/VID length, "IDA-MLC-009" error is returned.
