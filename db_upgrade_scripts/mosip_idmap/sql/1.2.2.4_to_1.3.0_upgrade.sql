\c mosip_idmap


-- PERFORMANCE INDEXES START--
CREATE INDEX IF NOT EXISTS idx_vid_status_expiry ON idmap.vid USING btree (status_code, expiry_dtimes DESC) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_vid_status_expiry_ind ON idmap.vid USING btree (status_code, expiry_dtimes);
CREATE INDEX IF NOT EXISTS idx_vid_uinhash_status_expiry ON idmap.vid USING btree (uin_hash, status_code, expiry_dtimes DESC) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_vid_uinhash_status_expiry_ind ON idmap.vid USING btree (uin_hash, status_code, expiry_dtimes);
CREATE INDEX IF NOT EXISTS idx_vid_uinhash_status_vidtype_expiry ON idmap.vid USING btree (uin_hash, status_code, vidtyp_code, expiry_dtimes DESC) WHERE (is_deleted = false);
CREATE INDEX IF NOT EXISTS idx_vid_uinhash_status_vidtype_expiry_ind ON idmap.vid USING btree (uin_hash, status_code, vidtyp_code, expiry_dtimes);
-- PERFORMANCE INDEXES END--