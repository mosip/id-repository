package io.mosip.idrepository.config;

import static io.mosip.idrepository.common.constant.IdRepoApiPathConstants.CREDENTIAL_REQUEST_PATH_PREFIX;
import static io.mosip.idrepository.common.constant.IdRepoApiPathConstants.CREDENTIAL_SERVICE_PATH_PREFIX;
import static io.mosip.idrepository.common.constant.IdRepoApiPathConstants.IDENTITY_PATH_PREFIX;
import static io.mosip.idrepository.common.constant.IdRepoApiPathConstants.VID_PATH_PREFIX;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.mosip.idrepository.credential.request.controller.CredentialRequestGeneratorController;
import io.mosip.idrepository.credential.store.controller.CredentialStoreController;
import io.mosip.idrepository.identity.controller.IdRepoController;
import io.mosip.idrepository.identity.controller.IdRepoDraftController;
import io.mosip.idrepository.identity.controller.VidEventCallbackController;
import io.mosip.idrepository.vid.controller.VidController;

/**
 * Preserves legacy public URL paths after consolidating four MOSIP microservices into one JVM.
 * <p>
 * Controllers declare relative mappings (for example {@code /v1/identity/...}); this config adds
 * servlet-style path prefixes per controller type so Reg Processor, IDA, partners, and
 * {@code api-test} rigs require no URL changes.
 * </p>
 *
 * <h2>Path prefix map</h2>
 * <table>
 *   <caption>Controller type to prefix</caption>
 *   <tr><th>Prefix</th><th>Controllers</th></tr>
 *   <tr><td>{@code /idrepository/v1/identity}</td><td>{@link IdRepoController}, {@link IdRepoDraftController}, {@link VidEventCallbackController}</td></tr>
 *   <tr><td>{@code /v1/credentialservice}</td><td>{@link CredentialStoreController}</td></tr>
 *   <tr><td>{@code /v1/credentialrequest}</td><td>{@link CredentialRequestGeneratorController}</td></tr>
 *   <tr><td>{@code /idrepository/v1}</td><td>{@link VidController}</td></tr>
 * </table>
 *
 * <p>
 * Server root remains {@code /} ({@code server.servlet.path=/} in {@code bootstrap.properties}).
 * Prefix string values live in {@link io.mosip.idrepository.common.constant.IdRepoApiPathConstants}.
 * </p>
 *
 * @see IdRepoOpenApiConfig
 * @see io.mosip.idrepository.IdRepositoryBootApplication
 * @see io.mosip.idrepository.common.constant.IdRepoApiPathConstants
 */
@Configuration
public class IdRepoApiPathConfig implements WebMvcConfigurer {

	/**
	 * Registers legacy path prefixes on the Spring MVC path matcher.
	 *
	 * @param configurer MVC path-match configurer supplied by Spring Boot
	 */
	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.addPathPrefix(IDENTITY_PATH_PREFIX,
				c -> IdRepoController.class.isAssignableFrom(c)
						|| IdRepoDraftController.class.isAssignableFrom(c)
						|| VidEventCallbackController.class.isAssignableFrom(c));
		configurer.addPathPrefix(CREDENTIAL_SERVICE_PATH_PREFIX,
				c -> CredentialStoreController.class.isAssignableFrom(c));
		configurer.addPathPrefix(CREDENTIAL_REQUEST_PATH_PREFIX,
				c -> CredentialRequestGeneratorController.class.isAssignableFrom(c));
		configurer.addPathPrefix(VID_PATH_PREFIX,
				c -> VidController.class.isAssignableFrom(c));
	}
}
