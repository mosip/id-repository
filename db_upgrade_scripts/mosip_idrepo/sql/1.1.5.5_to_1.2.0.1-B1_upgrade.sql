ALTER TABLE idrepo.uin_auth_lock ADD COLUMN unlock_expiry_datetime timestamp;
------------------------------------------------------------------------------------------------

\ir ../ddl/idrepo-credential_request_status.sql

\ir ../ddl/idrepo-uin_biometric_draft.sql
\ir ../ddl/idrepo-uin_draft.sql
\ir ../ddl/idrepo-uin_document_draft.sql
\ir ../ddl/idrepo-anonymous_profile.sql
\ir ../ddl/idrepo-channel_info.sql

-------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.uin ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_auth_lock ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_biometric ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_biometric_h ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_document ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_document_h ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_h ALTER COLUMN lang_code DROP NOT NULL;
---------------------------------------------------------------------------------
