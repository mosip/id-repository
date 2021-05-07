package io.mosip.idrepository.credentialsfeeder;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.scheduling.ScheduledTasksEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * The Class CredentialsFeederApplication - Salt generator Job is a
 * one-time job which populates salts for hashing and encrypting data.
 *
 * @author Manoj SP
 */
@SpringBootApplication
@EnableBatchProcessing
@EnableAutoConfiguration(exclude={ScheduledTasksEndpointAutoConfiguration.class})  
public class CredentialsFeederApplication {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		ApplicationContext applicationContext = SpringApplication.run(CredentialsFeederApplication.class,
				args);
		SpringApplication.exit(applicationContext);
	}

}
