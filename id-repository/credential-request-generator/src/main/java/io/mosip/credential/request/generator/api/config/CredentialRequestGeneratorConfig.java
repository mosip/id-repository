package io.mosip.credential.request.generator.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class CredentialRequestGeneratorConfig {
	
	@Bean
	public DummyPartnerCheckUtil dummyPartnerCheckUtil() {
		return new DummyPartnerCheckUtil();
	}
	
	@Bean
	public RestHelper restHelper() {
		return new RestHelper();
	}
	
	@Bean
	public IdRepoSecurityManager securityManager() {
		return new IdRepoSecurityManager();
	}
	
	@Bean
	public Docket dataShareapiBean() {
		return new Docket(DocumentationType.SWAGGER_2).groupName("Credential Request Renerator").select()
				.apis(RequestHandlerSelectors.basePackage("io.mosip.credential.request.generator.controller"))
				.paths(PathSelectors.regex("(?!/(error|actuator).*).*")).build();

	}
}
