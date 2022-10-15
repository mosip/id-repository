\c mosip_credential sysadmin

ALTER TABLE credential.credential_transaction ADD COLUMN status_comment character varying(512) ;
