--INDEX
CREATE INDEX IF NOT EXISTS idx_uin_uin_hash ON idrepo.uin USING btree(uin_hash);
CREATE INDEX IF NOT EXISTS idx_uin_draft_reg_id ON idrepo.uin_draft USING btree(reg_id);
CREATE INDEX IF NOT EXISTS idx_uin_h_reg_id ON idrepo.uin_h USING btree(reg_id);
CREATE INDEX IF NOT EXISTS idx_handle_handle_hash ON idrepo.handle USING btree(handle_hash);
CREATE INDEX IF NOT EXISTS idx_handle_uin_hash ON idrepo.handle USING btree(uin_hash);
CREATE INDEX IF NOT EXISTS idx_cred_req_NEW_status_cr_dtimes ON idrepo.credential_request_status USING btree(cr_dtimes) WHERE status = 'NEW';
CREATE INDEX IF NOT EXISTS idx_cred_tran_status ON idrepo.credential_request_status USING  btree(status);