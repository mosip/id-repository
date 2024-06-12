package io.mosip.credential.request.generator.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * @author Neha Farheen The Class RestUtil.
 */

@Configuration
public class CredentialScheduleJobConfiguration {

	/** The job launcher. */
	@Autowired
	private JobLauncher jobLauncher;

	/** The credential process job. */
	@Autowired
	private Job credentialProcessJob;

	/** The credential re process job. */
	@Autowired
	private Job credentialReProcessJob;

	private static final String CREDENTIAL_SCHEDULE_CONFIGURATION = "CredentialJobConfiguration";
	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialScheduleJobConfiguration.class);

	/**
	 * Process job.
	 */
	@Scheduled(fixedDelayString = "${mosip.credential.request.job.timedelay}")
	public void processJob() {
		try {
			JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
					.toJobParameters();
			jobLauncher.run(credentialProcessJob, jobParameters);

		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SCHEDULE_CONFIGURATION,
					"error in JobLauncher " + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Re process job.
	 */
	@Scheduled(fixedDelayString = "${mosip.credential.request.reprocess.job.timedelay}")
	public void reProcessJob() {
		try {
			JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
					.toJobParameters();
			jobLauncher.run(credentialReProcessJob, jobParameters);

		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_SCHEDULE_CONFIGURATION,
					"error in JobLauncher " + ExceptionUtils.getStackTrace(e));
		}
	}

	

}
