package io.mosip.idrepository.common.config.openapi;

import java.util.List;

import lombok.Data;

/**
 * SpringDoc API group definition bound from {@code openapi.group.*}.
 * <p>
 * Legacy credential configs use {@link #getName()} as the SpringDoc group id and
 * {@link #getPaths()} as {@code pathsToMatch} patterns so Swagger UI shows only
 * credential-service or credential-request endpoints in standalone deployments.
 * </p>
 *
 * <h2>Example config fragment</h2>
 * <pre>
 * openapi:
 *   group:
 *     name: credential-service
 *     paths:
 *       - /v1/credentialservice/**
 * </pre>
 *
 * <p>
 * Consolidated deployable uses package-scanned groups in
 * {@link io.mosip.idrepository.config.IdRepoOpenApiConfig} instead of path patterns from config.
 * </p>
 *
 * @see OpenApiProperties#getGroup()
 * @see org.springdoc.core.models.GroupedOpenApi
 */
@Data
public class OpenApiGroup {

	/**
	 * Group identifier in Swagger UI's group dropdown (for example {@code credential-service}).
	 */
	private String name;

	/**
	 * Ant-style path patterns selecting REST endpoints for this group.
	 * Passed to {@link org.springdoc.core.models.GroupedOpenApi.Builder#pathsToMatch(String...)}.
	 */
	private List<String> paths;
}
