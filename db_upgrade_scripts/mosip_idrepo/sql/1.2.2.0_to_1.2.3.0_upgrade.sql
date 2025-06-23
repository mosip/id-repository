
\c mosip_idrepo sysadmin
---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.credential_request_status ADD COLUMN trigger_action character varying(36);
ALTER TABLE idrepo.handle ADD COLUMN status character varying(32) NOT NULL DEFAULT 'ACTIVATED';

------------------------------------------------------------------------------------------------
