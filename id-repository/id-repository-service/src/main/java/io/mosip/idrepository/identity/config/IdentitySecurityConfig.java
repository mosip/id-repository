package io.mosip.idrepository.identity.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.manager.CredentialServiceManager;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;

/**
 * Security-aware outbound HTTP beans using the self-token {@link WebClient}.
 * <p>
 * Provides auth-capable {@link RestHelper}, {@link IdRepoSecurityManager}, and
 * {@link CredentialServiceManager} instances for identity flows that call
 * external MOSIP services with a service account token.
 * </p>
 *
 * @see RestHelper
 * @see IdRepoSecurityManager
 * @see CredentialServiceManager
 * @see io.mosip.idrepository.identity.config.IdRepoConfig
 */
@Configuration
public class IdentitySecurityConfig {

	/**
	 * @param webClient self-token reactive HTTP client from kernel auth adapter
	 * @return REST helper that propagates MOSIP auth headers
	 */
	@Bean
	@Primary
	/**
	 * Rest helper with auth.
	 * @param webClient web client
	 * @return rest helper
	 */
	public RestHelper restHelper(@Qualifier("selfTokenWebClient") WebClient webClient) {
		return new RestHelper(webClient);
	}

	/**
	 * @param webClient self-token reactive HTTP client
	 * @return security manager with authenticated REST helper
	 */
	@Bean
	/**
	 * Security manager with auth.
	 * @param webClient web client
	 * @return id repo security manager
	 */
	public IdRepoSecurityManager securityManagerWithAuth(@Qualifier("selfTokenWebClient") WebClient webClient) {
		return new IdRepoSecurityManager(restHelper(webClient));
	}

	/**
	 * @param webClient self-token reactive HTTP client
	 * @return credential pipeline manager using authenticated outbound calls
	 */
	@Bean
	/**
	 * Credential service manager.
	 * @param webClient web client
	 * @return credential service manager
	 */
	public CredentialServiceManager credentialServiceManager(@Qualifier("selfTokenWebClient") WebClient webClient) {
		return new CredentialServiceManager(restHelper(webClient));
	}
}