DROP DATABASE IF EXISTS mosip_credential;
CREATE DATABASE mosip_credential 
	ENCODING = 'UTF8' 
	LC_COLLATE = 'en_US.UTF-8' 
	LC_CTYPE = 'en_US.UTF-8' 
	TABLESPACE = pg_default 
	OWNER = sysadmin
	TEMPLATE  = template0;

-- ddl-end --
COMMENT ON DATABASE mosip_credential IS 'credential related entities and its data is stored in this database';
-- ddl-end --

\c mosip_credential sysadmin

-- object: credential | type: SCHEMA --
DROP SCHEMA IF EXISTS credential CASCADE;
CREATE SCHEMA credential;
-- ddl-end --
ALTER SCHEMA credential OWNER TO sysadmin;
-- ddl-end --

ALTER DATABASE mosip_credential SET search_path TO credential,pg_catalog,public;
-- ddl-end --

-- REVOKE CONNECT ON DATABASE mosip_credential FROM PUBLIC;
-- REVOKE ALL ON SCHEMA credential FROM PUBLIC;
-- REVOKE ALL ON ALL TABLES IN SCHEMA credential FROM PUBLIC ;
