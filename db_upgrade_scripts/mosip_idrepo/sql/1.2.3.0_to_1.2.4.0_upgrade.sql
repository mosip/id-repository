\c mosip_idrepo sysadmin
---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.handle ADD COLUMN status character varying(32) NOT NULL DEFAULT 'ACTIVATED';

ALTER TABLE idrepo.credential_request_status ALTER COLUMN individual_id TYPE varchar(1024);

------------------------------------------------------------------------------------------------