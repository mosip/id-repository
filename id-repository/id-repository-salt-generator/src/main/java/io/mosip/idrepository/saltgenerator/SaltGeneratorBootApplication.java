package io.mosip.idrepository.saltgenerator;

import io.mosip.idrepository.saltgenerator.service.SaltGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.context.ApplicationContext;

/**
 * The Class SaltGeneratorBootApplication - Salt generator Job is a
 * one-time job which populates salts for hashing and encrypting data.
 *
 * @author Manoj SP
 */
//@EnableAutoConfiguration(exclude={ScheduledTasksEndpointAutoConfiguration.class})
@SpringBootApplication(exclude = {BatchAutoConfiguration.class})
public class SaltGeneratorBootApplication implements CommandLineRunner {

	@Autowired
	private SaltGenerator saltGenerator;

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		ApplicationContext applicationContext = SpringApplication.run(SaltGeneratorBootApplication.class,
				args);
		SpringApplication.exit(applicationContext);
	}

	@Override
	public void run(String... args) throws Exception {

		//logger.info(" started......");
		saltGenerator.start();
		//logger.info("  Completed......");

	}

}
