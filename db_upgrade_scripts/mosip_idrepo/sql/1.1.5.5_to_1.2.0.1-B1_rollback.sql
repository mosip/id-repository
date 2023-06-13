ALTER DATABASE mosip_idrepo OWNER TO postgres;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA idrepo TO postgres;

REVOKE ALL PRIVILEGES ON DATABASE mosip_idmap FROM idrepouser, sysadmin;

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE ON ALL TABLES IN SCHEMA idrepo TO idrepouser;

ALTER TABLE idrepo.uin_auth_lock DROP COLUMN unlock_expiry_datetime;
DROP TABLE IF EXISTS idrepo.credential_request_status;
-----------------------------------------------------------------------------------------------------


DROP TABLE IF EXISTS idrepo.uin_biometric_draft;
DROP TABLE IF EXISTS idrepo.uin_draft;
DROP TABLE IF EXISTS idrepo.uin_document_draft;

------------------------------------------------------------------------------------------------------

DROP TABLE IF EXISTS idrepo.anonymous_profile;
DROP TABLE IF EXISTS idrepo.channel_info;
------------------------------------------------------------------------------------------------------
