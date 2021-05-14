-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idamap
-- Release Version 	: 1.1.5.2
-- Purpose    		: Database Alter scripts for the release for ID Map DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Jan-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-- Mar-2021		Ram Bhatt	    Reverting is_deleted not null changes
---------------------------------------------------------------------------------------------------

\c mosip_idmap sysadmin


----------------------------------------------------------------------------------------------------

--ALTER TABLE idmap.vid ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE idmap.vid_seed ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE idmap.vid_seq ALTER COLUMN is_deleted SET NOT NULL;

--ALTER TABLE idmap.vid ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE idmap.vid_seed ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE idmap.vid_seq ALTER COLUMN is_deleted SET DEFAULT FALSE;


----------------------------------------------------------------------------------------------------
