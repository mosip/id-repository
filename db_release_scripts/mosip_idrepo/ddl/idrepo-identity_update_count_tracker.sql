CREATE TABLE idrepo.identity_update_count_tracker (
	id varchar NOT NULL,
	identity_update_count bytea NOT NULL,
	CONSTRAINT iut_pk PRIMARY KEY (id)
);

-- Column comments
COMMENT ON COLUMN idrepo.identity_update_count_tracker.id IS 'Hash value of Individual Id';
COMMENT ON COLUMN idrepo.identity_update_count_tracker.identity_update_count IS 'Encoded Identity update count json';
