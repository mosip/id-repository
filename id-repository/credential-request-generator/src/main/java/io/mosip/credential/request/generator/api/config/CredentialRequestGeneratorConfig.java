package io.mosip.credential.request.generator.api.config;

import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = {
		CredentialRepositary.class }, basePackages = "io.mosip.credential.request.generator.repositary.*", repositoryBaseClass = HibernateRepositoryImpl.class)
@EntityScan(basePackageClasses = { CredentialEntity.class })
public class CredentialRequestGeneratorConfig {

	private static final Logger logger = LoggerFactory.getLogger(CredentialRequestGeneratorConfig.class);

	@Autowired
	private OpenApiProperties openApiProperties;

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
		logger.info("swagger open api bean is ready");
		return api;
	}

	@Bean
	public GroupedOpenApi groupedOpenApi() {
		return GroupedOpenApi.builder().group(openApiProperties.getGroup().getName())
				.pathsToMatch(openApiProperties.getGroup().getPaths().stream().toArray(String[]::new))
				.build();
	}
}
