

\c mosip_credential sysadmin

----------------------------------------------------------------------------------------------------

----------------------------------CREDENTIAL DB ALTER SCRIPT----------------------------------------


ALTER TABLE credential.credential_transaction ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE credential.credential_transaction ALTER COLUMN is_deleted SET DEFAULT FALSE;


------------------------------------------------------------------------------------------------------
