package io.mosip.credentialstore.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


/**
 * @author Sowmya
 * 
 *         The Class CredentialStoreApp.
 */
@SpringBootApplication
@ComponentScan(basePackages = { "io.mosip.credentialstore.*" })
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
