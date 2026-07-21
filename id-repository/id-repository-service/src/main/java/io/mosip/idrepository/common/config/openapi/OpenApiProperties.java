package io.mosip.idrepository.common.config.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Root Spring Boot binding for {@code openapi.*} properties from MOSIP config server.
 * <p>
 * Registered as a {@code @Configuration} bean so legacy OpenAPI configs can {@code @Autowired}
 * it without explicit {@code @EnableConfigurationProperties}. In the consolidated deployable,
 * {@link io.mosip.idrepository.config.IdRepoOpenApiConfig} provides static OpenAPI metadata;
 * this bean remains for standalone/rollback configs listed below.
 * </p>
 *
 * <h2>Bound property prefixes</h2>
 * <table>
 *   <caption>Nested keys under {@code openapi}</caption>
 *   <tr><th>Java field</th><th>Config key</th><th>Purpose</th></tr>
 *   <tr><td>{@link #info}</td><td>{@code openapi.info}</td><td>Title, version, description, license</td></tr>
 *   <tr><td>{@link #service}</td><td>{@code openapi.service}</td><td>Server list for credential OpenAPI docs</td></tr>
 *   <tr><td>{@link #group}</td><td>{@code openapi.group}</td><td>SpringDoc group name and path patterns</td></tr>
 *   <tr><td>{@link #idRepoIdentityServiceServer}</td><td>{@code openapi.id-repo-identity-service-server}</td><td>Server list for identity OpenAPI docs</td></tr>
 * </table>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link io.mosip.idrepository.identity.config.SwaggerConfig}</li>
 *   <li>{@link io.mosip.idrepository.credential.store.api.config.CredentialStoreConfig}</li>
 *   <li>{@link io.mosip.idrepository.credential.request.api.config.CredentialRequestGeneratorConfig}</li>
 * </ul>
 *
 * @see OpenApiInfoProperty
 * @see OpenApiService
 * @see OpenApiGroup
 */
@Configuration
@ConfigurationProperties(prefix = "openapi")
@Data
public class OpenApiProperties {

	/**
	 * API document metadata ({@code openapi.info.*}) — title, description, version, license.
	 * Mapped to {@link io.swagger.v3.oas.models.info.Info} in legacy OpenAPI beans.
	 */
	private OpenApiInfoProperty info;

	/**
	 * Credential-service / credential-request server entries ({@code openapi.service.servers[]}).
	 * Used by {@code CredentialStoreConfig} and {@code CredentialRequestGeneratorConfig}.
	 */
	private OpenApiService service;

	/**
	 * SpringDoc group selector ({@code openapi.group.name}, {@code openapi.group.paths[]}).
	 * Drives {@link org.springdoc.core.models.GroupedOpenApi#pathsToMatch} in legacy configs.
	 */
	private OpenApiGroup group;

	/**
	 * Identity-service server list ({@code openapi.id-repo-identity-service-server.servers[]}).
	 * Used exclusively by {@link io.mosip.idrepository.identity.config.SwaggerConfig}.
	 */
	private OpenApiService idRepoIdentityServiceServer;

}
