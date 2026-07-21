
-- object: idmap | type: SCHEMA --
DROP SCHEMA IF EXISTS idmap CASCADE;
CREATE SCHEMA idmap;


-- object: idmap.uin_hash_salt | type: TABLE --
DROP TABLE IF EXISTS idmap.uin_hash_salt CASCADE;
CREATE TABLE idmap.uin_hash_salt(
	id bigint NOT NULL,
	salt character varying(36) NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	CONSTRAINT pk_uinhs PRIMARY KEY (id)

);

DROP TABLE IF EXISTS idmap.uin_encrypt_salt CASCADE;
CREATE TABLE idmap.uin_encrypt_salt(
	id bigint NOT NULL,
	salt character varying(36) NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	CONSTRAINT pk_uines PRIMARY KEY (id)

);



-- object: idmap | type: SCHEMA --
DROP SCHEMA IF EXISTS idrepo CASCADE;
CREATE SCHEMA idrepo;


-- object: idmap.uin_hash_salt | type: TABLE --
DROP TABLE IF EXISTS idrepo.uin_hash_salt CASCADE;
CREATE TABLE idrepo.uin_hash_salt(
	id bigint NOT NULL,
	salt character varying(36) NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	CONSTRAINT pk_uinhs PRIMARY KEY (id)

);

DROP TABLE IF EXISTS idrepo.uin_encrypt_salt CASCADE;
CREATE TABLE idrepo.uin_encrypt_salt(
	id bigint NOT NULL,
	salt character varying(36) NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	CONSTRAINT pk_uines PRIMARY KEY (id)

);

