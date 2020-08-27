package io.mosip.credential.request.generator.batch.config;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

import io.mosip.credential.request.generator.constants.LoggerFileConstant;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;


/**
 * The listener interface for receiving jobCompletionNotification events.
 * The class that is interested in processing a jobCompletionNotification
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addJobCompletionNotificationListener<code> method. When
 * the jobCompletionNotification event occurs, that object's appropriate
 * method is invoked.
 *
 * @author Sowmya
 */
@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

	private static final Logger LOGGER = IdRepoLogger
			.getLogger(JobCompletionNotificationListener.class);

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.listener.JobExecutionListenerSupport#afterJob(org.springframework.batch.core.JobExecution)
	 */
	@Override
	public void afterJob(JobExecution jobExecution) {
		  if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.ID.toString(),
					LoggerFileConstant.ID.toString(), "Job completed successfully");
		}
	}

}
