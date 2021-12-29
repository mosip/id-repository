-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_credential
-- Release Version 	: 1.2
-- Purpose    		: Database Alter scripts for the release for credential DB.       
-- Create By   		: Sadanandegowda DM
-- Created Date		: Sep-2020
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_credential sysadmin

CREATE INDEX IF NOT EXISTS idx_cred_trn_cr_dtimes ON credential.credential_transaction USING btree (cr_dtimes);


---------------------------------------------------------------------------------------------------
