

\c mosip_idmap sysadmin


----------------------------------------------------------------------------------------------------

ALTER TABLE idmap.vid ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE idmap.vid_seed ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE idmap.vid_seq ALTER COLUMN is_deleted SET NOT NULL;

ALTER TABLE idmap.vid ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE idmap.vid_seed ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE idmap.vid_seq ALTER COLUMN is_deleted SET DEFAULT FALSE;


----------------------------------------------------------------------------------------------------
