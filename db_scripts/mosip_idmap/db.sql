CREATE DATABASE mosip_idmap 
	ENCODING = 'UTF8' 
	LC_COLLATE = 'en_US.UTF-8' 
	LC_CTYPE = 'en_US.UTF-8' 
	TABLESPACE = pg_default 
	OWNER = postgres
	TEMPLATE  = template0;
COMMENT ON DATABASE mosip_idmap IS 'idmap related entities and its data is stored in this database';

\c mosip_idmap 

DROP SCHEMA IF EXISTS idmap CASCADE;
CREATE SCHEMA idmap;
ALTER SCHEMA idmap OWNER TO postgres;
ALTER DATABASE mosip_idmap SET search_path TO idmap,pg_catalog,public;
