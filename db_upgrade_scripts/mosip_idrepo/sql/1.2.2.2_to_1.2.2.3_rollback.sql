--INDEX
DROP INDEX IF EXISTS idx_uin_uin_hash ON idrepo.uin;
DROP INDEX IF EXISTS idx_uin_draft_reg_id ON idrepo.uin_draft;
DROP INDEX IF EXISTS idx_uin_h_reg_id ON idrepo.uin_h;
DROP INDEX IF EXISTS idx_handle_handle_hash ON idrepo.handle;
DROP INDEX IF EXISTS idx_handle_uin_hash ON idrepo.handle;
DROP INDEX IF EXISTS idx_cred_req_NEW_status_cr_dtimes ON idrepo.credential_request_status;
DROP INDEX IF EXISTS idx_cred_tran_status ON idrepo.credential_request_status;