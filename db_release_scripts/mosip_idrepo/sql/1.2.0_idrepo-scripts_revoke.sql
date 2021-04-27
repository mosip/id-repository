-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Release Version 	: 1.2
-- Purpose    		: Revoking Database Alter deployement done for release in ID Repository DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Apr-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin

DROP TABLE IF EXISTS idrepo.uin_auth_lock;
-----------------------------------------------------------------------------------------------------
