\c mosip_idrepo

-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Release Version 	: 1.2
-- Purpose    		: Database Alter scripts for the release for ID Repository DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: May-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-- Jul-2021		Ram Bhatt	    Creation of uin biometric draft and uin draft tables
-- Jul-2021		Ram Bhatt	    Lang Code nullable for multiple tables
-- Jul-2021		Manoj SP	    Addition of anonymous profile column to tables
-- Sep-2021		Manoj SP	    Removed Anonymous Profile alter scripts
-- Sep-2021		Manoj SP	    Added anonymous_profile and channel_info tables.
-- Jul 2022	           Manoj SP           Added idrepo-identity_update_count_tracker table
-------------------------------------------------------------------------------------------------------

REASSIGN OWNED BY sysadmin TO postgres;

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA idrepo FROM idrepouser;

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA idrepo FROM sysadmin;

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE ON ALL TABLES IN SCHEMA idrepo TO idrepouser;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA idrepo TO postgres;

ALTER TABLE idrepo.uin_auth_lock ADD COLUMN unlock_expiry_datetime timestamp;
------------------------------------------------------------------------------------------------

--\ir ../ddl/idrepo-credential_request_status.sql

--\ir ../ddl/idrepo-uin_biometric_draft.sql
--\ir ../ddl/idrepo-uin_draft.sql
--\ir ../ddl/idrepo-uin_document_draft.sql
--\ir ../ddl/idrepo-anonymous_profile.sql
--\ir ../ddl/idrepo-channel_info.sql
--\ir ../ddl/idrepo-identity_update_count_tracker.sql

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

CREATE TABLE idrepo.anonymous_profile(
	id character varying(36) NOT NULL,
	profile character varying NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean DEFAULT FALSE,
	del_dtimes timestamp,
	CONSTRAINT pk_profile PRIMARY KEY (id)
);

CREATE TABLE idrepo.channel_info(
	hashed_channel character varying(128) NOT NULL,
	channel_type character varying(5) NOT NULL,
	no_of_records numeric NOT null default 0,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean DEFAULT FALSE,
	del_dtimes timestamp,
	CONSTRAINT pk_channel_info PRIMARY KEY (hashed_channel)
);

CREATE TABLE idrepo.identity_update_count_tracker (
	id varchar NOT NULL,
	identity_update_count bytea NOT NULL,
	CONSTRAINT iut_pk PRIMARY KEY (id)
);


-------------------------------------------------------------------------------------------------

ALTER TABLE idrepo.uin ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_auth_lock ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_biometric ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_biometric_h ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_document ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_document_h ALTER COLUMN lang_code DROP NOT NULL;
ALTER TABLE idrepo.uin_h ALTER COLUMN lang_code DROP NOT NULL;
---------------------------------------------------------------------------------
