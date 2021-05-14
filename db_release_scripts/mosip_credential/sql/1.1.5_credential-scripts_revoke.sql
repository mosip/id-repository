-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_credential
-- Release Version 	: 1.1.5.2
-- Purpose    		: Revoking Database Alter deployement done for release in mosip_credential DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Jan-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_credential sysadmin

ALTER TABLE credential.credential_transaction DROP COLUMN status_comment ;
