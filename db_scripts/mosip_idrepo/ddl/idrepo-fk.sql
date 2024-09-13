-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idmap
-- Table Name : IDMAP Table relations
-- Purpose    : All the FKs are created separately, not part of create table scripts to ease the deployment process
--           
-- Create By   : Nasir Khan / Sadanandegowda
-- Created Date: 15-Jul-2019
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- 
-- ------------------------------------------------------------------------------------------

-- object: fk_uind_uin | type: CONSTRAINT --
-- ALTER TABLE idrepo.uin_document DROP CONSTRAINT IF EXISTS fk_uind_uin CASCADE;
ALTER TABLE idrepo.uin_document ADD CONSTRAINT fk_uind_uin FOREIGN KEY (uin_ref_id)
REFERENCES idrepo.uin (uin_ref_id) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION;
-- ddl-end --



-- object: fk_uinb_uin | type: CONSTRAINT --
-- ALTER TABLE idrepo.uin_biometric DROP CONSTRAINT IF EXISTS fk_uinb_uin CASCADE;
ALTER TABLE idrepo.uin_biometric ADD CONSTRAINT fk_uinb_uin FOREIGN KEY (uin_ref_id)
REFERENCES idrepo.uin (uin_ref_id) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION;
-- ddl-end --

--INDEX 
CREATE INDEX IF NOT EXISTS idx_uin_uin_hash ON idrepo.uin USING btree(uin_hash);
CREATE INDEX IF NOT EXISTS idx_uin_draft_reg_id ON idrepo.uin_draft USING btree(reg_id);
CREATE INDEX IF NOT EXISTS idx_uin_h_reg_id ON idrepo.uin_h USING btree(reg_id);
CREATE INDEX IF NOT EXISTS idx_handle_handle_hash ON idrepo.handle USING btree(handle_hash);
CREATE INDEX IF NOT EXISTS cred_status_uin_hash ON idrepo.handle USING btree(uin_hash);
CREATE INDEX IF NOT EXISTS cred_req_NEW_status_cr_dtimes ON idrepo.credential_request_status USING btree(cr_dtimes) WHERE status = 'NEW';
CREATE INDEX IF NOT EXISTS cred_tran_status ON idrepo.credential_request_status USING  btree(status);