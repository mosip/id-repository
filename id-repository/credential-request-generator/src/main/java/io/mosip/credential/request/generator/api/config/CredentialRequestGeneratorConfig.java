package io.mosip.credential.request.generator.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@EnableJpaRepositories(basePackageClasses = {
		CredentialRepositary.class }, basePackages = "io.mosip.credential.request.generator.repositary.*", repositoryBaseClass = HibernateRepositoryImpl.class)
@EntityScan(basePackageClasses = { CredentialEntity.class })
public class CredentialRequestGeneratorConfig {

	@Bean
	public Docket dataShareapiBean() {
		return new Docket(DocumentationType.SWAGGER_2).groupName("Credential Request Renerator").select()
				.apis(RequestHandlerSelectors.basePackage("io.mosip.credential.request.generator.controller"))
				.paths(PathSelectors.regex("(?!/(error|actuator).*).*")).build();

	}
}
