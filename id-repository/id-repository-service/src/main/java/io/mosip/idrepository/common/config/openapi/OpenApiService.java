package io.mosip.idrepository.common.config.openapi;

import java.util.List;

import lombok.Data;

/**
 * Container for a list of {@link OpenApiServer} entries under an {@code openapi.*} server key.
 * <p>
 * Reused for both credential modules ({@link OpenApiProperties#getService()}) and identity
 * ({@link OpenApiProperties#getIdRepoIdentityServiceServer()}). Legacy configs iterate
 * {@link #getServers()} and call {@code OpenAPI.addServersItem(...)} for each entry.
 * </p>
 *
 * <h2>Config keys</h2>
 * <ul>
 *   <li>{@code openapi.service.servers} — credential store / credreq Swagger servers</li>
 *   <li>{@code openapi.id-repo-identity-service-server.servers} — identity Swagger servers</li>
 * </ul>
 *
 * @see OpenApiServer
 * @see io.mosip.idrepository.credential.store.api.config.CredentialStoreConfig#openApi()
 * @see io.mosip.idrepository.identity.config.SwaggerConfig#openApi(OpenApiProperties)
 */
@Data
public class OpenApiService {

	/**
	 * Ordered server definitions exposed in Swagger UI's environment selector.
	 * May be {@code null} when config server omits server list (legacy configs log an error).
	 */
	private List<OpenApiServer> servers;
}
