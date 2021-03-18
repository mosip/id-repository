-- object: credentialuser | type: ROLE --
-- DROP ROLE IF EXISTS credentialuser;
CREATE ROLE credentialuser WITH 
	INHERIT
	LOGIN
	PASSWORD :dbuserpwd;
-- ddl-end --
