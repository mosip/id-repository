package io.mosip.credential.request.generator.batch.config;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {



	@Override
	public void afterJob(JobExecution jobExecution) {
		  if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			// TODO add log
		}
	}

}
