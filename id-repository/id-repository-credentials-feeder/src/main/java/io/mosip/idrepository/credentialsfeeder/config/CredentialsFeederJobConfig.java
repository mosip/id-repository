package io.mosip.idrepository.credentialsfeeder.config;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.mosip.idrepository.credentialsfeeder.entity.idrepo.CredentialRequestEntity;

/**
 * The Class CredentialsFeederJobConfig - provides configuration for Credentials Feeder Job.
 *
 * @author Manoj SP
 */
@Configuration
@DependsOn("credentialsFeederConfig")
public class CredentialsFeederJobConfig {
	
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
	
	/** The reader. */
	@Autowired
	private ItemReader<CredentialRequestEntity> reader;
	
	/** The writer. */
	@Autowired
	private ItemWriter<CredentialRequestEntity> writer;

	@Value("${" + IDREPO_CREDENTIAL_FEEDER_CHUNK_SIZE + ":10}")
	private int credentialFeederChunkSize;
	
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
				.<CredentialRequestEntity, Future<CredentialRequestEntity>> chunk(credentialFeederChunkSize)
				.reader(reader)
				.processor(asyncItemProcessor())
				.writer(asyncItemWriter(writer))
				.build();
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
        executor.setCorePoolSize(credentialFeederChunkSize);
        executor.setMaxPoolSize(credentialFeederChunkSize);
        executor.setQueueCapacity(credentialFeederChunkSize);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("MultiThreaded-");
        return executor;
    }
}
