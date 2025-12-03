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

-- PERFORMANCE OPTIMIZATION INDEXES
ALTER TABLE batch_job_execution SET (autovacuum_vacuum_scale_factor = 0.05, autovacuum_vacuum_threshold = 1000, autovacuum_analyze_scale_factor = 0.03, autovacuum_analyze_threshold = 500);

-- PERFORMANCE INDEXES START--
CREATE INDEX IF NOT EXISTS idx_job_exec_instance ON credential.batch_job_execution USING btree (job_instance_id);
-- PERFORMANCE INDEXES END--