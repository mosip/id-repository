package io.mosip.kernel.saltgenerator.listener.test;

import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;

import io.mosip.idrepository.credentialsfeeder.listener.BatchJobListener;

public class BatchJobListenerTest {

	BatchJobListener listener = new BatchJobListener();

	@Test
	public void testListener() {
		JobExecution jobExecution = new JobExecution(1l);
		jobExecution.setStatus(BatchStatus.COMPLETED);
		listener.beforeJob(jobExecution);
		listener.afterJob(jobExecution);
	}
}
