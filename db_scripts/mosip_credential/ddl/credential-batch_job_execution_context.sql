-- Table: credential.batch_job_execution_context

-- DROP TABLE credential.batch_job_execution_context;

CREATE TABLE credential.batch_job_execution_context
(
    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
	SHORT_CONTEXT VARCHAR(2500) NOT NULL,
	SERIALIZED_CONTEXT TEXT
)
WITH (
    OIDS = FALSE
);
