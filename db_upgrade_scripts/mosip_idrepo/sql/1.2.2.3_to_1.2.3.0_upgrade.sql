
\c mosip_idrepo sysadmin
---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.handle ADD COLUMN status character varying(32) NOT NULL DEFAULT 'ACTIVATED';

------------------------------------------------------------------------------------------------
