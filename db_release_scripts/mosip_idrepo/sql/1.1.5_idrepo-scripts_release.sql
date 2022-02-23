-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Release Version 	: 1.1.5.2
-- Purpose    		: Database Alter scripts for the release for ID Repository DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Jan-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-- Mar-2021		Ram Bhatt	    Reverting is_deleted not null changes
-- Oct-2021		Manoj SP	    Added anonymous_profile and channel_info tables.
----------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin

\ir ../ddl/idrepo-anonymous_profile.sql
\ir ../ddl/idrepo-channel_info.sql

----------------------------------------------------------------------------------------------------

--ALTER TABLE idrepo.uin_h ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE idrepo.uin_biometric_h ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE idrepo.uin_document_h ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE idrepo.uin_auth_lock ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE idrepo.uin_document ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE idrepo.uin_biometric ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE idrepo.uin ALTER COLUMN is_deleted SET NOT NULL;


--ALTER TABLE idrepo.uin_h ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE idrepo.uin_biometric_h ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE idrepo.uin_document_h ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE idrepo.uin_auth_lock ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE idrepo.uin_document ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE idrepo.uin_biometric ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE idrepo.uin ALTER COLUMN is_deleted SET DEFAULT FALSE;

-----------------------------------------------------------------------------------------------------
