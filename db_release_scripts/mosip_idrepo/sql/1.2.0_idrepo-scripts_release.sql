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
-- Jul-2021		Ram Bhatt	    Lang Code nullable for multiple tables
-- Jul-2021		Manoj SP	    Addition of anonymous profile column to tables
-- Sep-2021		Manoj SP	    Removed Anonymous Profile alter scripts
-- Sep-2021		Manoj SP	    Added anonymous_profile and channel_info tables.
-- Jul 2022	           Manoj SP           Added idrepo-identity_update_count_tracker table
-------------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin
---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.uin_auth_lock ADD COLUMN unlock_expiry_datetime timestamp;
------------------------------------------------------------------------------------------------

\ir ../ddl/idrepo-credential_request_status.sql

\ir ../ddl/idrepo-uin_biometric_draft.sql
\ir ../ddl/idrepo-uin_draft.sql
\ir ../ddl/idrepo-uin_document_draft.sql
\ir ../ddl/idrepo-anonymous_profile.sql
\ir ../ddl/idrepo-channel_info.sql
\ir ../ddl/idrepo-identity_update_count_tracker.sql

-------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.uin ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_auth_lock ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_biometric ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_biometric_h ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_document ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_document_h ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_h ALTER COLUMN lang_code DROP NOT NULL;
---------------------------------------------------------------------------------
