-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Release Version 	: 1.2
-- Purpose    		: Revoking Database Alter deployement done for release in ID Repository DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Apr-2021
-- 
-- Modified Date       Modified By       Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-- Sep 2021            Manoj SP           Added anonymous_profile and channel_info tables
-- Jul 2022	           Manoj SP           Added idrepo-identity_update_count_tracker table
-----------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin

ALTER TABLE idrepo.uin_auth_lock DROP COLUMN unlock_expiry_datetime;
DROP TABLE IF EXISTS idrepo.credential_request_status;
-----------------------------------------------------------------------------------------------------


DROP TABLE IF EXISTS idrepo.uin_biometric_draft;
DROP TABLE IF EXISTS idrepo.uin_draft;
DROP TABLE IF EXISTS idrepo.uin_document_draft;

------------------------------------------------------------------------------------------------------

DROP TABLE IF EXISTS idrepo.anonymous_profile;
DROP TABLE IF EXISTS idrepo.channel_info;
------------------------------------------------------------------------------------------------------

DROP TABLE IF EXISTS idrepo.idrepo-identity_update_count_tracker;