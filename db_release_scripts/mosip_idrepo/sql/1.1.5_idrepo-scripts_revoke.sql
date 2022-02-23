-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Release Version 	: 1.1.5.2
-- Purpose    		: Revoking Database Alter deployement done for release in ID Repository DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Jan-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-- Oct 2021            Manoj SP           Added anonymous_profile and channel_info tables
-----------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin

DROP TABLE IF EXISTS idrepo.anonymous_profile;
DROP TABLE IF EXISTS idrepo.channel_info;
