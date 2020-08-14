package io.mosip.credential.request.generator.batch.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;


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

	/** The crdential repo. */
	@Autowired
	private CredentialRepositary<CredentialEntity, String> crdentialRepo;

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
		// TODO based on only NEW status code or any other 
		reader.setArguments(methodArgs);
		reader.setSort(sorts);
		reader.setPageSize(10);
	
		RepositoryItemWriter<CredentialEntity> writer = new RepositoryItemWriter<>();
		writer.setRepository(crdentialRepo);
		writer.setMethodName("update");
		return stepBuilderFactory.get("credentialProcessStep").<CredentialEntity, CredentialEntity>chunk(10)
				.reader(reader)
				.processor(processor())
				.writer(writer).build();

	}

}
