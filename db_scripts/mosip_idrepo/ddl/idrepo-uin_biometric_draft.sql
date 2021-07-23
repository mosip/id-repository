-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Table Name 	: idrepo.uin_biometric_draft
-- Purpose    	: UIN Biometric Draft: 
--           
-- Create By   	: Ram Bhatt
-- Created Date	: Jul-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- 
-- ------------------------------------------------------------------------------------------

-- object: idrepo.uin_biometric_draft | type: TABLE --
-- DROP TABLE IF EXISTS idrepo.uin_biometric_draft CASCADE;
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
-- ddl-end --

