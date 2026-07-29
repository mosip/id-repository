\c mosip_idrepo

--INDEX
DROP INDEX IF EXISTS idx_uin_uin_hash;
DROP INDEX IF EXISTS idx_uin_draft_reg_id;
DROP INDEX IF EXISTS idx_uin_h_reg_id;
DROP INDEX IF EXISTS idx_handle_handle_hash;
DROP INDEX IF EXISTS idx_handle_uin_hash;
DROP INDEX IF EXISTS idx_cred_req_NEW_status_cr_dtimes;
DROP INDEX IF EXISTS idx_cred_tran_status;
DROP INDEX IF EXISTS cred_tran_indidhash;