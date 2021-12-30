

\c mosip_idrepo sysadmin


DROP TABLE IF EXISTS idrepo.uin_auth_lock;

CREATE INDEX IF NOT EXISTS idx_uin_h_reg_id ON idrepo.uin_h USING btree (reg_id);

\ir ../ddl/idrepo-uin_auth_lock.sql
