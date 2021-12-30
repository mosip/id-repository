

\c mosip_idrepo sysadmin

----------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.uin_h ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE idrepo.uin_biometric_h ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE idrepo.uin_document_h ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE idrepo.uin_auth_lock ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE idrepo.uin_document ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE idrepo.uin_biometric ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE idrepo.uin ALTER COLUMN is_deleted SET NOT NULL;


ALTER TABLE idrepo.uin_h ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE idrepo.uin_biometric_h ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE idrepo.uin_document_h ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE idrepo.uin_auth_lock ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE idrepo.uin_document ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE idrepo.uin_biometric ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE idrepo.uin ALTER COLUMN is_deleted SET DEFAULT FALSE;

-----------------------------------------------------------------------------------------------------
