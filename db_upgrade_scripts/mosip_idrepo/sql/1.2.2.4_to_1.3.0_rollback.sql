\c mosip_idrepo

--INDEX
DROP INDEX IF EXISTS idx_uin_uin_hash ON idrepo.uin;
DROP INDEX IF EXISTS idx_uin_draft_reg_id ON idrepo.uin_draft;
DROP INDEX IF EXISTS idx_uin_h_reg_id ON idrepo.uin_h;
DROP INDEX IF EXISTS idx_handle_handle_hash ON idrepo.handle;
DROP INDEX IF EXISTS idx_handle_uin_hash ON idrepo.handle;
DROP INDEX IF EXISTS idx_cred_req_NEW_status_cr_dtimes ON idrepo.credential_request_status;
DROP INDEX IF EXISTS idx_cred_tran_status ON idrepo.credential_request_status;

-- ROLLBACK FOR PERFORMANCE OPTIMIZATION INDEXES

DROP INDEX IF EXISTS idrepo.idx_uin_auth_lock_hash_type_crdtimes;
DROP INDEX IF EXISTS idrepo.idx_uin_auth_lock_covering;

DROP INDEX IF EXISTS idrepo.idx_cred_individual_id_deleted;
DROP INDEX IF EXISTS idrepo.idx_cred_individual_id_hash_deleted;
DROP INDEX IF EXISTS idrepo.idx_cred_hash_partner_deleted;
DROP INDEX IF EXISTS idrepo.idx_cred_expiry_ts;
DROP INDEX IF EXISTS idrepo.idx_cred_status_cr_dtimes;
DROP INDEX IF EXISTS idrepo.idx_crs_hash_not_deleted;

-- END ROLLBACK FOR PERFORMANCE OPTIMIZATION INDEXES
