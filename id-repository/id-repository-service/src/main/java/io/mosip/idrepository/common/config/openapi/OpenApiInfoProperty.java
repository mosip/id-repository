package io.mosip.idrepository.common.config.openapi;

import lombok.Data;

/**
 * OpenAPI document metadata bound from {@code openapi.info.*}.
 * <p>
 * Mapped to {@link io.swagger.v3.oas.models.info.Info} when legacy configuration classes
 * ({@code SwaggerConfig}, {@code CredentialStoreConfig}, {@code CredentialRequestGeneratorConfig})
 * assemble the top-level {@link io.swagger.v3.oas.models.OpenAPI} bean from config server values.
 * </p>
 *
 * <h2>Property mapping</h2>
 * <table>
 *   <caption>Config key to OpenAPI field</caption>
 *   <tr><th>Property field</th><th>Config key</th><th>OpenAPI 3 target</th></tr>
 *   <tr><td>{@link #title}</td><td>{@code openapi.info.title}</td><td>{@code Info.title}</td></tr>
 *   <tr><td>{@link #description}</td><td>{@code openapi.info.description}</td><td>{@code Info.description}</td></tr>
 *   <tr><td>{@link #version}</td><td>{@code openapi.info.version}</td><td>{@code Info.version}</td></tr>
 *   <tr><td>{@link #license}</td><td>{@code openapi.info.license}</td><td>{@code Info.license}</td></tr>
 * </table>
 *
 * @see OpenApiLicenseProperty
 * @see OpenApiProperties#getInfo()
 * @see io.mosip.idrepository.config.IdRepoOpenApiConfig
 */
@Data
public class OpenApiInfoProperty {

	/** API title shown in Swagger UI header (for example {@code ID Repository Identity Service}). */
	private String title;

	/** Long-form description in the OpenAPI info section; may include module scope and MOSIP version notes. */
	private String description;

	/** Semantic API version string published in documentation (for example {@code 1.0}, {@code 1.2.1}). */
	private String version;

	/** Nested license block; see {@link OpenApiLicenseProperty}. */
	private OpenApiLicenseProperty license;
}
