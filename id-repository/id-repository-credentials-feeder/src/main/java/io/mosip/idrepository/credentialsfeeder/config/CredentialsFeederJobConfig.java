package io.mosip.idrepository.credentialsfeeder.config;

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

import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.repository.CredentialRequestStatusRepo;
import io.mosip.idrepository.credentialsfeeder.step.CredentialsFeedingWriter;
import io.mosip.kernel.core.util.DateUtils;

/**
 * The Class CredentialsFeederJobConfig - provides configuration for Credentials Feeder Job.
 *
 * @author Manoj SP
 */
@Configuration
@DependsOn({"credentialsFeederConfig"})
public class CredentialsFeederJobConfig {
	
	private static final int DEFAULT_CHUNCK_SIZE = 10;

	private static final String STATUS_REQUESTED = "REQUESTED";

	private static final String IDREPO_CREDENTIAL_FEEDER_CHUNK_SIZE = "idrepo-credential-feeder-chunk-size";

	@Value("${" + IDREPO_CREDENTIAL_FEEDER_CHUNK_SIZE + ":" + DEFAULT_CHUNCK_SIZE + "}")
	private int chunkSize;
	
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
	public Step step(StepBuilderFactory stepBuilderFactory, CredentialsFeedingWriter writer, CredentialRequestStatusRepo credentialRequestStatusRepo) {
		return stepBuilderFactory
				.get("step")
				.<CredentialRequestStatus, Future<CredentialRequestStatus>> chunk(chunkSize)
				.reader(credentialEventReader(credentialRequestStatusRepo))
				.processor(asyncItemProcessor())
				.writer(asyncItemWriter(writer))
				.build();
	}
	
	@Bean
	public ItemReader<CredentialRequestStatus> credentialEventReader(CredentialRequestStatusRepo credentialRequestStatusRepo) {
		RepositoryItemReader<CredentialRequestStatus> reader = new RepositoryItemReader<>();
		reader.setRepository(credentialRequestStatusRepo);
		reader.setMethodName("findByRequestedStatusBeforeCrDtimes");
		reader.setArguments(List.of(DateUtils.getUTCCurrentDateTime(), STATUS_REQUESTED));
		final Map<String, Sort.Direction> sorts = new HashMap<>();
		    sorts.put("crDTimes", Direction.ASC); // then try processing Least failed entries first
		reader.setSort(sorts);
		reader.setPageSize(chunkSize);
		return reader;
	}
	
	@Bean
	public <T> AsyncItemProcessor<T, T> asyncItemProcessor() {
		AsyncItemProcessor<T, T> asyncItemProcessor = new AsyncItemProcessor<>();
		    asyncItemProcessor.setDelegate(elem -> elem);
		    asyncItemProcessor.setTaskExecutor(taskExecutor());
		return asyncItemProcessor;
	}
	
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
        executor.setThreadNamePrefix("MultiThreaded-");
        return executor;
    }
}
