-- ------------------------------------------------------------------------------------------
-- Upgrade script for Migrating Spring batch version to 5.0 as part of Java 21 Migration.
-- References: 
--  1. https://github.com/spring-projects/spring-batch/wiki/Spring-Batch-5.0-Migration-Guide#ms-sqlserver
--  2. https://github.com/spring-projects/spring-batch/blob/main/spring-batch-core/src/main/resources/org/springframework/batch/core/migration/5.0/migration-postgresql.sql
-- ------------------------------------------------------------------------------------------
ALTER TABLE BATCH_STEP_EXECUTION ADD CREATE_TIME TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:00';
ALTER TABLE BATCH_STEP_EXECUTION ALTER COLUMN START_TIME DROP NOT NULL;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS DROP COLUMN DATE_VAL;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS DROP COLUMN LONG_VAL;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS DROP COLUMN DOUBLE_VAL;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS ALTER COLUMN TYPE_CD TYPE VARCHAR(100);
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS RENAME TYPE_CD TO PARAMETER_TYPE;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS ALTER COLUMN KEY_NAME TYPE VARCHAR(100);
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS RENAME KEY_NAME TO PARAMETER_NAME;
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS ALTER COLUMN STRING_VAL TYPE VARCHAR(2500);
ALTER TABLE BATCH_JOB_EXECUTION_PARAMS RENAME STRING_VAL TO PARAMETER_VALUE;
ALTER TABLE BATCH_JOB_EXECUTION DROP COLUMN JOB_CONFIGURATION_LOCATION;

CREATE INDEX IF NOT EXISTS idx_job_name ON BATCH_JOB_INSTANCE(JOB_NAME);
CREATE INDEX IF NOT EXISTS idx_job_key ON BATCH_JOB_INSTANCE(JOB_KEY);

-- UPGRADE FOR PERFORMANCE OPTIMIZATION INDEXES

CREATE UNIQUE INDEX batch_job_execution_pkey ON credential.batch_job_execution USING btree (job_execution_id);
CREATE INDEX idx_job_exec_instance ON credential.batch_job_execution USING btree (job_instance_id);

CREATE UNIQUE INDEX batch_job_execution_context_pkey ON credential.batch_job_execution_context USING btree (job_execution_id);

CREATE UNIQUE INDEX batch_job_instance_pkey ON credential.batch_job_instance USING btree (job_instance_id);

CREATE UNIQUE INDEX batch_step_execution_pkey ON credential.batch_step_execution USING btree (step_execution_id);
CREATE INDEX idx_step_exec_jobid_stepname ON credential.batch_step_execution USING btree (job_execution_id, step_name);

CREATE UNIQUE INDEX batch_step_execution_context_pkey ON credential.batch_step_execution_context USING btree (step_execution_id);

CREATE INDEX IF NOT EXISTS idx_cred_new_status_cr_dtimes_active ON credential.credential_transaction (cr_dtimes) WHERE status_code = 'NEW' AND is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_cred_status_cr_dtimes_active ON credential.credential_transaction (status_code, cr_dtimes) WHERE is_deleted = false;
CREATE INDEX IF NOT EXISTS idx_cred_status_upd_dtimes_active ON credential.credential_transaction (status_code, upd_dtimes) WHERE is_deleted = false;
CREATE INDEX idx_credtran_status_crdtimes ON credential.credential_transaction USING btree (status_code, cr_dtimes);

-- END UPGRADE FOR PERFORMANCE OPTIMIZATION INDEXES
