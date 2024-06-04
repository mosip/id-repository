-- Table: credential.batch_job_execution_context

-- DROP TABLE credential.batch_job_execution_context;

CREATE TABLE credential.batch_job_execution_context
(
    STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
	SHORT_CONTEXT VARCHAR(2500) NOT NULL,
	SERIALIZED_CONTEXT TEXT ,
	constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
)
WITH (
    OIDS = FALSE
);
