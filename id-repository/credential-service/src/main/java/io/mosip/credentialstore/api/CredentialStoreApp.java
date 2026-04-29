package io.mosip.credentialstore.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;


/**
 * @author Sowmya
 * 
 *         The Class CredentialStoreApp.
 */
@SpringBootApplication
@EnableCaching
@Import(value = { java.lang.String.class })
@ComponentScan(basePackages={ "io.mosip.*" ,"${mosip.auth.adapter.impl.basepackage}"})
public class CredentialStoreApp 
{

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(CredentialStoreApp.class, args);
	}
}
