package io.mosip.idrepository.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * OpenAPI 3 and SpringDoc configuration for the consolidated ID-Repository deployable.
 * <p>
 * Exposes one merged API document and four Swagger UI groups that mirror the pre-consolidation
 * microservice boundaries. Controller discovery is package-scoped; URL prefixes come from
 * {@link IdRepoApiPathConfig}.
 * </p>
 *
 * <h2>Swagger UI groups</h2>
 * <table>
 *   <caption>SpringDoc groups vs servlet paths</caption>
 *   <tr><th>Group id</th><th>Controllers</th><th>Typical base path</th></tr>
 *   <tr><td>{@code identity}</td><td>{@code identity.controller.*}</td><td>{@code /idrepository/v1/identity}</td></tr>
 *   <tr><td>{@code credential-service}</td><td>{@code credential.store.controller.*}</td><td>{@code /v1/credentialservice}</td></tr>
 *   <tr><td>{@code credential-request}</td><td>{@code credential.request.controller.*}</td><td>{@code /v1/credentialrequest}</td></tr>
 *   <tr><td>{@code vid}</td><td>{@code vid.controller.*}</td><td>{@code /idrepository/v1}</td></tr>
 * </table>
 *
 * <h2>Bootstrap properties</h2>
 * <p>
 * {@code springdoc.swagger-ui.urls-primary-name=identity} and related keys live in
 * {@code bootstrap.properties}. Legacy per-service {@code SwaggerConfig} classes are excluded from scan.
 * </p>
 *
 * @see IdRepoApiPathConfig
 */
@Configuration
public class IdRepoOpenApiConfig {

	/**
	 * Top-level OpenAPI metadata shown on the unified service document.
	 *
	 * @return OpenAPI descriptor with title, description, version, and MPL license
	 */
	@Bean
	public OpenAPI idRepositoryOpenApi() {
		return new OpenAPI()
				.components(new Components())
				.info(new Info()
						.title("MOSIP ID-Repository Service")
						.description("Identity, Credential, Credential Request, and VID APIs")
						.version("1.2.1")
						.license(new License().name("MPL 2.0").url("https://www.mozilla.org/en-US/MPL/2.0/")));
	}

	/**
	 * Swagger group for identity and draft identity controllers.
	 *
	 * @return grouped API scanning {@code io.mosip.idrepository.identity.controller}
	 */
	@Bean
	public GroupedOpenApi identityApi() {
		return GroupedOpenApi.builder().group("identity")
				.displayName("Id Repository Identity Service")
				.packagesToScan("io.mosip.idrepository.identity.controller")
				.build();
	}

	/**
	 * Swagger group for credential issuance (credential store) controllers.
	 *
	 * @return grouped API scanning {@code io.mosip.idrepository.credential.store.controller}
	 */
	@Bean
	public GroupedOpenApi credentialServiceApi() {
		return GroupedOpenApi.builder().group("credential-service")
				.displayName("Credential Store")
				.packagesToScan("io.mosip.idrepository.credential.store.controller")
				.build();
	}

	/**
	 * Swagger group for credential request queue / batch controllers.
	 *
	 * @return grouped API scanning {@code io.mosip.idrepository.credential.request.controller}
	 */
	@Bean
	public GroupedOpenApi credentialRequestApi() {
		return GroupedOpenApi.builder().group("credential-request")
				.displayName("Credential Request Generator")
				.packagesToScan("io.mosip.idrepository.credential.request.controller")
				.build();
	}

	/**
	 * Swagger group for VID lifecycle controllers.
	 *
	 * @return grouped API scanning {@code io.mosip.idrepository.vid.controller}
	 */
	@Bean
	public GroupedOpenApi vidApi() {
		return GroupedOpenApi.builder().group("vid")
				.displayName("Id Repo VID Service")
				.packagesToScan("io.mosip.idrepository.vid.controller")
				.build();
	}
}