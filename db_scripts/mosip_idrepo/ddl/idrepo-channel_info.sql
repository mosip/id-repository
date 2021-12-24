-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_idrepo
-- Table Name 	: idrepo.channel_info
-- Purpose    	: channel_info: Channel information for reporting purpose.
--           
-- Create By   	: Manoj SP
-- Created Date	: 10-Sep-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- ------------------------------------------------------------------------------------------
-- Oct-2021             Manoj SP            Created channel_info table 
-- ------------------------------------------------------------------------------------------

-- object: idrepo.channel_info | type: TABLE --
-- DROP TABLE IF EXISTS idrepo.channel_info CASCADE;
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
-- ddl-end --
COMMENT ON TABLE idrepo.channel_info IS 'channel_info: Anonymous profiling information on channels such as phone,email for reporting purpose.';
-- ddl-end --
COMMENT ON COLUMN idrepo.channel_info.hashed_channel IS 'Hashed Channel: Hash of the phone/email channel.';
-- ddl-end --
COMMENT ON COLUMN idrepo.channel_info.channel_type IS 'Channel Type : Whether channel is email or phone.';
-- ddl-end --
COMMENT ON COLUMN idrepo.channel_info.no_of_records IS 'No of records : Cumulative count of specific email or phone channel.';
-- ddl-end --
COMMENT ON COLUMN idrepo.channel_info.cr_by IS 'Created By : ID or name of the user who create / insert record';
-- ddl-end --
COMMENT ON COLUMN idrepo.channel_info.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted';
-- ddl-end --
COMMENT ON COLUMN idrepo.channel_info.upd_by IS 'Updated By : ID or name of the user who update the record with new values';
-- ddl-end --
COMMENT ON COLUMN idrepo.channel_info.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';
-- ddl-end --
COMMENT ON COLUMN idrepo.channel_info.is_deleted IS 'IS_Deleted : Flag to mark whether the record is Soft deleted.';
-- ddl-end --
COMMENT ON COLUMN idrepo.channel_info.del_dtimes IS 'Deleted DateTimestamp : Date and Timestamp when the record is soft deleted with is_deleted=TRUE';
-- ddl-end --
