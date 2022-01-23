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
COMMENT ON TABLE idrepo.channel_info IS 'channel_info: Anonymous profiling information for reporting purpose.';
COMMENT ON COLUMN idrepo.channel_info.cr_by IS 'Created By : ID or name of the user who create / insert record';
COMMENT ON COLUMN idrepo.channel_info.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted';
COMMENT ON COLUMN idrepo.channel_info.upd_by IS 'Updated By : ID or name of the user who update the record with new values';
COMMENT ON COLUMN idrepo.channel_info.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';
COMMENT ON COLUMN idrepo.channel_info.is_deleted IS 'IS_Deleted : Flag to mark whether the record is Soft deleted.';
COMMENT ON COLUMN idrepo.channel_info.del_dtimes IS 'Deleted DateTimestamp : Date and Timestamp when the record is soft deleted with is_deleted=TRUE';
