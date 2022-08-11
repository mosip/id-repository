package io.mosip.idrepository.credentialsfeeder.config;

import static io.mosip.idrepository.credentialsfeeder.constant.Constants.DEFAULT_CHUNCK_SIZE;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.IDREPO_CREDENTIAL_FEEDER_CHUNK_SIZE;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.MOSIP_IDREPO_IDENTITY_UIN_STATUS_REGISTERED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.mosip.idrepository.credentialsfeeder.entity.Uin;
import io.mosip.idrepository.credentialsfeeder.repository.UinRepo;
import io.mosip.idrepository.credentialsfeeder.step.CredentialsFeedingWriter;
import io.mosip.kernel.core.util.DateUtils;

/**
 * The Class CredentialsFeederJobConfig - provides configuration for Credentials
 * Feeder Job.
 *
 * @author Manoj SP
 */
@Configuration
@DependsOn({ "credentialsFeederConfig" })
public class CredentialsFeederJobConfig {

	@Value("${" + IDREPO_CREDENTIAL_FEEDER_CHUNK_SIZE + ":" + DEFAULT_CHUNCK_SIZE + "}")
	private int chunkSize;

	@Value("${" + MOSIP_IDREPO_IDENTITY_UIN_STATUS_REGISTERED + "}")
	private String uinActiveStatus;

	/**
	 * Job.
	 *
	 * @param step the step
	 * @return the job
	 */
	@Bean
	public Job job(Step step, JobBuilderFactory jobBuilderFactory, JobExecutionListener listener) {
		return jobBuilderFactory
				.get("job")
				.incrementer(new RunIdIncrementer())
				.listener(listener)
				.flow(step)
				.end()
				.build();
	}

	/**
	 * Step.
	 *
	 * @return the step
	 */
	@Bean
	public Step step(StepBuilderFactory stepBuilderFactory, CredentialsFeedingWriter writer, UinRepo uinRepo) {
		return stepBuilderFactory
				.get("step")
				.<Uin, Future<Uin>>chunk(chunkSize)
				.reader(credentialEventReader(uinRepo))
				.processor(asyncItemProcessor())
				.writer(asyncItemWriter(writer))
				.build();
	}

	/**
	 * This function reads the data from the database and returns the data in the
	 * form of a list of
	 * objects
	 * 
	 * @param uinRepo This is the repository that we are using to fetch the data.
	 * @return A list of Uin objects
	 */
	@Bean
	public ItemReader<Uin> credentialEventReader(UinRepo uinRepo) {
		RepositoryItemReader<Uin> reader = new RepositoryItemReader<>();
		reader.setRepository(uinRepo);
		reader.setMethodName("findByStatusCodeAndCreatedDateTimeBefore");
		reader.setArguments(List.of(uinActiveStatus, DateUtils.getUTCCurrentDateTime()));
		final Map<String, Sort.Direction> sorts = new HashMap<>();
		sorts.put("createdDateTime", Direction.ASC); // then try processing Least failed entries first
		reader.setSort(sorts);
		reader.setPageSize(chunkSize);
		return reader;
	}

	/**
	 * The function creates an AsyncItemProcessor that delegates to the same
	 * function that it is passed
	 * 
	 * @return An AsyncItemProcessor
	 */
	@Bean
	public <T> AsyncItemProcessor<T, T> asyncItemProcessor() {
		AsyncItemProcessor<T, T> asyncItemProcessor = new AsyncItemProcessor<>();
		asyncItemProcessor.setDelegate(elem -> elem);
		asyncItemProcessor.setTaskExecutor(taskExecutor());
		return asyncItemProcessor;
	}

	/**
	 * The function takes an ItemWriter and returns an AsyncItemWriter that wraps
	 * the ItemWriter
	 * 
	 * @param itemWriter The ItemWriter that will be wrapped by the AsyncItemWriter.
	 * @return An AsyncItemWriter object.
	 */
	public <T> AsyncItemWriter<T> asyncItemWriter(ItemWriter<T> itemWriter) {
		AsyncItemWriter<T> asyncItemWriter = new AsyncItemWriter<>();
		asyncItemWriter.setDelegate(itemWriter);
		return asyncItemWriter;
	}

	/**
	 * Task executor.
	 *
	 * @return the task executor
	 */
	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(chunkSize);
		executor.setMaxPoolSize(chunkSize);
		executor.setQueueCapacity(chunkSize);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setThreadNamePrefix("credential-feeder-");
		return executor;
	}
}
