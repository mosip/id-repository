package io.mosip.idrepository.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.idrepository.common.config.openapi.OpenApiProperties;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Legacy identity-only OpenAPI bean from standalone identity-service.
 * <p>
 * Superseded in the consolidated deployable by
 * {@code io.mosip.idrepository.config.IdRepoOpenApiConfig} in the service module.
 * Retained for backward compatibility when this config is loaded in isolation.
 * </p>
 *
 * @see OpenApiProperties
 */
@Configuration
public class SwaggerConfig {

	/** Application logger for OpenAPI property binding diagnostics. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(SwaggerConfig.class);

	/**
	 * Builds the identity OpenAPI document from {@code openapi.*} properties.
	 *
	 * @param openApiProperties bound OpenAPI metadata and server list
	 * @return configured {@link OpenAPI} instance
	 */
	@Bean
	/**
	 * Open api.
	 * @param openApiProperties open api properties
	 * @return open api
	 */
	public OpenAPI openApi(OpenApiProperties openApiProperties) {
		String msg = "Swagger open api, ";
		OpenAPI api = new OpenAPI()
				.components(new Components());
		if (null != openApiProperties.getInfo()) {
			api.info(new Info()
					.title(openApiProperties.getInfo().getTitle())
					.version(openApiProperties.getInfo().getVersion())
					.description(openApiProperties.getInfo().getDescription()));
			if (null != openApiProperties.getInfo().getLicense()) {
				api.getInfo().license(new License()
						.name(openApiProperties.getInfo().getLicense().getName())
						.url(openApiProperties.getInfo().getLicense().getUrl()));
				mosipLogger.info(msg + "info license property is added");
			} else {
				mosipLogger.error(msg + "info license property is empty");
			}
			mosipLogger.info(msg + "info property is added");
		} else {
			mosipLogger.error(msg + "info property is empty");
		}

		if (null != openApiProperties.getIdRepoIdentityServiceServer()
				&& null != openApiProperties.getIdRepoIdentityServiceServer().getServers()) {
			openApiProperties.getIdRepoIdentityServiceServer().getServers().forEach(server -> {
				api.addServersItem(new Server().description(server.getDescription()).url(server.getUrl()));
			});
			mosipLogger.info(msg + "server property is added");
		} else {
			mosipLogger.error(msg + "server property is empty");
		}
		return api;
	}
}