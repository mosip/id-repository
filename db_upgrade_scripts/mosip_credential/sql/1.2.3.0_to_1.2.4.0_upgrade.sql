\c mosip_credential

CREATE INDEX IF NOT EXISTS cred_txn_status_code ON credential.credential_transaction USING btree (status_code);