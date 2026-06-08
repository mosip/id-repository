CREATE DATABASE :mosipdbname
	ENCODING = 'UTF8'
	LC_COLLATE = 'en_US.UTF-8'
	LC_CTYPE = 'en_US.UTF-8'
	TABLESPACE = pg_default
	OWNER = postgres
	TEMPLATE  = template0;
COMMENT ON DATABASE :mosipdbname IS 'ID Repo database stores all the data related to an individual for which an UIN is generated';

\c :mosipdbname

DROP SCHEMA IF EXISTS idrepo CASCADE;
CREATE SCHEMA idrepo;
ALTER SCHEMA idrepo OWNER TO postgres;
ALTER DATABASE :mosipdbname SET search_path TO idrepo,pg_catalog,public;
