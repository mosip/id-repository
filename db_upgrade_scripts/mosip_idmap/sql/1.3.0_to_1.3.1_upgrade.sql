\c :mosipdbname


-- PERFORMANCE INDEXES START--
CREATE INDEX idx_vid_vid ON idmap.vid (vid);
CREATE INDEX idx_vid_uinhash_status_vidtype ON idmap.vid (uin_hash, status_code, vidtyp_code);
-- PERFORMANCE INDEXES END--