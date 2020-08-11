package io.mosip.credential.request.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;

@SpringBootApplication
@ComponentScan(basePackages = { "io.mosip.credential.request.generator.*" })
public class CredentialRequestGeneratorApp 
{
    public static void main( String[] args )
    {
    	SpringApplication.run(CredentialRequestGeneratorApp.class, args);
    }
}
