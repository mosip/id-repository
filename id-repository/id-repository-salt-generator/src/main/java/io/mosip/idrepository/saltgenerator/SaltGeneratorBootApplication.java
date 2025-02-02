package io.mosip.idrepository.saltgenerator;

import io.mosip.idrepository.saltgenerator.logger.SaltGeneratorLogger;
import io.mosip.idrepository.saltgenerator.service.SaltGenerator;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * The Class SaltGeneratorBootApplication - Salt generator Job is a
 * one-time job which populates salts for hashing and encrypting data.
 *
 * @author Manoj SP
 */
@ComponentScan(basePackages={"io.mosip.idrepository.saltgenerator.*"})
@SpringBootApplication(exclude = {BatchAutoConfiguration.class})
public class SaltGeneratorBootApplication implements CommandLineRunner {

	Logger mosipLogger = SaltGeneratorLogger.getLogger(SaltGeneratorBootApplication.class);

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
		mosipLogger.info(" started......");
		saltGenerator.start();
		mosipLogger.info("  Completed......");

	}

}
