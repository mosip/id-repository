\c mosip_idrepo

--INDEX
DROP INDEX IF EXISTS idrepo.idx_uin_uin_hash;
DROP INDEX IF EXISTS idrepo.idx_uin_draft_reg_id;
DROP INDEX IF EXISTS idrepo.idx_uin_h_reg_id;
DROP INDEX IF EXISTS idrepo.idx_handle_handle_hash;
DROP INDEX IF EXISTS idrepo.idx_handle_uin_hash;
DROP INDEX IF EXISTS idrepo.idx_cred_req_NEW_status_cr_dtimes;
DROP INDEX IF EXISTS idrepo.idx_cred_tran_status;

-- Below script required to rollback from 1.3.0-beta.1 to 1.3.0.

-- ROLLBACK FOR PERFORMANCE OPTIMIZATION INDEXES

DROP INDEX IF EXISTS idrepo.idx_uin_auth_lock_hash_type_crdtimes;
DROP INDEX IF EXISTS idrepo.idx_uin_auth_lock_covering;

DROP INDEX IF EXISTS idrepo.idx_cred_individual_id_deleted;
DROP INDEX IF EXISTS idrepo.idx_cred_individual_id_hash_deleted;
DROP INDEX IF EXISTS idrepo.idx_cred_hash_partner_deleted;
DROP INDEX IF EXISTS idrepo.idx_cred_expiry_ts;
DROP INDEX IF EXISTS idrepo.idx_cred_status_cr_dtimes;
DROP INDEX IF EXISTS idrepo.idx_crs_hash_not_deleted;
DROP INDEX IF EXISTS idrepo.idx_uin_status;

ALTER TABLE idrepo.credential_request_status RESET (autovacuum_vacuum_scale_factor, autovacuum_vacuum_threshold, autovacuum_analyze_scale_factor, autovacuum_analyze_threshold);
ALTER TABLE idrepo.channel_info RESET (autovacuum_vacuum_scale_factor, autovacuum_vacuum_threshold, autovacuum_analyze_scale_factor, autovacuum_analyze_threshold);

-- END ROLLBACK FOR PERFORMANCE OPTIMIZATION INDEXES
