package io.mosip.credential.request.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


/**
 * The Class CredentialRequestGeneratorApp.
 *
 * @author Sowmya
 */
@SpringBootApplication
@ComponentScan(basePackages = { "io.mosip.credential.request.generator.*" })
public class CredentialRequestGeneratorApp 
{

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
    public static void main( String[] args )
    {
    	SpringApplication.run(CredentialRequestGeneratorApp.class, args);
    }
}
