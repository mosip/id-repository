\c mosip_idrepo 

GRANT CONNECT
   ON DATABASE mosip_idrepo
   TO idrepouser;

GRANT USAGE
   ON SCHEMA idrepo
   TO idrepouser;

GRANT SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES
   ON ALL TABLES IN SCHEMA idrepo
   TO idrepouser;

ALTER DEFAULT PRIVILEGES IN SCHEMA idrepo 
	GRANT SELECT,INSERT,UPDATE,DELETE,REFERENCES ON TABLES TO idrepouser;

