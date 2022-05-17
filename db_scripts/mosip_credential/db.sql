CREATE DATABASE mosip_credential 
	ENCODING = 'UTF8' 
	LC_COLLATE = 'en_US.UTF-8' 
	LC_CTYPE = 'en_US.UTF-8' 
	TABLESPACE = pg_default 
	OWNER = postgres
	TEMPLATE  = template0;

COMMENT ON DATABASE mosip_credential IS 'credential related entities and its data is stored in this database';

\c mosip_credential

DROP SCHEMA IF EXISTS credential CASCADE;
CREATE SCHEMA credential;
ALTER SCHEMA credential OWNER TO postgres;
ALTER DATABASE mosip_credential SET search_path TO credential,pg_catalog,public;
