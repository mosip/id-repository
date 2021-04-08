-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_credential
-- Release Version 	: 1.1.5
-- Purpose    		: Database Alter scripts for the release for credential DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Jan-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-- Mar-2021		Ram Bhatt	    Reverting is_deleted not null changes
-----------------------------------------------------------------------------------------------------

\c mosip_credential sysadmin

----------------------------------------------------------------------------------------------------

----------------------------------CREDENTIAL DB ALTER SCRIPT----------------------------------------


--ALTER TABLE credential.credential_transaction ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE credential.credential_transaction ALTER COLUMN is_deleted SET DEFAULT FALSE;


------------------------------------------------------------------------------------------------------
