package io.mosip.idrepository.identity.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.mosip.idrepository.core.config.IdRepoDataSourceConfig;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.manager.CredentialStatusManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;

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
public class IdRepoConfig extends IdRepoDataSourceConfig
		implements WebMvcConfigurer {

	@Value("${" + IdRepoConstants.WEB_SUB_PUBLISH_URL + "}")
	public String publisherHubURL;
	
	@Value("${mosip.idrepo.extract.template.core-pool-size:50}")
	private int corePoolSize;

	@Value("${mosip.idrepo.extract.template.max-pool-size:100}")
	private int maxPoolSize;

	@Value("${mosip.idrepo.extract.template.queue-capacity:1000}")
	private int queueCapacity;

	/** The mosip logger. */
	Logger mosipLogger = IdRepoLogger.getLogger(IdRepoConfig.class);

	/** The env. */
//	@Autowired
//	private RestTemplate restTemplate;

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

	@Bean
	@Primary
	public Executor executor() {
	    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	    executor.setCorePoolSize(Math.floorDiv(EnvUtil.getActiveAsyncThreadCount(), 4));
	    executor.setMaxPoolSize(EnvUtil.getActiveAsyncThreadCount());
	    executor.setThreadNamePrefix("idrepo-identity-");
	    executor.setWaitForTasksToCompleteOnShutdown(true);
	    executor.initialize();
	    return executor;
	}
	
	@Bean
	@Qualifier("webSubHelperExecutor")
	public Executor webSubHelperExecutor() {
	    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	    executor.setCorePoolSize(Math.floorDiv(EnvUtil.getActiveAsyncThreadCount(), 4));
	    executor.setMaxPoolSize(EnvUtil.getActiveAsyncThreadCount());
	    executor.setThreadNamePrefix("idrepo-websub-");
	    executor.setWaitForTasksToCompleteOnShutdown(true);
	    executor.initialize();
	    return executor;
	}
	
	@Bean
	@Qualifier("credentialStatusManagerJobExecutor")
	public Executor credentialStatusManagerJobExecutor() {
	    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	    executor.setCorePoolSize(Math.floorDiv(EnvUtil.getActiveAsyncThreadCount(), 4));
	    executor.setMaxPoolSize(EnvUtil.getActiveAsyncThreadCount());
	    executor.setThreadNamePrefix("idrepo-cred-status-job-");
	    executor.setWaitForTasksToCompleteOnShutdown(true);
	    executor.initialize();
	    return executor;
	}
	
	@Bean
	@Qualifier("anonymousProfileExecutor")
	public Executor anonymousProfileExecutor() {
	    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	    executor.setCorePoolSize(Math.floorDiv(EnvUtil.getActiveAsyncThreadCount(), 4));
	    executor.setMaxPoolSize(EnvUtil.getActiveAsyncThreadCount());
	    executor.setThreadNamePrefix("idrepo-identity-anonymousprofile-");
	    executor.setWaitForTasksToCompleteOnShutdown(true);
	    executor.initialize();
	    return executor;
	}
	
	@Scheduled(fixedRateString = "${" + "mosip.idrepo.monitor-thread-queue-in-ms" + ":10000}")
	public void monitorThreadQueueLimit() {
		if (StringUtils.isNotBlank(EnvUtil.getMonitorAsyncThreadQueue())) {
			ThreadPoolTaskExecutor threadPoolTaskExecutor = (ThreadPoolTaskExecutor) executor();
			ThreadPoolTaskExecutor webSubHelperExecutor = (ThreadPoolTaskExecutor) webSubHelperExecutor();
			ThreadPoolTaskExecutor credentialStatusManagerJobExecutor = (ThreadPoolTaskExecutor) credentialStatusManagerJobExecutor();
			ThreadPoolTaskExecutor anonymousProfileExecutor = (ThreadPoolTaskExecutor) anonymousProfileExecutor();
			String monitoringLog = "Thread Name : {} Thread Active Count: {} Thread Task count: {} Thread queue count: {}";
			logThreadQueueDetails(threadPoolTaskExecutor, threadPoolTaskExecutor.getThreadPoolExecutor().getQueue().size(), monitoringLog);
			logThreadQueueDetails(webSubHelperExecutor, webSubHelperExecutor.getThreadPoolExecutor().getQueue().size(), monitoringLog);
			logThreadQueueDetails(credentialStatusManagerJobExecutor, credentialStatusManagerJobExecutor.getThreadPoolExecutor().getQueue().size(), monitoringLog);
			logThreadQueueDetails(anonymousProfileExecutor, anonymousProfileExecutor.getThreadPoolExecutor().getQueue().size(), monitoringLog);
		}
	}

	private void logThreadQueueDetails(ThreadPoolTaskExecutor threadPoolTaskExecutor, int threadPoolQueueSize,
			String monitoringLog) {
		if (threadPoolQueueSize > EnvUtil.getAsyncThreadQueueThreshold())
			mosipLogger.info(monitoringLog, threadPoolTaskExecutor.getThreadNamePrefix(),
					threadPoolTaskExecutor.getActiveCount(),
					threadPoolTaskExecutor.getThreadPoolExecutor().getTaskCount(), threadPoolQueueSize);
	}
	
	/*Add commentMore actions
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
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("idrepo-");
		executor.initialize();
		return executor;
	}
}