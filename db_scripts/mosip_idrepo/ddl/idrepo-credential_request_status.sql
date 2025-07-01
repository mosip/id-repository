-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_repo
-- Table Name 	: idrepo.credential_request_status
-- Purpose    	: Credential Request Status: 
--           
-- Create By   	: Ram Bhatt
-- Created Date	: May-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- 
-- ------------------------------------------------------------------------------------------

-- object: idrepo.credential_request_status | type: TABLE --
-- DROP TABLE IF EXISTS idrepo.credential_request_status CASCADE;
CREATE TABLE idrepo.credential_request_status (
	individual_id character varying(500) NOT NULL,
	individual_id_hash character varying(128) NOT NULL,
	partner_id character varying(36) NOT NULL,
	request_id character varying(64),
	token_id character varying(128),
	status character varying(36) NOT NULL,
	trigger_action character varying(36),
	id_transaction_limit numeric,
	id_expiry_timestamp timestamp,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted bool DEFAULT false,
	del_dtimes timestamp,
	CONSTRAINT credential_request_status_pk PRIMARY KEY (individual_id_hash,partner_id)

);
-- ddl-end --
--index section starts----
CREATE INDEX IF NOT EXISTS idx_cred_req_NEW_status_cr_dtimes ON idrepo.credential_request_status USING btree(cr_dtimes) WHERE status = 'NEW';
CREATE INDEX IF NOT EXISTS idx_cred_tran_status ON idrepo.credential_request_status USING  btree(status);
CREATE INDEX IF NOT EXISTS cred_tran_indidhash ON idrepo.credential_request_status USING btree (individual_id_hash);
--index section ends------
