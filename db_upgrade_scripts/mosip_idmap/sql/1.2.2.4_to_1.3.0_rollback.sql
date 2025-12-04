\c mosip_idmap

-- Drop the indices if they were created
DROP INDEX IF EXISTS idmap.idx_vid_status_expiry;
DROP INDEX IF EXISTS idmap.idx_vid_status_expiry_ind;
DROP INDEX IF EXISTS idmap.idx_vid_uinhash_status_expiry;
DROP INDEX IF EXISTS idmap.idx_vid_uinhash_status_expiry_ind;
DROP INDEX IF EXISTS idmap.idx_vid_uinhash_status_vidtype_expiry;
DROP INDEX IF EXISTS idmap.idx_vid_uinhash_status_vidtype_expiry_ind;