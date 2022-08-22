-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_credential
-- Purpose    		: Database Alter scripts for the release for credential DB.       
-- Create By   		: Anusha SE
-- Created Date		: Dec-2023
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------

\c mosip_credential sysadmin

ALTER TABLE credential.credential_transaction ALTER COLUMN id type character varying(64);

CREATE INDEX cred_tran_NEW_status_cr_dtimes ON credential.credential_transaction USING btree (cr_dtimes) WHERE status_code = 'NEW';

---------------------------------------------------------------------------------------------------
