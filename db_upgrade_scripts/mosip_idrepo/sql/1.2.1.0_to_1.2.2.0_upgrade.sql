-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Purpose    	: Database alter scripts for release of ID Repository DB.       
-- Create By   	: Ritik Jain
-- Created Date	: May-2024
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin
---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.handle ADD COLUMN status character varying(32) NOT NULL DEFAULT 'ACTIVATED';

------------------------------------------------------------------------------------------------
