-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Rollback      : 1.4.1 → 1.4.0
-- Purpose       : Reverse array-of-handles schema changes
--
-- DATA LOSS / ONE-WAY:
--   1) handle.status — DROP COLUMN discards ACTIVATED / DELETE / DELETE_REQUESTED.
--   2) credential_request_status.individual_id varchar(1024) → varchar(500):
--      This is NOT a lossless reverse of the 1.4.0→1.4.1 widen.
--      PostgreSQL will ABORT with "value too long for type character varying(500)"
--      if any stored individual_id is longer than 500 (expected for encrypted
--      handle IDs after AoH). It will not silently truncate.
--      Do NOT add USING substring/left(..., 500) to force the ALTER — that
--      would permanently destroy the tail of those identifiers.
--      If such rows exist, stop; keep the 1024 width or restore from backup.
-- -------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin
---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.handle DROP COLUMN IF EXISTS status;

-- Fails (does not truncate) if any individual_id is longer than 500. See header.
ALTER TABLE idrepo.credential_request_status ALTER COLUMN individual_id TYPE varchar(500);

------------------------------------------------------------------------------------------------
