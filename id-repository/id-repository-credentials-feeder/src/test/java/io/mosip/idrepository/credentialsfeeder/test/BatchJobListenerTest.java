package io.mosip.idrepository.credentialsfeeder.test;

import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;

import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.credentialsfeeder.listener.BatchJobListener;

/**
 * @author Manoj SP
 *
 */
public class BatchJobListenerTest {

	BatchJobListener listener = new BatchJobListener();

	@Test
	public void testListener() {
		JobExecution jobExecution = new JobExecution(1l);
		jobExecution.setStatus(BatchStatus.COMPLETED);
		listener.beforeJob(jobExecution);
		listener.afterJob(jobExecution);
	}

	@Test(expected = IdRepoAppUncheckedException.class)
	public void testListenerJobFailed() {
		JobExecution jobExecution = new JobExecution(1l);
		jobExecution.setStatus(BatchStatus.FAILED);
		listener.afterJob(jobExecution);
	}
}
