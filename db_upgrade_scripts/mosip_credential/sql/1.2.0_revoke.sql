-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_credential
-- Purpose    		: Revoking Database Alter deployement done for release in mosip_credential DB.       
-- Create By   		: Anusha SE
-- Created Date		: Dec-2023
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_credential sysadmin

ALTER TABLE credential.credential_transaction ALTER COLUMN id type character varying(36);

DROP INDEX IF EXISTS cred_tran_NEW_status_cr_dtimes;

-----------------------------------------------------------------------------------------------------
