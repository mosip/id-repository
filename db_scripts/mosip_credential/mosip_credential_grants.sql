\c mosip_credential sysadmin

-- object: grant_b0ae4f0dce | type: PERMISSION --
GRANT CREATE,CONNECT,TEMPORARY
   ON DATABASE mosip_credential
   TO sysadmin;
-- ddl-end --

-- object: grant_99dd1cb062 | type: PERMISSION --
GRANT CREATE,CONNECT,TEMPORARY
   ON DATABASE mosip_credential
   TO appadmin;
-- ddl-end --

-- object: grant_18180691b7 | type: PERMISSION --
GRANT CONNECT
   ON DATABASE mosip_credential
   TO credentialuser;
-- ddl-end --

-- object: grant_3543fb6cf7 | type: PERMISSION --
GRANT CREATE,USAGE
   ON SCHEMA credential
   TO sysadmin;
-- ddl-end --

-- object: grant_3543fb6cf7 | type: PERMISSION --
GRANT USAGE
   ON SCHEMA credential
   TO credentialuser;
-- ddl-end --

-- object: grant_8e1a2559ed | type: PERMISSION --
GRANT SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES
   ON ALL TABLES IN SCHEMA credential
   TO credentialuser;
-- ddl-end --

ALTER DEFAULT PRIVILEGES IN SCHEMA credential 
	GRANT SELECT,INSERT,UPDATE,DELETE,REFERENCES ON TABLES TO credentialuser;


-- object: grant_78ed2da4ee | type: PERMISSION --
GRANT SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES
   ON ALL TABLES IN SCHEMA credential
   TO appadmin;
-- ddl-end --
