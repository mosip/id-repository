package io.mosip.credential.request.generator.batch.config;

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

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;



@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;
	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	private CredentialRepositary<CredentialEntity, String> crdentialRepo;

	@Bean
	public CredentialItemProcessor processor() {
		return new CredentialItemProcessor();
	}

	@Bean
	public Job credentialProcessJob(JobCompletionNotificationListener listener) {
		return jobBuilderFactory.get("credentialProcessJob").incrementer(new RunIdIncrementer()).listener(listener)
				.flow(credentialProcessStep()).end().build();
	}

	@Bean
	public Step credentialProcessStep() {
		RepositoryItemReader<CredentialEntity> reader = new RepositoryItemReader<>();
		reader.setRepository(crdentialRepo);
		reader.setMethodName("findByStatusCode");
		// TODO based on only NEW status code or any other 
		RepositoryItemWriter<CredentialEntity> writer = new RepositoryItemWriter<>();
		//update the entity after processing
		writer.setRepository(crdentialRepo);
		writer.setMethodName("update");
		return stepBuilderFactory.get("credentialProcessStep").<CredentialEntity, CredentialEntity>chunk(10)
				.reader(reader)
				.processor(processor())
				.writer(writer).build();
		// need to set page size
	}

}
