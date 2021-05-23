\c mosip_credential 

GRANT CONNECT
   ON DATABASE mosip_credential
   TO credentialuser;

GRANT USAGE
   ON SCHEMA credential
   TO credentialuser;

GRANT SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES
   ON ALL TABLES IN SCHEMA credential
   TO credentialuser;

ALTER DEFAULT PRIVILEGES IN SCHEMA credential 
	GRANT SELECT,INSERT,UPDATE,DELETE,REFERENCES ON TABLES TO credentialuser;

