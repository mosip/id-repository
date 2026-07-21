package io.mosip.idrepository.common.config.openapi;

import lombok.Data;

/**
 * OpenAPI license block bound from {@code openapi.info.license.*}.
 * <p>
 * Nested inside {@link OpenApiInfoProperty} and applied to
 * {@link io.swagger.v3.oas.models.info.License} when legacy configs build the
 * {@link io.swagger.v3.oas.models.OpenAPI} bean.
 * </p>
 *
 * <h2>Example config fragment</h2>
 * <pre>
 * openapi:
 *   info:
 *     license:
 *       name: Mosip
 *       url: https://docs.mosip.io/1.2.0/setup/license
 * </pre>
 *
 * @see OpenApiInfoProperty
 * @see OpenApiProperties#getInfo()
 */
@Data
public class OpenApiLicenseProperty {

	/** SPDX short name or display label (for example {@code Mosip}, {@code MPL 2.0}). */
	private String name;

	/** HTTPS URL to the full license text or project license page. */
	private String url;
}
