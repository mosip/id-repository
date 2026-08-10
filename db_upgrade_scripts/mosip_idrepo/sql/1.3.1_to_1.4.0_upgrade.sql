-- -------------------------------------------------------------------------------------------------
-- Migration: 1.3.1 → 1.4.0
-- Purpose  : Allow NULL uin/uin_hash in uin_draft to support LOST-packet drafts that are
--            created before the resident's UIN is resolved via ABIS matching.
-- -------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.uin_draft ALTER COLUMN uin DROP NOT NULL;
ALTER TABLE idrepo.uin_draft ALTER COLUMN uin_hash DROP NOT NULL;
