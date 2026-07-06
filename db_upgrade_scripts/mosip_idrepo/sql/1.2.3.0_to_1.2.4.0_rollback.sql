
\c mosip_idrepo sysadmin
---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.handle DROP COLUMN IF EXISTS status;

ALTER TABLE idrepo.credential_request_status ALTER COLUMN individual_id TYPE varchar(500);

------------------------------------------------------------------------------------------------
