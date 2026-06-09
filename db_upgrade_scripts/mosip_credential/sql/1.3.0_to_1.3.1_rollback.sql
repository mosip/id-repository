\c mosip_credential

-- Drop the indices if they were created
DROP INDEX IF EXISTS idx_credential_transaction_status_code;
DROP INDEX IF EXISTS idx_credential_transaction_status_upd;
DROP INDEX IF EXISTS idx_credential_transaction_status_cr;
