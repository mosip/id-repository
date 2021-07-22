-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Release Version 	: 1.2
-- Purpose    		: Database Alter scripts for the release for ID Repository DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: May-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-- Jul-2021		Ram Bhatt	    Creation of uin biometric draft and uin draft tables
-------------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin
---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.uin_auth_lock ADD COLUMN unlock_expiry_datetime timestamp;
------------------------------------------------------------------------------------------------

\ir ../ddl/idrepo-credential_request_status.sql

\ir ../ddl/idrepo-uin_biometric_draft.sql
\ir ../ddl/idrepo-uin_draft.sql


-------------------------------------------------------------------------------------------------

