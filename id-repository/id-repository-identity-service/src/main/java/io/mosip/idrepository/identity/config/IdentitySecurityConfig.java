package io.mosip.idrepository.identity.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;


@Configuration
public class IdentitySecurityConfig {
	
	@Bean
	public RestHelper restHelperWithAuth(@Qualifier("selfTokenWebClient") WebClient webClient) {
		return new RestHelper(webClient);
	}
	
	@Bean
	public IdRepoSecurityManager securityManagerWithAuth(@Qualifier("selfTokenWebClient") WebClient webClient) {
		return new IdRepoSecurityManager(restHelperWithAuth(webClient));
	}
	
	@Bean
	public CredentialServiceManager credentialServiceManager(@Qualifier("selfTokenWebClient") WebClient webClient) {
		return new CredentialServiceManager(restHelperWithAuth(webClient));
	}

}
