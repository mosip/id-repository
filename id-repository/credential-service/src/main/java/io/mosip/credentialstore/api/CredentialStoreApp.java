package io.mosip.credentialstore.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * @author Sowmya
 * 
 *         The Class CredentialStoreApp.
 */
@SpringBootApplication
@Import(value = { java.lang.String.class })
@ComponentScan(basePackages = { "io.mosip.*",
		"${mosip.auth.adapter.impl.basepackage}" }, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {
				"io.mosip.kernel.biometrics.*" }))
public class CredentialStoreApp {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(CredentialStoreApp.class, args);
	}
}
