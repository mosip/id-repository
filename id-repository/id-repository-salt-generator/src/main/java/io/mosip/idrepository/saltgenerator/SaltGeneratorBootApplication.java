package io.mosip.idrepository.saltgenerator;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.scheduling.ScheduledTasksEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * The Class SaltGeneratorBootApplication - Salt generator Job is a
 * one-time job which populates salts for hashing and encrypting data.
 *
 * @author Manoj SP
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude={ManagementWebSecurityAutoConfiguration.class,SecurityAutoConfiguration.class,ScheduledTasksEndpointAutoConfiguration.class, JmxAutoConfiguration.class})
@ComponentScan(value = {"io.mosip.idrepository.saltgenerator.*"})
public class SaltGeneratorBootApplication {

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

}
