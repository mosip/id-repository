-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Release Version 	: 1.2
-- Purpose    		: Database Alter scripts for the release for ID Repository DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Apr-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------


\c mosip_idrepo sysadmin


DROP TABLE IF EXISTS idrepo.uin_auth_lock;

\ir ../ddl/idrepo-uin_auth_lock.sql

---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.uin_auth_lock ADD COLUMN unlock_expiry_datetime timestamp;
------------------------------------------------------------------------------------------------

