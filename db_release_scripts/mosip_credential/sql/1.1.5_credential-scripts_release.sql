-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_credential
-- Release Version 	: 1.1.5.2
-- Purpose    		: Database Alter scripts for the release for credential DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Jan-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-- Mar-2021		Ram Bhatt	    Reverting is_deleted not null changes
-- Apr-2021		Ram Bhatt	    status_comment column added.
-----------------------------------------------------------------------------------------------------

\c mosip_credential sysadmin

----------------------------------------------------------------------------------------------------

----------------------------------CREDENTIAL DB ALTER SCRIPT----------------------------------------
ALTER TABLE credential.credential_transaction ADD COLUMN status_comment character varying(512)  ;

--ALTER TABLE credential.credential_transaction ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE credential.credential_transaction ALTER COLUMN is_deleted SET DEFAULT FALSE;


------------------------------------------------------------------------------------------------------
