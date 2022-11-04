package io.mosip.credential.request.generator.batch.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.QueryHint;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.credential.request.generator.util.RestUtil;



/**
 * The Class BatchConfiguration.
 *
 * @author Sowmya
 */
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
	
	@Autowired
	public CredentialItemTasklet credentialItemTasklet;

	@Autowired
	public CredentialItemReprocessTasklet credentialItemReprocessTasklet;

	/** The job builder factory. */
	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	/** The step builder factory. */
	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	/** The job launcher. */
	@Autowired
	private JobLauncher jobLauncher;

	/** The crdential repo. */
	@Autowired
	private CredentialRepositary<CredentialEntity, String> crdentialRepo;

	/** The credential process job. */
	@Autowired
	@Qualifier("credentialProcessJob")
	private Job credentialProcessJob;

	/** The credential re process job. */
	@Autowired
	private Job credentialReProcessJob;

	private static final String BATCH_CONFIGURATION = "BatchConfiguration";
	private static final Logger LOGGER = IdRepoLogger.getLogger(BatchConfiguration.class);
	
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
			LOGGER.error(IdRepoSecurityManager.getUser(), BATCH_CONFIGURATION,
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
			LOGGER.error(IdRepoSecurityManager.getUser(), BATCH_CONFIGURATION,
					"error in JobLauncher " + ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Credential process job.
	 *
	 * @param listener the listener
	 * @return the job
	 */
	@Bean
	public Job credentialProcessJob(JobCompletionNotificationListener listener) throws Exception {
		return jobBuilderFactory.get("credentialProcessJob").incrementer(new RunIdIncrementer()).listener(listener)
				.flow(credentialProcessStep()).end().build();
	}

	/**
	 * Credential re process job.
	 *
	 * @param listener the listener
	 * @return the job
	 */
	@Bean
	public Job credentialReProcessJob(JobCompletionNotificationListener listener) throws Exception {
		return jobBuilderFactory.get("credentialReProcessJob").incrementer(new RunIdIncrementer()).listener(listener)
				.flow(credentialReProcessStep()).end().build();
	}
	
	@Bean
	@DependsOn("alterAnnotation")
	public Step credentialProcessStep() {
		return stepBuilderFactory.get("credentialProcessJob").tasklet(credentialItemTasklet).build();

	}
	
	@Bean
	@DependsOn("alterAnnotation")
	public Step credentialReProcessStep() throws Exception {
		return stepBuilderFactory.get("credentialProcessJob").tasklet(credentialItemReprocessTasklet).build();

	}
	
	/**
	 * Gets the rest util.
	 *
	 * @return the rest util
	 */
	@Bean
	public RestUtil getRestUtil() {
		return new RestUtil();
	}

	/**
	 * Gets the task scheduler.
	 *
	 * @return the task scheduler
	 */
	@Bean
	public ThreadPoolTaskScheduler getTaskScheduler() {
		return new ThreadPoolTaskScheduler();
	}

	/**
	 * Credential re process step.
	 *
	 * @return the step
	 */
	

	@Bean
	public PropertyLoader propertyLoader() {
		return new PropertyLoader();
	}



	@Bean(name = "alterAnnotation")
	public String alterAnnotation() throws Exception {

		Method findCredentialByStatusCode = CredentialRepositary.class.getDeclaredMethod("findCredentialByStatusCode",
				String.class, Pageable.class);
		findCredentialByStatusCode.setAccessible(true);
		QueryHints queryHints = findCredentialByStatusCode.getDeclaredAnnotation(QueryHints.class);
		QueryHint queryHint = (QueryHint) queryHints.value()[0];
		java.lang.reflect.InvocationHandler invocationHandler = Proxy.getInvocationHandler(queryHint);
		Field memberValues = invocationHandler.getClass().getDeclaredField("memberValues");
		memberValues.setAccessible(true);
		Map<String, Object> values = (Map<String, Object>) memberValues.get(invocationHandler);
		values.put("value", propertyLoader().processLockTimeout);
		findCredentialByStatusCode.setAccessible(false);

		Method findCredentialByStatusCodes = CredentialRepositary.class.getDeclaredMethod("findCredentialByStatusCodes",
				String[].class,Pageable.class);
		findCredentialByStatusCodes.setAccessible(true);
		QueryHints queryHintsReprocess = findCredentialByStatusCodes.getDeclaredAnnotation(QueryHints.class);
		QueryHint queryHintReprocess = (QueryHint) queryHintsReprocess.value()[0];
		java.lang.reflect.InvocationHandler invocationHandlerReprocess = Proxy.getInvocationHandler(queryHintReprocess);
		Field memberValuesReprocess = invocationHandlerReprocess.getClass().getDeclaredField("memberValues");
		memberValuesReprocess.setAccessible(true);
		Map<String, Object> valuesReprocess = (Map<String, Object>) memberValuesReprocess
				.get(invocationHandlerReprocess);

		valuesReprocess.put("value", propertyLoader().reProcessLockTimeout);
		findCredentialByStatusCodes.setAccessible(false);

		return "";

	}
	
	@Bean
	public AfterburnerModule afterburnerModule() {
		return new AfterburnerModule();
	}
}
