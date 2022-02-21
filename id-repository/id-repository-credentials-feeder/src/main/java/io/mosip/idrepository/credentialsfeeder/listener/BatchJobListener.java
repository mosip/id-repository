package io.mosip.idrepository.credentialsfeeder.listener;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.credentialsfeeder.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * The listener interface for receiving batchJob events.
 * The class that is interested in processing a batchJob
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's addBatchJobListener method. When
 * the batchJob event occurs, that object's appropriate
 * method is invoked.
 *
 * @author Manoj SP
 */
@Component
public class BatchJobListener extends JobExecutionListenerSupport {

	private static final String CREDENTIALS_FEEDER = "CREDENTIALS_FEEDER";
	/** The mosip logger. */
	Logger mosipLogger = IdRepoLogger.getLogger(BatchJobListener.class);

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.listener.JobExecutionListenerSupport#beforeJob(org.springframework.batch.core.JobExecution)
	 */
	@Override
	public void beforeJob(JobExecution jobExecution) {
		mosipLogger.debug(CREDENTIALS_FEEDER, "BatchJobListener", "BATCH JOB STARTED WITH STATUS : ",
				jobExecution.getStatus().name());
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.listener.JobExecutionListenerSupport#afterJob(org.springframework.batch.core.JobExecution)
	 */
	@Override
	public void afterJob(JobExecution jobExecution) {
		mosipLogger.debug(CREDENTIALS_FEEDER, "BatchJobListener", "BATCH JOB COMPLETED WITH STATUS : ",
				jobExecution.getStatus().name());
		if (!jobExecution.getStatus().equals(BatchStatus.COMPLETED)) {
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.JOB_FAILED);
		}
		jobExecution.setExitStatus(ExitStatus.COMPLETED);
	}
}
