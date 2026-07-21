package io.mosip.idrepository.identity.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.mosip.idrepository.core.bootstrap.ContextClassLoaderRunnable;
import io.mosip.idrepository.core.config.IdRepoDataSourceConfig;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.manager.CredentialStatusManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;

/**
 * Identity module configuration extending the primary {@code mosip_idrepo} persistence unit.
 * <p>
 * Binds {@code mosip.idrepo.identity.*} properties, registers identity JPA repositories,
 * async executors, and credential-status scheduling support.
 * </p>
 *
 * @author Manoj SP
 * @see IdRepoDataSourceConfig
 * @see io.mosip.idrepository.config.IdRepoLibraryConfig
 * @see io.mosip.idrepository.core.jobs.IdRepoSchedulerConfiguration
 * @see IdentitySecurityConfig
 */
@Configuration
@ConfigurationProperties("mosip.idrepo.identity")
@EnableScheduling
@EnableJpaRepositories(basePackages = "io.mosip.idrepository.identity.repository")
@Import({ CredentialStatusManager.class, DummyPartnerCheckUtil.class })
public class IdRepoConfig extends IdRepoDataSourceConfig
		implements WebMvcConfigurer, SchedulingConfigurer {

	/** WebSub publisher hub URL for identity event publishing. */
	@Value("${" + IdRepoConstants.WEB_SUB_PUBLISH_URL + "}")
	/** Publisher hub url. */
	public String publisherHubURL;

	private static Logger mosipLogger = IdRepoLogger.getLogger(IdRepoConfig.class);

	/** Spring environment for property resolution. */
//	@Autowired
//	private RestTemplate restTemplate;

	/** Db. */
//	If sharding is enabled, need to uncomment
//	private Map<String, Map<String, String>> db;

	private List<String> uinStatus;

	/** Allowed bio types. */
	private List<String> allowedBioAttributes;

	/** Bio attributes. */
	private List<String> bioAttributes;

	/** Id. */
	private Map<String, String> id;

    @Value("${" + IdRepoConstants.EXTRACT_TEMPLATE_CORE_POOL_SIZE + ":50}")
    /** Core pool size. */
    private int corePoolSize;

    @Value("${" + IdRepoConstants.EXTRACT_TEMPLATE_MAX_POOL_SIZE + ":100}")
    /** Max pool size. */
    private int maxPoolSize;

    @Value("${" + IdRepoConstants.EXTRACT_TEMPLATE_QUEUE_CAPACITY + ":1000}")
    /** Queue capacity. */
    private int queueCapacity;

	/**
	 * @return db
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
	/**
	 * Allowed bio attributes.
	 * @return list<string>
	 */
	public List<String> allowedBioAttributes() {
		return Collections.unmodifiableList(allowedBioAttributes);
	}

	/**
	 * Bio attributes.
	 *
	 * @return the list
	 */
	@Bean
	/**
	 * Bio attributes.
	 * @return list<string>
	 */
	public List<String> bioAttributes() {
		return Collections.unmodifiableList(bioAttributes);
	}

	/**
	 * Status.
	 *
	 * @return the map
	 */
	@Bean
	/**
	 * Uin status.
	 * @return list<string>
	 */
	public List<String> uinStatus() {
		return Collections.unmodifiableList(uinStatus);
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
		scheduler.setThreadNamePrefix("idrepo-scheduling-");
		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		configureScheduledExecutor(scheduler);
		scheduler.initialize();
		taskRegistrar.setTaskScheduler(scheduler);
	}

	/** Primary async executor for identity operations. */
	@Bean
	@Primary
	/**
	 * Executor.
	 * @return executor
	 */
	public Executor executor() {
	    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	    executor.setCorePoolSize(Math.floorDiv(EnvUtil.getActiveAsyncThreadCount(), 4));
	    executor.setMaxPoolSize(EnvUtil.getActiveAsyncThreadCount());
	    executor.setThreadNamePrefix("idrepo-identity-");
	    executor.setWaitForTasksToCompleteOnShutdown(true);
	    configureThreadPoolExecutor(executor);
	    executor.initialize();
	    return executor;
	}

	/** Executor for WebSub publish/subscribe helper tasks. */
	@Bean
	@Qualifier("webSubHelperExecutor")
	/**
	 * Web sub helper executor.
	 * @return executor
	 */
	public Executor webSubHelperExecutor() {
	    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	    executor.setCorePoolSize(Math.floorDiv(EnvUtil.getActiveAsyncThreadCount(), 4));
	    executor.setMaxPoolSize(EnvUtil.getActiveAsyncThreadCount());
	    executor.setThreadNamePrefix("idrepo-websub-");
	    executor.setWaitForTasksToCompleteOnShutdown(true);
	    configureThreadPoolExecutor(executor);
	    executor.initialize();
	    return executor;
	}

	/** Executor for the credential status scheduled job. */
	@Bean
	@Qualifier("credentialStatusManagerJobExecutor")
	/**
	 * Credential status manager job executor.
	 * @return executor
	 */
	public Executor credentialStatusManagerJobExecutor() {
	    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	    executor.setCorePoolSize(Math.floorDiv(EnvUtil.getActiveAsyncThreadCount(), 4));
	    executor.setMaxPoolSize(EnvUtil.getActiveAsyncThreadCount());
	    executor.setThreadNamePrefix("idrepo-cred-status-job-");
	    executor.setWaitForTasksToCompleteOnShutdown(true);
	    configureThreadPoolExecutor(executor);
	    executor.initialize();
	    return executor;
	}

	/** Executor for anonymous profile (IOV) async builds. */
	@Bean
	@Qualifier("anonymousProfileExecutor")
	/**
	 * Anonymous profile executor.
	 * @return executor
	 */
	public Executor anonymousProfileExecutor() {
	    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	    executor.setCorePoolSize(Math.floorDiv(EnvUtil.getActiveAsyncThreadCount(), 4));
	    executor.setMaxPoolSize(EnvUtil.getActiveAsyncThreadCount());
	    executor.setThreadNamePrefix("idrepo-identity-anonymousprofile-");
	    executor.setWaitForTasksToCompleteOnShutdown(true);
	    configureThreadPoolExecutor(executor);
	    executor.initialize();
	    return executor;
	}

	private void configureThreadPoolExecutor(ThreadPoolTaskExecutor executor) {
		configureScheduledExecutor(executor);
	}

	private void configureScheduledExecutor(org.springframework.scheduling.concurrent.ExecutorConfigurationSupport executor) {
		String prefix = executor.getThreadNamePrefix();
		executor.setThreadFactory(ContextClassLoaderRunnable.delegatingThreadFactory(runnable -> {
			Thread thread = new Thread(runnable);
			if (prefix != null) {
				thread.setName(prefix + thread.threadId());
			}
			return thread;
		}));
	}

	/** Logs thread-pool queue depth when it exceeds the configured threshold. */
	@Scheduled(fixedRateString = "${" + IdRepoConstants.MONITOR_THREAD_QUEUE_IN_MS + ":10000}")
	/**
	 * Monitor thread queue limit.
	 */
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

	/*
	 * This bean is returned because for async task the security context needs to be
	 * passed.
	 *
	 */
	@Bean("withSecurityContext")
	/**
	 * Task executor.
	 * @return delegating security context async task executor
	 */
	public DelegatingSecurityContextAsyncTaskExecutor taskExecutor() {
		return new DelegatingSecurityContextAsyncTaskExecutor(threadPoolTaskExecutor());
	}

	private ThreadPoolTaskExecutor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("idrepo-");
		configureThreadPoolExecutor(executor);
		executor.initialize();
		return executor;
	}
}
