\c mosip_idrepo

REASSIGN OWNED BY sysadmin TO postgres;

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA idrepo FROM idrepouser;

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA idrepo FROM sysadmin;

GRANT SELECT, INSERT, REFERENCES, UPDATE, DELETE ON ALL TABLES IN SCHEMA idrepo TO idrepouser;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA idrepo TO postgres;

ALTER TABLE idrepo.uin_auth_lock ADD COLUMN unlock_expiry_datetime timestamp;
------------------------------------------------------------------------------------------------

CREATE TABLE idrepo.credential_request_status (
	individual_id character varying(500) NOT NULL,
	individual_id_hash character varying(128) NOT NULL,
	partner_id character varying(36) NOT NULL,
	request_id character varying(36),
	token_id character varying(128),
	status character varying(36) NOT NULL,
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

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE
   ON idrepo.credential_request_status
   TO idrepouser;
----------------------------------------------------------------------------------------------------------------------------------
CREATE TABLE idrepo.uin_biometric_draft(
	reg_id character varying(39) NOT NULL,								
	biometric_file_type character varying(36) NOT NULL,						
	bio_file_id character varying(128) NOT NULL,					
	biometric_file_name character varying(128) NOT NULL,						
	biometric_file_hash character varying(64) NOT NULL,					
	cr_by character varying(256) NOT NULL,		
	cr_dtimes timestamp NOT NULL,			
	upd_by character varying(256),		
	upd_dtimes timestamp,	
	is_deleted bool	DEFAULT	FALSE,
	del_dtimes timestamp,														
	CONSTRAINT pk_uinbiodft_id PRIMARY KEY (reg_id,biometric_file_type),
	CONSTRAINT unq_biofileid UNIQUE (bio_file_id)
);

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE
   ON idrepo.uin_biometric_draft
   TO idrepouser;
-----------------------------------------------------------------------------------------------------------------------------------------------
CREATE TABLE idrepo.uin_draft(
	reg_id character varying (39) NOT NULL,
	uin character varying (500) NOT NULL,
	uin_hash character varying (128) NOT NULL,
	uin_data bytea,		
	uin_data_hash character varying (64),			
	status_code character varying (32) NOT NULL,
	cr_by character varying (256) NOT NULL,		
	cr_dtimes timestamp NOT NULL,		
	upd_by	character varying (256),			
	upd_dtimes timestamp,			
	is_deleted bool	DEFAULT FALSE,
	del_dtimes timestamp, 
	CONSTRAINT pk_uindft_id PRIMARY KEY (reg_id),
	CONSTRAINT unq_uin UNIQUE (uin),
	CONSTRAINT unq_uinhsh UNIQUE (uin_hash)
);

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE
   ON idrepo.uin_draft
   TO idrepouser;
---------------------------------------------------------------------------------------------------------------------------------------------
CREATE TABLE idrepo.uin_document_draft(
	reg_id character varying(39) NOT NULL,										
	doccat_code character varying(36) NOT NULL,									
	doctyp_code character varying(64) NOT NULL,										
	doc_id character varying(128) NOT NULL,							
	doc_name character varying(128)	NOT NULL,								
	docfmt_code character varying(36) NOT NULL,							
	doc_hash character varying(64) NOT NULL,						
	cr_by character varying(256) NOT NULL,					
	cr_dtimes timestamp NOT NULL,				
	upd_by character varying(256),			
	upd_dtimes timestamp,		
	is_deleted bool	DEFAULT	FALSE,
	del_dtimes timestamp,
	CONSTRAINT pk_uindocdft_id PRIMARY KEY (reg_id,doccat_code),
	CONSTRAINT unq_docid UNIQUE (doc_id)
);

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE
   ON idrepo.uin_document_draft
   TO idrepouser;

-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
CREATE TABLE idrepo.identity_update_count_tracker (
	id varchar NOT NULL,
	identity_update_count bytea NOT NULL,
	CONSTRAINT iut_pk PRIMARY KEY (id)
);

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE
   ON idrepo.identity_update_count_tracker
   TO idrepouser;

-------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.uin ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_auth_lock ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_biometric ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_biometric_h ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_document ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_document_h ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_h ALTER COLUMN lang_code DROP NOT NULL;
---------------------------------------------------------------------------------
