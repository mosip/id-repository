\c mosip_idrepo

--INDEX
CREATE INDEX IF NOT EXISTS idx_uin_uin_hash ON idrepo.uin USING btree(uin_hash);
CREATE INDEX IF NOT EXISTS idx_uin_draft_reg_id ON idrepo.uin_draft USING btree(reg_id);
CREATE INDEX IF NOT EXISTS idx_uin_h_reg_id ON idrepo.uin_h USING btree(reg_id);
CREATE INDEX IF NOT EXISTS idx_handle_handle_hash ON idrepo.handle USING btree(handle_hash);
CREATE INDEX IF NOT EXISTS idx_handle_uin_hash ON idrepo.handle USING btree(uin_hash);
CREATE INDEX IF NOT EXISTS idx_cred_req_NEW_status_cr_dtimes ON idrepo.credential_request_status USING btree(cr_dtimes) WHERE status = 'NEW';
CREATE INDEX IF NOT EXISTS idx_cred_tran_status ON idrepo.credential_request_status USING  btree(status);

-- Below script required to upgraded from 1.3.0-beta.1 to 1.3.0

-- UPGRADE FOR PERFORMANCE OPTIMIZATION INDEXES

CREATE INDEX idx_uin_auth_lock_hash_type_crdtimes ON idrepo.uin_auth_lock (uin_hash, auth_type_code, cr_dtimes);
CREATE INDEX idx_uin_auth_lock_covering ON idrepo.uin_auth_lock (uin_hash, auth_type_code, cr_dtimes, status_code, unlock_expiry_datetime);

CREATE INDEX IF NOT EXISTS idx_cred_individual_id_deleted ON idrepo.credential_request_status(individual_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_cred_individual_id_hash_deleted ON idrepo.credential_request_status(individual_id_hash, is_deleted);
CREATE INDEX IF NOT EXISTS idx_cred_hash_partner_deleted ON idrepo.credential_request_status(individual_id_hash, partner_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_cred_expiry_ts ON idrepo.credential_request_status(id_expiry_timestamp);
CREATE INDEX IF NOT EXISTS idx_cred_status_cr_dtimes ON idrepo.credential_request_status(status, cr_dtimes);
CREATE INDEX idx_crs_hash_not_deleted ON idrepo.credential_request_status (individual_id_hash) WHERE is_deleted = false;

-- END UPGRADE FOR PERFORMANCE OPTIMIZATION INDEXES
