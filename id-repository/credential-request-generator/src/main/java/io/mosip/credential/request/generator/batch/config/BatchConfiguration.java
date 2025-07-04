package io.mosip.credential.request.generator.batch.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.credential.request.generator.util.RestUtil;
import jakarta.persistence.QueryHint;

/**
 * The Class BatchConfiguration.
 *
 */
@Configuration
public class BatchConfiguration {

	@Autowired
	public CredentialItemTasklet credentialItemTasklet;

	@Autowired
	public CredentialItemReprocessTasklet credentialItemReprocessTasklet;

	/**
	 * Credential process job.
	 *
	 * @param listener the listener
	 * @return the job
	 */
	@Bean
	public Job credentialProcessJob(JobRepository jobRepository, JobCompletionNotificationListener listener,PlatformTransactionManager transactionManager) {
		return new JobBuilder("credentialProcessJob", jobRepository).incrementer(new RunIdIncrementer())
				.listener(listener).flow(credentialProcessStep(jobRepository,transactionManager)).end().build();
	}

	/**
	 * Credential re process job.
	 *
	 * @param listener the listener
	 * @return the job
	 * @throws Exception
	 */

	@Bean
	public Job credentialReProcessJob(JobRepository jobRepository, JobCompletionNotificationListener listener,PlatformTransactionManager transactionManager)
			throws Exception {
		return new JobBuilder("credentialReProcessJob", jobRepository).incrementer(new RunIdIncrementer())
				.listener(listener).flow(credentialReProcessStep(jobRepository,transactionManager)).end().build();
	}

	@Bean
	@DependsOn("alterAnnotation")
	public Step credentialProcessStep(JobRepository jobRepository,PlatformTransactionManager transactionManager) {
		return new StepBuilder("credentialProcessJob", jobRepository).tasklet(credentialItemTasklet, null).transactionManager(transactionManager).build();
	}

	@Bean
	@DependsOn("alterAnnotation")
	public Step credentialReProcessStep(JobRepository jobRepository,PlatformTransactionManager transactionManager) {
		Step step = new StepBuilder("credentialReProcessJob", jobRepository).tasklet(credentialItemReprocessTasklet, null).transactionManager(transactionManager)
				.build();
		return step;
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
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(queryHint);
		Field memberValues = invocationHandler.getClass().getDeclaredField("memberValues");
		memberValues.setAccessible(true);
		Map<String, Object> values = (Map<String, Object>) memberValues.get(invocationHandler);
		values.put("value", propertyLoader().processLockTimeout);
		findCredentialByStatusCode.setAccessible(false);

		Method findCredentialByStatusCodes = CredentialRepositary.class.getDeclaredMethod("findCredentialByStatusCodes",
				String[].class, Pageable.class);
		findCredentialByStatusCodes.setAccessible(true);
		QueryHints queryHintsReprocess = findCredentialByStatusCodes.getDeclaredAnnotation(QueryHints.class);
		QueryHint queryHintReprocess = (QueryHint) queryHintsReprocess.value()[0];
		InvocationHandler invocationHandlerReprocess = Proxy.getInvocationHandler(queryHintReprocess);
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
