package io.mosip.idrepository.identity.config;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.CLIENT_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.MASTERDATA_RETRIEVE_ERROR;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.mosip.idrepository.core.config.IdRepoDataSourceConfig;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.AuthenticationException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.httpfilter.AuthTokenExchangeFilter;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.manager.CredentialStatusManager;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
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
@EnableScheduling
@EnableJpaRepositories(basePackages = "io.mosip.idrepository.*")
@Import({ CredentialStatusManager.class, DummyPartnerCheckUtil.class })
public class IdRepoConfig extends IdRepoDataSourceConfig implements WebMvcConfigurer, ApplicationListener<ApplicationReadyEvent> {

	@Value("${" + IdRepoConstants.WEB_SUB_PUBLISH_URL + "}")
	public String publisherHubURL;

	/** The mosip logger. */
	Logger mosipLogger = IdRepoLogger.getLogger(IdRepoConfig.class);

	/** The env. */
	@Autowired
	private RestTemplate restTemplate;

	/** The db. */
//	If sharding is enabled, need to uncomment
//	private Map<String, Map<String, String>> db;

	/** The uin Status. */
	private List<String> uinStatus;

	/** The allowed bio types. */
	private List<String> allowedBioAttributes;

	/** The bio attributes. */
	private List<String> bioAttributes;

	/** The id. */
	private Map<String, String> id;

	@Autowired
	private CredentialStatusManager credStatusManager;

	@Autowired
	private IdRepoWebSubHelper websubHelper;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		websubHelper.subscribeForVidEvent();
	}

	@PostConstruct
	public void init() {
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
						if (errorList.isEmpty()) {
							throw new AuthenticationException(IdRepoErrorConstants.AUTHENTICATION_FAILED,
									response.getRawStatusCode());
						} else {
							throw new AuthenticationException(errorList.get(0).getErrorCode(), errorList.get(0).getMessage(),
									response.getRawStatusCode());
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
	 * @param db the db
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
	 * @param id the id
	 */
	public void setId(Map<String, String> id) {
		this.id = id;
	}

	/**
	 * Sets the allowed bio types.
	 *
	 * @param allowedBioAttributes the new allowed bio types
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
	 * Status.
	 *
	 * @return the map
	 */
	@Bean
	public List<String> uinStatus() {
		return Collections.unmodifiableList(uinStatus);
	}

	@Scheduled(fixedDelayString = "${" + IdRepoConstants.CREDENTIAL_STATUS_JOB_DELAY + ":1000}")
	public void credentialStatusHandlerJob() {
		credStatusManager.triggerEventNotifications();
	}

	@Bean
	public CredentialServiceManager credentialServiceManager() {
		return new CredentialServiceManager(restHelperWithAuth());
	}

	@Bean
	@Primary
	public RestHelper restHelper() {
		return new RestHelper();
	}

	@Bean
	public AuthTokenExchangeFilter authTokenExchangeFilter() {
		return new AuthTokenExchangeFilter();
	}

	@Bean("restHelperWithAuth")
	public RestHelper restHelperWithAuth() {
		return new RestHelper(WebClient.builder().filter(authTokenExchangeFilter()).build());
	}

	@Bean
	public IdRepoSecurityManager securityManager() {
		return new IdRepoSecurityManager(restHelper());
	}

	@Bean("securityManagerWithAuth")
	public IdRepoSecurityManager securityManagerWithAuth() {
		return new IdRepoSecurityManager(restHelperWithAuth());
	}
	
	@Bean("AsyncWithAuth")
	public DelegatingSecurityContextAsyncTaskExecutor taskExecutor(ThreadPoolTaskExecutor delegate) { 
	    return new DelegatingSecurityContextAsyncTaskExecutor(delegate); 
	}
}
