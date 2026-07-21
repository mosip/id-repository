package io.mosip.idrepository.common.config.openapi;

import lombok.Data;

/**
 * OpenAPI {@code servers[]} entry — one deployable environment base URL for Swagger UI.
 * <p>
 * Bound from list elements under {@code openapi.service.servers} or
 * {@code openapi.id-repo-identity-service-server.servers}. Each instance is converted to
 * {@link io.swagger.v3.oas.models.servers.Server} in legacy OpenAPI configuration classes.
 * </p>
 *
 * <h2>Example config fragment</h2>
 * <pre>
 * openapi:
 *   service:
 *     servers:
 *       - description: Dev
 *         url: https://api.dev.mosip.net/v1/credentialservice
 * </pre>
 *
 * @see OpenApiService
 * @see OpenApiProperties#getService()
 */
@Data
public class OpenApiServer {

	/**
	 * Human-readable label shown in Swagger UI's server dropdown (for example {@code Dev}, {@code QA}).
	 */
	private String description;

	/**
	 * Base URL for try-it-out requests — scheme, host, optional port, and API context path.
	 * Should match the virtual service or ingress path clients use in that environment.
	 */
	private String url;
}
