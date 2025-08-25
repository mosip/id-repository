-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_repo
-- Table Name 	: idrepo.uin_draft
-- Purpose    	: UIN Hash Salt: 
--           
-- Create By   	: Ram Bhatt
-- Created Date	: Jul-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- Sep-2021		Manoj SP	    Removed Anonymous Profile column
-- ------------------------------------------------------------------------------------------

-- object: idrepo.uin_draft | type: TABLE --
-- DROP TABLE IF EXISTS idrepo.uin_draft CASCADE;
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
-- ddl-end --
--index section starts----
CREATE INDEX IF NOT EXISTS idx_uin_draft_reg_id ON idrepo.uin_draft USING btree(reg_id);
--index section ends------
