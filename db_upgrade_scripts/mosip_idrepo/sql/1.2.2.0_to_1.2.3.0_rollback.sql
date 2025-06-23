
\c mosip_idrepo sysadmin
---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.credential_request_status DROP COLUMN IF EXISTS trigger_action;
ALTER TABLE idrepo.handle DROP COLUMN IF EXISTS status;

------------------------------------------------------------------------------------------------
