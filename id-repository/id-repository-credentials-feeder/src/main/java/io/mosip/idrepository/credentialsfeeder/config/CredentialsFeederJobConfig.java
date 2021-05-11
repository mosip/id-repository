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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.mosip.idrepository.credentialsfeeder.entity.CredentialRequestStatusEntity;
import io.mosip.idrepository.credentialsfeeder.repository.CredentialRequestStatusRepository;
import io.mosip.idrepository.credentialsfeeder.step.CredentialsFeedingWriter;

/**
 * The Class CredentialsFeederJobConfig - provides configuration for Credentials Feeder Job.
 *
 * @author Manoj SP
 */
@Configuration
@DependsOn("credentialsFeederConfig")
public class CredentialsFeederJobConfig {
	
	private static final String STATUS_REQUESTED = "REQUESTED";

	private static final String IDREPO_CREDENTIAL_FEEDER_CHUNK_SIZE = "idrepo-credential-feeder-chunk-size";

	/** The job builder factory. */
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	/** The step builder factory. */
	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	/** The listener. */
	@Autowired
	private JobExecutionListener listener;
	
	/** The writer. */
	@Autowired
	private CredentialsFeedingWriter writer;

	@Value("${" + IDREPO_CREDENTIAL_FEEDER_CHUNK_SIZE + ":10}")
	private int chunkSize;
	
	@Autowired
	private CredentialRequestStatusRepository credentialRequestStatusRepository;
	
	/**
	 * Job.
	 *
	 * @param step the step
	 * @return the job
	 */
	@Bean
	public Job job(Step step) {
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
	public Step step() {
		return stepBuilderFactory
				.get("step")
				.<CredentialRequestStatusEntity, Future<CredentialRequestStatusEntity>> chunk(chunkSize)
				.reader(credentialEventReader())
				.processor(asyncItemProcessor())
				.writer(asyncItemWriter(writer))
				.build();
	}
	
	@Bean
	public ItemReader<CredentialRequestStatusEntity> credentialEventReader() {
		RepositoryItemReader<CredentialRequestStatusEntity> reader = new RepositoryItemReader<>();
		reader.setRepository(credentialRequestStatusRepository);
		reader.setMethodName("findByRequestedStatusOrderByCrdtimes");
		reader.setArguments(List.of(STATUS_REQUESTED));
		final Map<String, Sort.Direction> sorts = new HashMap<>();
		    sorts.put("createDtimes", Direction.ASC); // then try processing Least failed entries first
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
