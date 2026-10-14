-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Release Version 	: 1.4.1
-- Purpose    		: Array-of-handles — handle.status and credential_request_status.individual_id width.
--
-- ONE-WAY NOTE (individual_id):
--   Widening varchar(500) → varchar(1024) is additive and safe.
--   After this release, encrypted handle IDs may exceed 500 characters.
--   Revoking to varchar(500) is NOT lossless: PostgreSQL will FAIL the ALTER
--   if any row is longer than 500. Do not force a substring cast.
--   Treat this column widen as one-way once production data is written.
-- -------------------------------------------------------------------------------------------------

\c mosip_idrepo sysadmin
---------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.handle ADD COLUMN IF NOT EXISTS status character varying(32) NOT NULL DEFAULT 'ACTIVATED';

COMMENT ON COLUMN idrepo.handle.status IS 'Handle lifecycle: ACTIVATED, DELETE, DELETE_REQUESTED';

-- One-way widen. See header: revoke to varchar(500) fails or loses data if values > 500.
ALTER TABLE idrepo.credential_request_status ALTER COLUMN individual_id TYPE varchar(1024);

-------------------------------------------------------------------------------------------------
