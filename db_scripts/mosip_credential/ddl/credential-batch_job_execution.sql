-- Table: credential.batch_job_execution

-- DROP TABLE credential.batch_job_execution;

CREATE TABLE credential.batch_job_execution  (
    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
	VERSION BIGINT,
	JOB_INSTANCE_ID BIGINT NOT NULL,
	CREATE_TIME TIMESTAMP NOT NULL,
	START_TIME TIMESTAMP DEFAULT NULL,
	END_TIME TIMESTAMP DEFAULT NULL,
	STATUS VARCHAR(10),
	EXIT_CODE VARCHAR(2500),
	EXIT_MESSAGE VARCHAR(2500),
	LAST_UPDATED TIMESTAMP
) 
WITH (
    OIDS = FALSE
);

--PERFORMANCE OPTIMIZATION INDEXES
CREATE UNIQUE INDEX batch_job_execution_pkey ON credential.batch_job_execution USING btree (job_execution_id);
CREATE INDEX idx_job_exec_instance ON credential.batch_job_execution USING btree (job_instance_id);
