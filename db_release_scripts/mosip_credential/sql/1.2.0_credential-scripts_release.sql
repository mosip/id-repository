

\c mosip_credential sysadmin

CREATE INDEX IF NOT EXISTS idx_cred_trn_cr_dtimes ON credential.credential_transaction USING btree (cr_dtimes);


---------------------------------------------------------------------------------------------------
