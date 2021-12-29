-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Release Version 	: 1.2
-- Purpose    		: Database Alter scripts for the release for ID Repository DB.       
-- Create By   		: Sadanandegowda DM
-- Created Date		: Sep-2020
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin


DROP TABLE IF EXISTS idrepo.uin_auth_lock;

CREATE INDEX IF NOT EXISTS idx_uin_h_reg_id ON idrepo.uin_h USING btree (reg_id);

\ir ../ddl/idrepo-uin_auth_lock.sql
