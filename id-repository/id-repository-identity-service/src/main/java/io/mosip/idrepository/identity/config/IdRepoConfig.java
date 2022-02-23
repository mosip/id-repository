package io.mosip.idrepository.identity.config;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.CLIENT_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.MASTERDATA_RETRIEVE_ERROR;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.AuthenticationException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * The Class IdRepoConfig.
 *
 * @author Manoj SP
 */
@Configuration
@ConfigurationProperties("mosip.idrepo.identity")
@EnableTransactionManagement
@EnableAsync
@EnableCaching
public class IdRepoConfig implements WebMvcConfigurer {
	
	@Value("${" + IdRepoConstants.WEB_SUB_PUBLISH_URL + "}")
	public String publisherHubURL;

	/** The mosip logger. */
	Logger mosipLogger = IdRepoLogger.getLogger(IdRepoConfig.class);

	/** The env. */
	@Autowired
	private RestTemplate restTemplate;

	/** The interceptor. */
	@Autowired
	private Interceptor interceptor;

	/** The db. */
//	If sharding is enabled, need to uncomment
//	private Map<String, Map<String, String>> db;

	/** The uin Status. */
	private List<String> uinStatus;

	/** The allowed bio types. */
	private List<String> allowedBioAttributes;

	/** The bio attributes. */
	private List<String> bioAttributes;

	/** The allowed types. */
	private List<String> allowedTypes;

	/** The id. */
	private Map<String, String> id;
	
	@Autowired
	private Environment env;
	
	@Autowired
	private ObjectMapper mapper;

	@PostConstruct
	public void init() {
		mapper.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {

			@Override
			protected void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
				mosipLogger.error(IdRepoSecurityManager.getUser(), "restTemplate - handleError", "Rest Template logs",
						"Status error : " + response.getRawStatusCode() + " " + response.getStatusCode() + "  "
								+ response.getStatusText());
				if (response.getStatusCode().is4xxClientError()) {
					if (response.getRawStatusCode() == 401 || response.getRawStatusCode() == 403) {
						mosipLogger.error(IdRepoSecurityManager.getUser(), "restTemplate - handleError",
								"request failed with status code :" + response.getRawStatusCode(),
								"\n\n" + new String(super.getResponseBody(response)));
						List<ServiceError> errorList = ExceptionUtils
								.getServiceErrorList(new String(super.getResponseBody(response)));
						mosipLogger.error(IdRepoSecurityManager.getUser(), "restTemplate - handleError",
								"Throwing AuthenticationException", errorList.toString());
						if(errorList.isEmpty()) {
							throw new AuthenticationException(IdRepoErrorConstants.AUTHENTICATION_FAILED, response.getRawStatusCode());
						} else {
							throw new AuthenticationException(errorList.get(0).getErrorCode(),
									errorList.get(0).getMessage(), response.getRawStatusCode());
						}
					} else {
						mosipLogger.error(IdRepoSecurityManager.getUser(), "restTemplate - handleError", "Rest Template logs",
								"Status error - returning RestServiceException - CLIENT_ERROR -- "
										+ new String(super.getResponseBody(response)));
						throw new IdRepoAppUncheckedException(CLIENT_ERROR);
					}
				} else {
					mosipLogger.error(IdRepoSecurityManager.getUser(), "restTemplate - handleError", "Rest Template logs",
							"Status error - returning RestServiceException - CLIENT_ERROR -- "
									+ new String(super.getResponseBody(response)));
					throw new IdRepoAppUncheckedException(MASTERDATA_RETRIEVE_ERROR);
				}
			}
		});
	}

	/**
	 * Gets the db.
	 *
	 * @return the db
	 */
//	If sharding is enabled, need to uncomment
//	public Map<String, Map<String, String>> getDb() {
//		return db;
//	}

	/**
	 * Sets the db.
	 *
	 * @param db
	 *            the db
	 */
//	If sharding is enabled, need to uncomment
//	public void setDb(Map<String, Map<String, String>> db) {
//		this.db = db;
//	}

	/**
	 * Sets the status.
	 *
	 * @param uinStatus the new uin status
	 */
	public void setUinStatus(List<String> uinStatus) {
		this.uinStatus = uinStatus;
	}

	/**
	 * Sets the id.
	 *
	 * @param id
	 *            the id
	 */
	public void setId(Map<String, String> id) {
		this.id = id;
	}

	/**
	 * Sets the allowed bio types.
	 *
	 * @param allowedBioAttributes
	 *            the new allowed bio types
	 */
	public void setAllowedBioAttributes(List<String> allowedBioAttributes) {
		this.allowedBioAttributes = allowedBioAttributes;
	}

	/**
	 * Sets the bio attributes.
	 *
	 * @param bioAttributes the new bio attributes
	 */
	public void setBioAttributes(List<String> bioAttributes) {
		this.bioAttributes = bioAttributes;
	}

	/**
	 * Sets the allowed types.
	 *
	 * @param allowedTypes the new allowed types
	 */
	public void setAllowedTypes(List<String> allowedTypes) {
		this.allowedTypes = allowedTypes;
	}

	// FIXME Need to check for UIN-Reg ID scenario
	// /**
	// * Gets the shard data source resolver.
	// *
	// * @return the shard data source resolver
	// */
	// @Bean
	// public ShardDataSourceResolver getShardDataSourceResolver() {
	// ShardDataSourceResolver resolver = new ShardDataSourceResolver();
	// resolver.setLenientFallback(false);
	// resolver.setTargetDataSources(db.entrySet().parallelStream()
	// .collect(Collectors.toMap(Map.Entry::getKey, value ->
	// buildDataSource(value.getValue()))));
	// return resolver;
	// }

	/**
	 * Id.
	 *
	 * @return the map
	 */
	@Bean
	public Map<String, String> id() {
		return Collections.unmodifiableMap(id);
	}

	/**
	 * Allowed bio types.
	 *
	 * @return the list
	 */
	@Bean
	public List<String> allowedBioAttributes() {
		return Collections.unmodifiableList(allowedBioAttributes);
	}

	/**
	 * Bio attributes.
	 *
	 * @return the list
	 */
	@Bean
	public List<String> bioAttributes() {
		return Collections.unmodifiableList(bioAttributes);
	}

	/**
	 * Allowed types.
	 *
	 * @return the list
	 */
	@Bean
	public List<String> allowedTypes() {
		return Collections.unmodifiableList(allowedTypes);
	}

	/**
	 * Status.
	 *
	 * @return the map
	 */
	@Bean
	public List<String> uinStatus() {
		return Collections.unmodifiableList(uinStatus);
	}

	/**
	 * Entity manager factory.
	 *
	 * @param dataSource
	 *            the data source
	 * @return the local container entity manager factory bean
	 */
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource);
		em.setPackagesToScan("io.mosip.idrepository.identity.*");

		JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(vendorAdapter);
		em.setJpaPropertyMap(additionalProperties());

		return em;
	}

	/**
	 * Additional properties.
	 *
	 * @return the properties
	 */
	private Map<String, Object> additionalProperties() {
		Map<String, Object> jpaProperties = new HashMap<>();
		jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL92Dialect");
		jpaProperties.put("hibernate.temp.use_jdbc_metadata_defaults", Boolean.FALSE);
		jpaProperties.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
		jpaProperties.put("hibernate.physical_naming_strategy", SpringPhysicalNamingStrategy.class.getName());
		jpaProperties.put("hibernate.ejb.interceptor", interceptor);
		return jpaProperties;
	}

	/**
	 * Builds the data source.
	 *
	 * @param dataSourceValues
	 *            the data source values
	 * @return the data source
	 */
	private DataSource buildDataSource(Map<String, String> dataSourceValues) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(dataSourceValues.get("url"));
		dataSource.setUsername(dataSourceValues.get("username"));
		dataSource.setPassword(dataSourceValues.get("password"));
		dataSource.setDriverClassName(dataSourceValues.get("driverClassName"));
		return dataSource;
	}

	/**
	 * Data source.
	 *
	 * @return the data source
	 */
	@Bean
	public DataSource dataSource() {
//		If sharding is enabled, need to uncomment
//		return buildDataSource(db.get("shard"));
		
//		If sharding is enabled, need to comment below code
		Map<String, String> dbValues = new HashMap<>();
		dbValues.put("url", env.getProperty("mosip.idrepo.identity.db.url"));
		dbValues.put("username", env.getProperty("mosip.idrepo.identity.db.username"));
		dbValues.put("password", env.getProperty("mosip.idrepo.identity.db.password"));
		dbValues.put("driverClassName", env.getProperty("mosip.idrepo.identity.db.driverClassName"));
		return buildDataSource(dbValues);
	}
	
	/*
	 * This bean is returned because for async task the security context needs to be
	 * passed.
	 * 
	 */
	@Bean("withSecurityContext")
	public DelegatingSecurityContextAsyncTaskExecutor taskExecutor() {
		return new DelegatingSecurityContextAsyncTaskExecutor(threadPoolTaskExecutor());
	}

	private ThreadPoolTaskExecutor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(3);
		executor.setMaxPoolSize(3);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("idrepo-");
		executor.initialize();
		return executor;
	}
}
