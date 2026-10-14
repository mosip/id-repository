-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Release Version 	: 1.4.1
-- Purpose    		: Revoke array-of-handles schema changes.
--
-- DATA LOSS / ONE-WAY:
--   1) handle.status — DROP COLUMN discards lifecycle values.
--   2) credential_request_status.individual_id varchar(1024) → varchar(500):
--      PostgreSQL will ABORT with "value too long for type character varying(500)"
--      if any stored individual_id is longer than 500. It will not silently
--      truncate. Do NOT force USING substring/left(..., 500) — that destroys data.
--      If such rows exist, keep the 1024 width or restore from backup.
-- -------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin

ALTER TABLE idrepo.handle DROP COLUMN IF EXISTS status;

-- Fails (does not truncate) if any individual_id is longer than 500. See header.
ALTER TABLE idrepo.credential_request_status ALTER COLUMN individual_id TYPE varchar(500);
