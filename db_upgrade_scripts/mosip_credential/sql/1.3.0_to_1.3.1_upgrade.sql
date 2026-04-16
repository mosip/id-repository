\c mosip_credential

-- PERFORMANCE INDEXES START--
CREATE INDEX IF NOT EXISTS idx_credential_transaction_status_code ON credential_transaction (status_code);
CREATE INDEX IF NOT EXISTS idx_credential_transaction_status_upd ON credential_transaction (status_code, upd_dtimes);
CREATE INDEX IF NOT EXISTS idx_credential_transaction_status_cr ON credential_transaction (status_code, cr_dtimes);
-- PERFORMANCE INDEXES END--
