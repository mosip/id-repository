package io.mosip.credential.request.generator.api;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The Class CredentialRequestGeneratorApp.
 *
 * @author Sowmya
 */
@SpringBootApplication
@Import(value = { java.lang.String.class })
@ComponentScan(basePackages = { "io.mosip.*",
		"${mosip.auth.adapter.impl.basepackage}" }, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {
				"io.mosip.kernel.biometrics.*" }))
@EnableBatchProcessing
@EnableScheduling
public class CredentialRequestGeneratorApp {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(CredentialRequestGeneratorApp.class, args);
	}
}
