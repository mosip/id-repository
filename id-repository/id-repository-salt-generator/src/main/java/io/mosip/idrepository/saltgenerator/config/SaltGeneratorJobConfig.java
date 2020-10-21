package io.mosip.idrepository.saltgenerator.config;

import static io.mosip.idrepository.saltgenerator.constant.SaltGeneratorConstant.CHUNK_SIZE;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

import io.mosip.idrepository.saltgenerator.entity.IdRepoSaltEntitiesComposite;

/**
 * The Class SaltGeneratorJobConfig - provides configuration for Salt generator Job.
 *
 * @author Manoj SP
 */
@Configuration
@DependsOn("saltGeneratorConfig")
public class SaltGeneratorJobConfig {
	
	@Autowired
	private Environment env;

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
	private ItemReader<IdRepoSaltEntitiesComposite> reader;
	
	/** The writer. */
	@Autowired
	private ItemWriter<IdRepoSaltEntitiesComposite> writer;
	
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
				.<IdRepoSaltEntitiesComposite, IdRepoSaltEntitiesComposite> chunk(env.getProperty(CHUNK_SIZE.getValue(), Integer.class))
				.reader(reader)
				.writer(writer)
				.build();
	}
}
