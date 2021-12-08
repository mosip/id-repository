package io.mosip.credential.request.generator.batch.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.QueryHint;

import io.mosip.credential.request.generator.entity.CredentialFailedEntity;
import io.mosip.credential.request.generator.repositary.CredentialFailedRepository;
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

	/** The crdential repo. */
	@Autowired
	private CredentialFailedRepository<CredentialFailedEntity, String> crdentialFailedRepo;

	/** The credential process job. */
	@Autowired
	private Job credentialProcessJob;

	/** The credential re process job. */
	@Autowired
	private Job credentialReProcessJob;



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
			e.printStackTrace();
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
			e.printStackTrace();
		}
	}
	/**
	 * Processor.
	 *
	 * @return the credential item processor
	 */
	@Bean
	public CredentialItemProcessor processor() {
		return new CredentialItemProcessor();
	}

	/**
	 * Re processor.
	 *
	 * @return the credential item re processor
	 */
	@Bean
	public CredentialItemReProcessor reProcessor() {
		return new CredentialItemReProcessor();
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
	/**
	 * Credential process step.
	 *
	 * @return the step
	 */
	@Bean
	@DependsOn("alterAnnotation")
	public Step credentialProcessStep() throws Exception {
		RepositoryItemReader<CredentialEntity> reader = new RepositoryItemReader<>();
		List<Object> methodArgs = new ArrayList<Object>();
		reader.setRepository(crdentialRepo);
		reader.setMethodName("findCredential");
		 final Map<String, Sort.Direction> sorts = new HashMap<>();
		    sorts.put("createDateTime", Direction.ASC);
		reader.setArguments(methodArgs);
		reader.setSort(sorts);
		reader.setPageSize(propertyLoader().pageSize);
	
		RepositoryItemWriter<CredentialEntity> writer = new RepositoryItemWriter<>();
		writer.setRepository(crdentialRepo);
		writer.setMethodName("update");
		return stepBuilderFactory.get("credentialProcessStep")
				.<CredentialEntity, CredentialEntity>chunk(propertyLoader().chunkSize)
				.reader(reader).processor((ItemProcessor) asyncItemProcessor()).writer(asyncItemWriter()).build();

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
	@DependsOn("alterAnnotation")
	public Step credentialReProcessStep() throws Exception {
		RepositoryItemReader<CredentialFailedEntity> reader = new RepositoryItemReader<>();
		List<Object> methodArgs = new ArrayList<Object>();
		reader.setRepository(crdentialFailedRepo);
		reader.setMethodName("findFailedCredential");
		final Map<String, Sort.Direction> sorts = new HashMap<>();
		sorts.put("updateDateTime", Direction.ASC);
		methodArgs.add(propertyLoader().credentialRequestType);
		reader.setArguments(methodArgs);
		reader.setSort(sorts);
		reader.setPageSize(propertyLoader().pageSize);

		RepositoryItemWriter<CredentialFailedEntity> writer = new RepositoryItemWriter<>();
		writer.setRepository(crdentialFailedRepo);
		writer.setMethodName("add");
		return stepBuilderFactory.get("credentialReProcessStep")
				.<CredentialFailedEntity, CredentialFailedEntity>chunk(propertyLoader().chunkSize)
				.reader(reader).processor((ItemProcessor) asyncItemReProcessor()).writer(asyncItemWReprocessWriter())
				.build();

	}

	@Bean
	public PropertyLoader propertyLoader() {
		return new PropertyLoader();
	}

	@Bean
	public AsyncItemProcessor<CredentialEntity, CredentialEntity> asyncItemProcessor() throws Exception {

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(propertyLoader().corePoolSize);
		executor.setMaxPoolSize(propertyLoader().maxPoolSize);
		executor.setQueueCapacity(propertyLoader().queueCapacity);
		executor.setThreadNamePrefix("CredentialProcessing-");
		executor.afterPropertiesSet();

		AsyncItemProcessor<CredentialEntity, CredentialEntity> asyncProcessor = new AsyncItemProcessor<>();
		asyncProcessor.setDelegate(processor());
		asyncProcessor.setTaskExecutor(executor);
		asyncProcessor.afterPropertiesSet();

		return asyncProcessor;
	}

	@Bean
	public AsyncItemProcessor<CredentialFailedEntity, CredentialFailedEntity> asyncItemReProcessor() throws Exception {

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(propertyLoader().corePoolSize);
		executor.setMaxPoolSize(propertyLoader().maxPoolSize);
		executor.setQueueCapacity(propertyLoader().queueCapacity);
		executor.setThreadNamePrefix("CredentialReProcessing-");
		executor.afterPropertiesSet();

		AsyncItemProcessor<CredentialFailedEntity, CredentialFailedEntity> asyncProcessor = new AsyncItemProcessor<>();
		asyncProcessor.setDelegate(reProcessor());
		asyncProcessor.setTaskExecutor(executor);
		asyncProcessor.afterPropertiesSet();

		return asyncProcessor;
	}

	@Bean
	public AsyncItemWriter<CredentialEntity> asyncItemWriter() {
		AsyncItemWriter<CredentialEntity> asyncWriter = new AsyncItemWriter<>();
		asyncWriter.setDelegate(processwriter());

		return asyncWriter;
	}

	@Bean
	public AsyncItemWriter<CredentialFailedEntity> asyncItemWReprocessWriter() {
		AsyncItemWriter<CredentialFailedEntity> asyncWriter = new AsyncItemWriter<>();
		asyncWriter.setDelegate(reProcesswriter());

		return asyncWriter;
	}

	@Bean
	public RepositoryItemWriter<CredentialEntity> processwriter() {
		RepositoryItemWriter<CredentialEntity> writer = new RepositoryItemWriter<>();
		writer.setRepository(crdentialRepo);
		writer.setMethodName("update");
		return writer;
	}

	@Bean
	public RepositoryItemWriter<CredentialFailedEntity> reProcesswriter() {
		RepositoryItemWriter<CredentialFailedEntity> writer = new RepositoryItemWriter<>();
		writer.setRepository(crdentialFailedRepo);
		writer.setMethodName("update");
		return writer;
	}

	@Bean(name = "alterAnnotation")
	public String alterAnnotation() throws Exception {

		Method findCredential = CredentialRepositary.class.getDeclaredMethod("findCredential", Pageable.class);
		findCredential.setAccessible(true);
		QueryHints queryHints = findCredential.getDeclaredAnnotation(QueryHints.class);
		QueryHint queryHint = (QueryHint) queryHints.value()[0];
		java.lang.reflect.InvocationHandler invocationHandler = Proxy.getInvocationHandler(queryHint);
		Field memberValues = invocationHandler.getClass().getDeclaredField("memberValues");
		memberValues.setAccessible(true);
		Map<String, Object> values = (Map<String, Object>) memberValues.get(invocationHandler);
		values.put("value", propertyLoader().processLockTimeout);
		findCredential.setAccessible(false);

		Method findFailedCredential = CredentialFailedRepository.class.getDeclaredMethod("findFailedCredential",
				String.class, Pageable.class);
		findFailedCredential.setAccessible(true);
		QueryHints queryHintsFailed = findFailedCredential.getDeclaredAnnotation(QueryHints.class);
		QueryHint queryHintFailed = (QueryHint) queryHintsFailed.value()[0];
		java.lang.reflect.InvocationHandler invocationHandlerFailed = Proxy.getInvocationHandler(queryHintFailed);
		Field memberValuesFailed = invocationHandlerFailed.getClass().getDeclaredField("memberValues");
		memberValuesFailed.setAccessible(true);
		Map<String, Object> valuesFailed = (Map<String, Object>) memberValuesFailed.get(invocationHandlerFailed);
		valuesFailed.put("value", propertyLoader().processLockTimeout);
		findFailedCredential.setAccessible(false);

		return "";

	}
}
