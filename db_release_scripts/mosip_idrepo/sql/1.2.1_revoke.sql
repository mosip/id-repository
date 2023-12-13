-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Release Version 	: 1.2.1
-- Purpose    		: Revoking Database Alter deployement done for release in ID Repository DB.       
-- Create By   		: Anusha SE
-- Created Date		: Dec-2023
-- 
-- Modified Date       Modified By       Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin

DROP TABLE IF EXISTS idrepo.handle;

ALTER TABLE idrepo.credential_request_status ALTER COLUMN request_id type character varying(36);
