-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Release Version 	: 1.2.1
-- Purpose    		: Database Alter scripts for the release for ID Repository DB.       
-- Create By   		: Anusha SE
-- Created Date		: Dec-2023
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin
---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.credential_request_status ALTER COLUMN request_id type character varying(64);

ALTER TABLE idrepo.uin_auth_lock ADD COLUMN lock_excluded_auth_partners character varying(256);
------------------------------------------------------------------------------------------------

\ir ../ddl/idrepo-handle.sql

-------------------------------------------------------------------------------------------------
