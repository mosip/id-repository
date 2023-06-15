\c mosip_idrepo

REASSIGN OWNED BY postgres TO sysadmin;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA idrepo TO sysadmin;

ALTER TABLE idrepo.uin_auth_lock DROP COLUMN unlock_expiry_datetime;
DROP TABLE IF EXISTS idrepo.credential_request_status;
-----------------------------------------------------------------------------------------------------


DROP TABLE IF EXISTS idrepo.uin_biometric_draft;
DROP TABLE IF EXISTS idrepo.uin_draft;
DROP TABLE IF EXISTS idrepo.uin_document_draft;

------------------------------------------------------------------------------------------------------

DROP TABLE IF EXISTS idrepo.anonymous_profile;
DROP TABLE IF EXISTS idrepo.channel_info;
DROP TABLE IF EXISTS idrepo.identity_update_count_tracker;
------------------------------------------------------------------------------------------------------
