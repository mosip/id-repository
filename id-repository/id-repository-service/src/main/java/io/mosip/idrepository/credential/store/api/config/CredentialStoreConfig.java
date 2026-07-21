package io.mosip.idrepository.credential.store.api.config;

import io.mosip.idrepository.common.config.openapi.OpenApiProperties;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Legacy standalone credential-service Spring configuration.
 * <p>
 * Provides OpenAPI/SpringDoc beans and a {@link RestRequestBuilder} for the pre-merge
 * credential-service JVM. Superseded by {@link io.mosip.idrepository.config.IdRepoLibraryConfig}
 * in the consolidated deployable but retained for rollback and standalone runs.
 * </p>
 *
 * @see OpenApiProperties
 * @see io.mosip.idrepository.config.IdRepoLibraryConfig
 */
@Configuration
public class CredentialStoreConfig {

	/** Logger for credential-store configuration lifecycle events. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(CredentialStoreConfig.class);

	/** SpringDoc/OpenAPI metadata bound from {@code openapi.*} config properties. */
	@Autowired
	private OpenApiProperties openApiProperties;

	/**
	 * Credential-service OpenAPI document with title, version, description, license, and servers.
	 * <p>
	 * Values are sourced from {@link #openApiProperties}. In the merged deployable, grouped
	 * documentation is provided by {@code IdRepoOpenApiConfig} in the service module.
	 * </p>
	 *
	 * @return configured {@link OpenAPI} bean for SpringDoc
	 */
	@Bean
	public OpenAPI openApi() {
		OpenAPI api = new OpenAPI()
				.components(new Components())
				.info(new Info()
						.title(openApiProperties.getInfo().getTitle())
						.version(openApiProperties.getInfo().getVersion())
						.description(openApiProperties.getInfo().getDescription())
						.license(new License()
								.name(openApiProperties.getInfo().getLicense().getName())
								.url(openApiProperties.getInfo().getLicense().getUrl())));

		openApiProperties.getService().getServers().forEach(server -> {
			api.addServersItem(new Server().description(server.getDescription()).url(server.getUrl()));
		});
		mosipLogger.info("swagger open api bean is ready");
		return api;
	}

	/**
	 * SpringDoc API group limiting Swagger UI to credential-service REST paths.
	 *
	 * @return {@link GroupedOpenApi} scoped to paths from {@link OpenApiProperties#getGroup()}
	 */
	@Bean
	public GroupedOpenApi groupedOpenApi() {
		return GroupedOpenApi.builder().group(openApiProperties.getGroup().getName())
				.pathsToMatch(openApiProperties.getGroup().getPaths().stream().toArray(String[]::new))
				.build();
	}

	/**
	 * Outbound REST request builder preloaded with every {@link RestServicesConstants} service name.
	 * <p>
	 * Superseded by the {@code @Primary} bean in {@link io.mosip.idrepository.config.IdRepoLibraryConfig}
	 * when running the consolidated deployable.
	 * </p>
	 *
	 * @return {@link RestRequestBuilder} for MOSIP inter-service HTTP calls
	 */
	@Bean
	public RestRequestBuilder getRestRequestBuilder() {
		return new RestRequestBuilder(Arrays.stream(RestServicesConstants.values())
				.map(RestServicesConstants::getServiceName).collect(Collectors.toList()));
	}
}
