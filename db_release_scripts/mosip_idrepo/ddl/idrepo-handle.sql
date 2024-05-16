-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_repo
-- Table Name 	: idrepo.handle
-- Purpose    	: Handle and handle hash mapped to UIN.
--           
-- Create By   	: Anusha S E
-- Created Date	: 11-Dec-2023
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- ------------------------------------------------------------------------------------------

-- object: idrepo.handle | type: TABLE --
-- DROP TABLE IF EXISTS idrepo.handle CASCADE;
CREATE TABLE idrepo.handle(
	id character varying(36) NOT NULL,
	uin_hash character varying NOT NULL,
	handle character varying NOT NULL,
	handle_hash character varying NOT NULL,
	status character varying(32) NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	CONSTRAINT pk_handle PRIMARY KEY (id),
	CONSTRAINT uk_handle UNIQUE (handle_hash)
);
-- ddl-end --
COMMENT ON TABLE idrepo.handle IS 'Handle and handle hash mapped to UIN.';
-- ddl-end --
COMMENT ON COLUMN idrepo.handle.id IS 'ID: System generated UUID';
-- ddl-end --
COMMENT ON COLUMN idrepo.handle.handle IS 'Handle : Unique username of the individual.';
-- ddl-end --
COMMENT ON COLUMN idrepo.handle.handle_hash IS 'Handle Hash: Hash value of Unique username of the individual.';
-- ddl-end --
COMMENT ON COLUMN idrepo.handle.status IS 'Status:  Current Status of the Handle.';
-- ddl-end --
COMMENT ON COLUMN idrepo.handle.cr_by IS 'Created By : ID or name of the user who create / insert record';
-- ddl-end --
COMMENT ON COLUMN idrepo.handle.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted';
-- ddl-end --
