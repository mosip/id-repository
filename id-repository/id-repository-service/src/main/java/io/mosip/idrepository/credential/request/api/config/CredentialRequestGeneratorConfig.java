package io.mosip.idrepository.credential.request.api.config;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.mosip.idrepository.common.config.openapi.OpenApiProperties;
import io.mosip.idrepository.credential.request.entity.CredentialEntity;
import io.mosip.idrepository.credential.request.interceptor.CredentialTransactionInterceptor;
import io.mosip.idrepository.credential.request.util.CredReqRestUtil;
import io.mosip.idrepository.credential.request.util.CryptoUtil;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.config.IdRepoHibernateJpaProperties;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Legacy standalone credential-request-generator configuration.
 * <p>
 * Extends kernel {@link HibernateDaoConfig} for the credential database, registers
 * {@link CredentialTransactionInterceptor} on the Hibernate session factory, and exposes
 * OpenAPI/SpringDoc beans. Superseded by
 * {@link io.mosip.idrepository.config.IdRepoLibraryConfig} in the merged JVM but retained
 * for rollback and standalone credential-request-generator deployments.
 * </p>
 *
 * @see io.mosip.idrepository.config.IdRepoLibraryConfig
 * @see OpenApiProperties
 */
@Configuration
@EnableJpaRepositories(entityManagerFactoryRef = "entityManagerFactory", basePackages = "io.mosip.idrepository.credential.request.repository.*", repositoryBaseClass = HibernateRepositoryImpl.class, excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ASPECTJ, pattern = { "io.mosip.idrepository.core.repository.*" }) })
@EntityScan(basePackageClasses = { CredentialEntity.class })
public class CredentialRequestGeneratorConfig extends HibernateDaoConfig {

	/** Logger for credential-request configuration lifecycle events. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(CredentialRequestGeneratorConfig.class);

	/**
	 * Outbound REST utility for credential-request external calls.
	 * Qualified as {@code credReqRestUtil} to avoid collision with credential-store {@code RestUtil}.
	 */
	@Autowired
	@Qualifier("credReqRestUtil")
	private CredReqRestUtil restUtil;

	/** SpringDoc/OpenAPI metadata bound from {@code openapi.*} config properties. */
	@Autowired
	private OpenApiProperties openApiProperties;

	/** Credential-row encrypt/decrypt helper used by the Hibernate session interceptor. */
	@Autowired
	private CryptoUtil cryptoUtil;

	/**
	 * Adds {@link CredentialTransactionInterceptor} to Hibernate session factory properties.
	 * <p>
	 * The interceptor encrypts credential payload columns on write and decrypts on read using
	 * {@link #restUtil} and {@link #cryptoUtil}.
	 * </p>
	 *
	 * @return JPA properties map including the {@code hibernate.session_factory.interceptor} entry
	 */
	@Override
	public Map<String, Object> jpaProperties() {
		Map<String, Object> jpaProperties = super.jpaProperties();
		IdRepoHibernateJpaProperties.applyKernelAuthClassLoaderSettings(jpaProperties);
		jpaProperties.put("hibernate.session_factory.interceptor", new CredentialTransactionInterceptor(restUtil, cryptoUtil));
		return jpaProperties;
	}

	/**
	 * Credential-request OpenAPI document with title, version, description, license, and servers.
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
	 * SpringDoc API group limiting Swagger UI to credential-request REST paths.
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
