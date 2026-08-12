-- ID-Repository local-dev-setup init (IDA-style single init.sql)
-- Creates idrepo / idmap / credential / keymgr databases, applies DDL, seeds defaults.

CREATE ROLE idrepouser WITH LOGIN PASSWORD 'mosip123';
CREATE ROLE idmapuser WITH LOGIN PASSWORD 'mosip123';
CREATE ROLE credentialuser WITH LOGIN PASSWORD 'mosip123';
CREATE ROLE keymgruser WITH LOGIN PASSWORD 'mosip123';

CREATE DATABASE mosip_idrepo
	ENCODING = 'UTF8'
	TABLESPACE = pg_default
	OWNER = postgres
	TEMPLATE  = template0;
COMMENT ON DATABASE mosip_idrepo IS 'ID Repo database';

CREATE DATABASE mosip_idmap
	ENCODING = 'UTF8'
	TABLESPACE = pg_default
	OWNER = postgres
	TEMPLATE  = template0;
COMMENT ON DATABASE mosip_idmap IS 'ID Map / VID database';

CREATE DATABASE mosip_credential
	ENCODING = 'UTF8'
	TABLESPACE = pg_default
	OWNER = postgres
	TEMPLATE  = template0;
COMMENT ON DATABASE mosip_credential IS 'Credential store database';

CREATE DATABASE mosip_keymgr
	ENCODING = 'UTF8'
	TABLESPACE = pg_default
	OWNER = postgres
	TEMPLATE  = template0;
COMMENT ON DATABASE mosip_keymgr IS 'Key Manager database for encryption keys and certificates';

-- ===================== mosip_idrepo =====================
\c mosip_idrepo

CREATE SCHEMA idrepo;
ALTER SCHEMA idrepo OWNER TO postgres;
ALTER DATABASE mosip_idrepo SET search_path TO idrepo,pg_catalog,public;
SET search_path TO idrepo,pg_catalog,public;
GRANT CONNECT ON DATABASE mosip_idrepo TO idrepouser;
GRANT USAGE ON SCHEMA idrepo TO idrepouser;
ALTER DEFAULT PRIVILEGES IN SCHEMA idrepo GRANT SELECT,INSERT,UPDATE,DELETE,REFERENCES ON TABLES TO idrepouser;

\i /db_scripts/mosip_idrepo/ddl/idrepo-anonymous_profile.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-channel_info.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-credential_request_status.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-handle.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-identity_update_count_tracker.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-uin.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-uin_auth_lock.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-uin_biometric.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-uin_biometric_draft.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-uin_biometric_h.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-uin_document.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-uin_document_draft.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-uin_document_h.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-uin_draft.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-uin_encrypt_salt.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-uin_h.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-uin_hash_salt.sql
\i /db_scripts/mosip_idrepo/ddl/idrepo-fk.sql

GRANT SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES ON ALL TABLES IN SCHEMA idrepo TO idrepouser;

-- Default salts (0-999) so local identity crypto works without salt-generator
CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO idrepo.uin_hash_salt (id, salt, cr_by, cr_dtimes)
SELECT g, left(encode(digest('idrepo-hash-' || g::text, 'sha256'), 'base64'), 36), 'LOCAL', now()
FROM generate_series(0, 999) AS g;

INSERT INTO idrepo.uin_encrypt_salt (id, salt, cr_by, cr_dtimes)
SELECT g, left(encode(digest('idrepo-enc-' || g::text, 'sha256'), 'base64'), 36), 'LOCAL', now()
FROM generate_series(0, 999) AS g;

-- ===================== mosip_idmap =====================
\c mosip_idmap

CREATE SCHEMA idmap;
ALTER SCHEMA idmap OWNER TO postgres;
ALTER DATABASE mosip_idmap SET search_path TO idmap,pg_catalog,public;
SET search_path TO idmap,pg_catalog,public;
GRANT CONNECT ON DATABASE mosip_idmap TO idmapuser;
GRANT USAGE ON SCHEMA idmap TO idmapuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA idmap GRANT SELECT,INSERT,UPDATE,DELETE,REFERENCES ON TABLES TO idmapuser;

\i /db_scripts/mosip_idmap/ddl/idmap-uin_encrypt_salt.sql
\i /db_scripts/mosip_idmap/ddl/idmap-uin_hash_salt.sql
\i /db_scripts/mosip_idmap/ddl/idmap-vid.sql
\i /db_scripts/mosip_idmap/ddl/idmap-vid_seed.sql
\i /db_scripts/mosip_idmap/ddl/idmap-vid_seq.sql

GRANT SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES ON ALL TABLES IN SCHEMA idmap TO idmapuser;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO idmap.uin_hash_salt (id, salt, cr_by, cr_dtimes)
SELECT g, left(encode(digest('idmap-hash-' || g::text, 'sha256'), 'base64'), 36), 'LOCAL', now()
FROM generate_series(0, 999) AS g;

INSERT INTO idmap.uin_encrypt_salt (id, salt, cr_by, cr_dtimes)
SELECT g, left(encode(digest('idmap-enc-' || g::text, 'sha256'), 'base64'), 36), 'LOCAL', now()
FROM generate_series(0, 999) AS g;

-- ===================== mosip_credential =====================
\c mosip_credential

CREATE SCHEMA credential;
ALTER SCHEMA credential OWNER TO postgres;
ALTER DATABASE mosip_credential SET search_path TO credential,pg_catalog,public;
SET search_path TO credential,pg_catalog,public;
GRANT CONNECT ON DATABASE mosip_credential TO credentialuser;
GRANT USAGE ON SCHEMA credential TO credentialuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA credential GRANT SELECT,INSERT,UPDATE,DELETE,REFERENCES ON TABLES TO credentialuser;

\i /db_scripts/mosip_credential/ddl/credential-batch_job_execution.sql
\i /db_scripts/mosip_credential/ddl/credential-batch_job_execution_context.sql
\i /db_scripts/mosip_credential/ddl/credential-batch_job_execution_params.sql
\i /db_scripts/mosip_credential/ddl/credential-batch_job_instance.sql
\i /db_scripts/mosip_credential/ddl/credential-batch_step_execution.sql
\i /db_scripts/mosip_credential/ddl/credential-batch_step_execution_context.sql
\i /db_scripts/mosip_credential/ddl/credential-credential_transaction.sql
\set dbuname credentialuser
\i /db_scripts/mosip_credential/ddl/credential-fk.sql

GRANT SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES ON ALL TABLES IN SCHEMA credential TO credentialuser;

-- ===================== mosip_keymgr =====================
\c mosip_keymgr

CREATE SCHEMA keymgr;
ALTER SCHEMA keymgr OWNER TO postgres;
ALTER DATABASE mosip_keymgr SET search_path TO keymgr,pg_catalog,public;
SET search_path TO keymgr,pg_catalog,public;
GRANT CONNECT ON DATABASE mosip_keymgr TO keymgruser;
GRANT USAGE ON SCHEMA keymgr TO keymgruser;
ALTER DEFAULT PRIVILEGES IN SCHEMA keymgr GRANT SELECT,INSERT,UPDATE,DELETE,REFERENCES ON TABLES TO keymgruser;

\i /keymgr/ddl/keymgr-key_alias.sql
\i /keymgr/ddl/keymgr-key_policy_def.sql
\i /keymgr/ddl/keymgr-key_store.sql
\i /keymgr/ddl/keymgr-data_encrypt_keystore.sql
\i /keymgr/ddl/keymgr-ca_cert_store.sql
\i /keymgr/ddl/keymgr-partner_cert_store.sql
\i /keymgr/ddl/keymgr-licensekey_list.sql
\i /keymgr/ddl/keymgr-licensekey_permission.sql
\i /keymgr/ddl/keymgr-tsp_licensekey_map.sql
\i /keymgr/ddl/keymgr-fk.sql

GRANT SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES ON ALL TABLES IN SCHEMA keymgr TO keymgruser;

-- Default key policies (mosip/keymanager dml + local extras), same idea as IDA init.sql seed
TRUNCATE TABLE keymgr.key_policy_def CASCADE;
INSERT INTO keymgr.key_policy_def (app_id, key_validity_duration, is_active, cr_by, cr_dtimes, pre_expire_days, access_allowed) VALUES
	('PRE_REGISTRATION', 1095, true, 'mosipadmin', now(), 60, 'NA'),
	('REGISTRATION', 1095, true, 'mosipadmin', now(), 60, 'NA'),
	('REGISTRATION_PROCESSOR', 1095, true, 'mosipadmin', now(), 60, 'NA'),
	('ID_REPO', 1095, true, 'mosipadmin', now(), 60, 'NA'),
	('KERNEL', 1095, true, 'mosipadmin', now(), 60, 'NA'),
	('ROOT', 2920, true, 'mosipadmin', now(), 1125, 'NA'),
	('BASE', 730, true, 'mosipadmin', now(), 30, 'NA'),
	('PMS', 1460, true, 'mosipadmin', now(), 395, 'NA'),
	('RESIDENT', 1095, true, 'mosipadmin', now(), 60, 'NA'),
	('ADMIN_SERVICES', 1095, true, 'mosipadmin', now(), 60, 'NA'),
	('DIGITAL_CARD', 1095, true, 'mosipadmin', now(), 60, 'NA'),
	('COMPLIANCE_TOOLKIT', 1095, true, 'mosipadmin', now(), 60, 'NA'),
	('CREDENTIAL_SERVICE', 1095, true, 'mosipadmin', now(), 60, 'NA'),
	('IDA', 7305, true, 'mosipadmin', now(), 60, 'NA');
