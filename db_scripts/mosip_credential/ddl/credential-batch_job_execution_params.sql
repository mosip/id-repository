-- Table: credential.batch_job_execution_params

-- DROP TABLE credential.batch_job_execution_params;

CREATE TABLE credential.batch_job_execution_params  (
	JOB_EXECUTION_ID BIGINT NOT NULL ,
	PARAMETER_NAME VARCHAR(100) NOT NULL ,
	PARAMETER_TYPE VARCHAR(100) NOT NULL ,
	PARAMETER_VALUE VARCHAR(2500) ,
	IDENTIFYING CHAR(1) NOT NULL ,
	constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
)
WITH (
    OIDS = FALSE
);
