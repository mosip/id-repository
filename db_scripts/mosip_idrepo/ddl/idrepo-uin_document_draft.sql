-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_repo
-- Table Name 	: idrepo.uin_document_draft
-- Purpose    	: UIN Document Draft: 
--           
-- Create By   	: Ram Bhatt
-- Created Date	: Jul-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- 
-- ------------------------------------------------------------------------------------------

-- object: idrepo.uin_document_draft | type: TABLE --
-- DROP TABLE IF EXISTS idrepo.uin_document_draft CASCADE;
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
-- ddl-end --

