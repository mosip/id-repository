package io.mosip.idrepository.saltgenerator;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.saltgenerator.service.SaltGenerator;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * {@link CommandLineRunner} that invokes the salt batch exactly once at application startup.
 *
 * <p>
 * After {@link #run(String...)} returns, {@link SaltGeneratorBootApplication#main(String[])}
 * calls {@code SpringApplication.exit} so the Job pod terminates cleanly.
 * </p>
 *
 * <h2>Failure behaviour</h2>
 * Any exception from {@link SaltGenerator#start()} propagates out of {@code run}, causing a
 * non-zero Spring exit code and a failed K8s Job. Re-run is safe because writes are idempotent
 * ({@code ON CONFLICT DO NOTHING} + resume from max id).
 *
 * @author MOSIP
 * @see SaltGenerator
 * @see SaltGeneratorBootApplication
 */
@Component
public class SaltGeneratorRunner implements CommandLineRunner {

	private static final Logger mosipLogger = IdRepoLogger.getLogger(SaltGeneratorRunner.class);

	private final SaltGenerator saltGenerator;

	/**
	 * @param saltGenerator batch orchestrator that generates and persists salt rows
	 */
	public SaltGeneratorRunner(SaltGenerator saltGenerator) {
		this.saltGenerator = saltGenerator;
	}

	/**
	 * Starts salt generation and logs begin/end markers for Job observability.
	 *
	 * @param args unused
	 * @throws Exception if sequence config is missing or JDBC writes fail
	 */
	@Override
	public void run(String... args) throws Exception {
		mosipLogger.info("Salt generator started");
		saltGenerator.start();
		mosipLogger.info("Salt generator completed");
	}
}
