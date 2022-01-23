CREATE DATABASE mosip_idrepo
	ENCODING = 'UTF8'
	LC_COLLATE = 'en_US.UTF-8'
	LC_CTYPE = 'en_US.UTF-8'
	TABLESPACE = pg_default
	OWNER = postgres
	TEMPLATE  = template0;
COMMENT ON DATABASE mosip_idrepo IS 'ID Repo database stores all the data related to an individual for which an UIN is generated';

\c mosip_idrepo 

DROP SCHEMA IF EXISTS idrepo CASCADE;
CREATE SCHEMA idrepo;
ALTER SCHEMA idrepo OWNER TO postgres;
ALTER DATABASE mosip_idrepo SET search_path TO idrepo,pg_catalog,public;
