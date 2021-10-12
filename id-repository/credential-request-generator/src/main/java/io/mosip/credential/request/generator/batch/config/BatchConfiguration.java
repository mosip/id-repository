package io.mosip.credential.request.generator.batch.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.credential.request.generator.util.RestUtil;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;


/**
 * The Class BatchConfiguration.
 *
 * @author Sowmya
 */
@Configuration
@EnableBatchProcessing
@Import({ HibernateDaoConfig.class })
@EnableJpaRepositories(basePackages = "io.mosip.*", repositoryBaseClass = HibernateRepositoryImpl.class)
public class BatchConfiguration {

	@Value("${credential.batch.page.size:100}")
	private int pageSize;

	@Value("${credential.batch.chunk.size:100}")
	private int chunkSize;

	/** The job builder factory. */
	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	/** The step builder factory. */
	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	private JobLauncher jobLauncher;

	/** The crdential repo. */
	@Autowired
	private CredentialRepositary<CredentialEntity, String> crdentialRepo;

	@Autowired
	private Job credentialProcessJob;


	@Scheduled(fixedRateString = "${mosip.credential.request.job.timeintervel}")
	public void printMessage() {
		try {
			JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
					.toJobParameters();
			jobLauncher.run(credentialProcessJob, jobParameters);

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
	 * Credential process job.
	 *
	 * @param listener the listener
	 * @return the job
	 */
	@Bean
	public Job credentialProcessJob(JobCompletionNotificationListener listener) {
		return jobBuilderFactory.get("credentialProcessJob").incrementer(new RunIdIncrementer()).listener(listener)
				.flow(credentialProcessStep()).end().build();
	}

	/**
	 * Credential process step.
	 *
	 * @return the step
	 */
	@Bean
	public Step credentialProcessStep() {
		RepositoryItemReader<CredentialEntity> reader = new RepositoryItemReader<>();
		List<Object> methodArgs = new ArrayList<Object>();
		reader.setRepository(crdentialRepo);
		reader.setMethodName("findCredentialByStatusCode");
		 final Map<String, Sort.Direction> sorts = new HashMap<>();
		    sorts.put("createDateTime", Direction.ASC);
		methodArgs.add("NEW");
		reader.setArguments(methodArgs);
		reader.setSort(sorts);
		reader.setPageSize(pageSize);
		reader.setMaxItemCount(pageSize);

		RepositoryItemWriter<CredentialEntity> writer = new RepositoryItemWriter<>();
		writer.setRepository(crdentialRepo);
		writer.setMethodName("update");
		return stepBuilderFactory.get("credentialProcessStep").<CredentialEntity, CredentialEntity>chunk(chunkSize)
				.reader(reader)
				.processor(processor())
				.writer(writer).build();

	}

	@Bean
	public RestUtil getRestUtil() {
		return new RestUtil();
	}

	@Bean
	public ThreadPoolTaskScheduler getTaskScheduler() {
		return new ThreadPoolTaskScheduler();
	}

}
