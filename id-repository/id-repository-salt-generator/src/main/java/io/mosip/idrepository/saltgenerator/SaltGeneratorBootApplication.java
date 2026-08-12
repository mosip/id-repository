package io.mosip.idrepository.saltgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import io.mosip.idrepository.saltgenerator.config.SaltGeneratorConfiguration;

/**
 * Spring Boot entry point for the ID-Repository salt-generator Kubernetes Job.
 *
 * <p>
 * Starts a <strong>non-web</strong> Spring context, runs
 * {@link SaltGeneratorRunner} once, then exits the JVM with the Spring exit code.
 * Spring Batch auto-configuration is excluded — this Job uses plain JDBC chunking,
 * not Spring Batch metadata tables.
 * </p>
 *
 * <h2>Component scan</h2>
 * <ul>
 *   <li>{@code io.mosip.idrepository.saltgenerator} — Job-local beans</li>
 *   <li>{@code io.mosip.idrepository.core.util} — {@code EnvUtil} and related helpers</li>
 * </ul>
 * Controllers, identity/VID/credential services, and HTTP security are intentionally
 * out of scope.
 *
 * <h2>Deploy</h2>
 * Chart: {@code helm/idrepo-saltgen}. Image entry runs this class as a one-shot Job
 * ({@code restartPolicy: Never}), not a Deployment.
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Ops / Helm after fresh DB deploy or salt-table DDL changes</li>
 *   <li>Local docker-compose: salts seeded in {@code init.sql} (no Job);
 *       otherwise {@code java -jar id-repository-salt-generator-*.jar}</li>
 * </ul>
 *
 * @author MOSIP
 * @see SaltGeneratorRunner
 * @see SaltGeneratorConfiguration
 * @see io.mosip.idrepository.saltgenerator.service.SaltGenerator
 */
@SpringBootApplication(excludeName = {
		"org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration" })
@ComponentScan(basePackages = { "io.mosip.idrepository.saltgenerator", "io.mosip.idrepository.core.util" })
@Import({ SaltGeneratorConfiguration.class, SaltGeneratorRunner.class })
public class SaltGeneratorBootApplication {

	/**
	 * Boots the non-web application, runs the salt Job via {@link SaltGeneratorRunner},
	 * and terminates the process.
	 *
	 * @param args unused command-line arguments (config comes from Spring Cloud Config /
	 *             environment)
	 */
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SaltGeneratorBootApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext ctx = app.run(args);
		System.exit(SpringApplication.exit(ctx));
	}
}
