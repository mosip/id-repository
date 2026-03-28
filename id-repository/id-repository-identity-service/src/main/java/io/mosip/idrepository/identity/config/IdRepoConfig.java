package io.mosip.idrepository.identity.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
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
 * Central application configuration for the ID Repository identity service.
 *
 * <h3>Threading model</h3>
 * <pre>
 *  Bean name                        Prefix                      Purpose
 *  ─────────────────────────────────────────────────────────────────────
 *  executor (primary)               idrepo-identity-            General async tasks
 *  webSubHelperExecutor             idrepo-websub-              WebSub publish/receive
 *  credentialStatusManagerJobExec   idrepo-cred-status-job-     Credential status jobs
 *  anonymousProfileExecutor         idrepo-anon-profile-        Anonymous profile builds
 *  withSecurityContext              idrepo-sec-ctx-             @Async tasks that need
 *                                                               security context propagation
 * </pre>
 *
 * <p>Every pool is sized independently via properties so that ops teams can
 * tune each workload without touching code.  All pools are registered as Spring
 * beans so that Spring can call {@code shutdown()} on them during graceful
 * termination, and so that the monitoring scheduler can inject the live
 * instances rather than creating fresh ones on every tick.
 *
 * @author Manoj SP (original)
 */
@Configuration
@ConfigurationProperties("mosip.idrepo.identity")
@EnableScheduling
@EnableJpaRepositories(basePackages = "io.mosip.idrepository.*")
@Import({ CredentialStatusManager.class, DummyPartnerCheckUtil.class })
public class IdRepoConfig extends IdRepoDataSourceConfig implements WebMvcConfigurer {

	// ------------------------------------------------------------------ //
	//  Logger                                                              //
	// ------------------------------------------------------------------ //

	private static final Logger mosipLogger = IdRepoLogger.getLogger(IdRepoConfig.class);

	// ------------------------------------------------------------------ //
	//  WebSub                                                              //
	// ------------------------------------------------------------------ //

	@Value("${" + IdRepoConstants.WEB_SUB_PUBLISH_URL + "}")
	public String publisherHubURL;

	// ------------------------------------------------------------------ //
	//  ConfigurationProperties-bound fields                               //
	// ------------------------------------------------------------------ //

	private List<String> uinStatus;
	private List<String> allowedBioAttributes;
	private List<String> bioAttributes;
	private Map<String, String> id;

	// ------------------------------------------------------------------ //
	//  Thread-pool sizing — primary / websub / cred-status / anon pools   //
	//  Each pool has its own core/max/queue properties so that each        //
	//  workload can be tuned independently in the config server.           //
	// ------------------------------------------------------------------ //

	// Primary general-purpose pool
	@Value("${mosip.idrepo.executor.core-pool-size:#{T(io.mosip.idrepository.core.util.EnvUtil).getActiveAsyncThreadCount() / 4}}")
	private int executorCorePoolSize;

	@Value("${mosip.idrepo.executor.max-pool-size:#{T(io.mosip.idrepository.core.util.EnvUtil).getActiveAsyncThreadCount()}}")
	private int executorMaxPoolSize;

	@Value("${mosip.idrepo.executor.queue-capacity:100}")
	private int executorQueueCapacity;

	@Value("${mosip.idrepo.executor.await-termination-seconds:30}")
	private int executorAwaitTerminationSeconds;

	// WebSub helper pool
	@Value("${mosip.idrepo.websub-executor.core-pool-size:#{T(io.mosip.idrepository.core.util.EnvUtil).getActiveAsyncThreadCount() / 4}}")
	private int webSubCorePoolSize;

	@Value("${mosip.idrepo.websub-executor.max-pool-size:#{T(io.mosip.idrepository.core.util.EnvUtil).getActiveAsyncThreadCount()}}")
	private int webSubMaxPoolSize;

	@Value("${mosip.idrepo.websub-executor.queue-capacity:100}")
	private int webSubQueueCapacity;

	@Value("${mosip.idrepo.websub-executor.await-termination-seconds:30}")
	private int webSubAwaitTerminationSeconds;

	// Credential status job pool
	@Value("${mosip.idrepo.cred-status-executor.core-pool-size:#{T(io.mosip.idrepository.core.util.EnvUtil).getActiveAsyncThreadCount() / 4}}")
	private int credStatusCorePoolSize;

	@Value("${mosip.idrepo.cred-status-executor.max-pool-size:#{T(io.mosip.idrepository.core.util.EnvUtil).getActiveAsyncThreadCount()}}")
	private int credStatusMaxPoolSize;

	@Value("${mosip.idrepo.cred-status-executor.queue-capacity:100}")
	private int credStatusQueueCapacity;

	@Value("${mosip.idrepo.cred-status-executor.await-termination-seconds:60}")
	private int credStatusAwaitTerminationSeconds;

	// Anonymous profile pool
	@Value("${mosip.idrepo.anon-profile-executor.core-pool-size:#{T(io.mosip.idrepository.core.util.EnvUtil).getActiveAsyncThreadCount() / 4}}")
	private int anonProfileCorePoolSize;

	@Value("${mosip.idrepo.anon-profile-executor.max-pool-size:#{T(io.mosip.idrepository.core.util.EnvUtil).getActiveAsyncThreadCount()}}")
	private int anonProfileMaxPoolSize;

	@Value("${mosip.idrepo.anon-profile-executor.queue-capacity:100}")
	private int anonProfileQueueCapacity;

	@Value("${mosip.idrepo.anon-profile-executor.await-termination-seconds:30}")
	private int anonProfileAwaitTerminationSeconds;

	// Security-context-propagating pool  (used by @Async("withSecurityContext"))
	@Value("${mosip.idrepo.extract.template.core-pool-size:20}")
	private int secCtxCorePoolSize;

	@Value("${mosip.idrepo.extract.template.max-pool-size:50}")
	private int secCtxMaxPoolSize;

	@Value("${mosip.idrepo.extract.template.queue-capacity:100}")
	private int secCtxQueueCapacity;

	@Value("${mosip.idrepo.extract.template.await-termination-seconds:60}")
	private int secCtxAwaitTerminationSeconds;

	// ------------------------------------------------------------------ //
	//  ConfigurationProperties setters                                     //
	// ------------------------------------------------------------------ //

	public void setUinStatus(List<String> uinStatus)                         { this.uinStatus = uinStatus; }
	public void setId(Map<String, String> id)                                 { this.id = id; }
	public void setAllowedBioAttributes(List<String> allowedBioAttributes)   { this.allowedBioAttributes = allowedBioAttributes; }
	public void setBioAttributes(List<String> bioAttributes)                  { this.bioAttributes = bioAttributes; }

	// ------------------------------------------------------------------ //
	//  Domain-value beans                                                  //
	// ------------------------------------------------------------------ //

	@Bean
	public Map<String, String> id() {
		return Collections.unmodifiableMap(id);
	}

	@Bean
	public List<String> allowedBioAttributes() {
		return Collections.unmodifiableList(allowedBioAttributes);
	}

	@Bean
	public List<String> bioAttributes() {
		return Collections.unmodifiableList(bioAttributes);
	}

	@Bean
	public List<String> uinStatus() {
		return Collections.unmodifiableList(uinStatus);
	}

	// ------------------------------------------------------------------ //
	//  Executor beans                                                       //
	//                                                                       //
	//  Each pool is a Spring-managed bean so that:                          //
	//   • Spring calls destroy() / shutdown() during graceful termination.  //
	//   • The monitoring scheduler can inject the live instance by name      //
	//     rather than creating a fresh pool on every tick (the original bug)//
	//   • Naming is unambiguous in thread dumps and metrics.                //
	// ------------------------------------------------------------------ //

	/**
	 * Primary general-purpose async executor.
	 *
	 * <p>Marked {@code @Primary} so that Spring injects it wherever an
	 * {@link Executor} or {@link org.springframework.core.task.TaskExecutor}
	 * is required without an explicit qualifier.
	 */
	@Bean
	@Primary
	public ThreadPoolTaskExecutor executor() {
		return buildExecutor(
				"idrepo-identity-",
				executorCorePoolSize,
				executorMaxPoolSize,
				executorQueueCapacity,
				executorAwaitTerminationSeconds
		);
	}

	/** Executor for WebSub publish/receive operations. */
	@Bean
	@Qualifier("webSubHelperExecutor")
	public ThreadPoolTaskExecutor webSubHelperExecutor() {
		return buildExecutor(
				"idrepo-websub-",
				webSubCorePoolSize,
				webSubMaxPoolSize,
				webSubQueueCapacity,
				webSubAwaitTerminationSeconds
		);
	}

	/** Executor for credential-status background jobs. */
	@Bean
	@Qualifier("credentialStatusManagerJobExecutor")
	public ThreadPoolTaskExecutor credentialStatusManagerJobExecutor() {
		return buildExecutor(
				"idrepo-cred-status-job-",
				credStatusCorePoolSize,
				credStatusMaxPoolSize,
				credStatusQueueCapacity,
				credStatusAwaitTerminationSeconds
		);
	}

	/** Executor for anonymous-profile builds. */
	@Bean
	@Qualifier("anonymousProfileExecutor")
	public ThreadPoolTaskExecutor anonymousProfileExecutor() {
		return buildExecutor(
				"idrepo-anon-profile-",
				anonProfileCorePoolSize,
				anonProfileMaxPoolSize,
				anonProfileQueueCapacity,
				anonProfileAwaitTerminationSeconds
		);
	}

	/**
	 * Security-context-propagating executor used by
	 * {@code @Async("withSecurityContext")}.
	 *
	 * <p>Wraps a {@link ThreadPoolTaskExecutor} with
	 * {@link DelegatingSecurityContextAsyncTaskExecutor} so that the
	 * Spring Security context is copied to each worker thread.
	 *
	 * <p>The inner executor is itself a Spring bean
	 * ({@code "securityContextDelegateExecutor"}) so that Spring manages
	 * its lifecycle independently and it appears in Actuator thread-pool
	 * metrics.
	 */
	@Bean("withSecurityContext")
	public DelegatingSecurityContextAsyncTaskExecutor taskExecutor(
			@Qualifier("securityContextDelegateExecutor") ThreadPoolTaskExecutor delegate) {
		return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
	}

	/**
	 * The inner {@link ThreadPoolTaskExecutor} wrapped by
	 * {@code "withSecurityContext"}.
	 *
	 * <p>Kept as a separate bean so that the monitoring scheduler can
	 * inject it directly and read live metrics from it.
	 */
	@Bean("securityContextDelegateExecutor")
	public ThreadPoolTaskExecutor securityContextDelegateExecutor() {
		return buildExecutor(
				"idrepo-sec-ctx-",
				secCtxCorePoolSize,
				secCtxMaxPoolSize,
				secCtxQueueCapacity,
				secCtxAwaitTerminationSeconds
		);
	}

	// ------------------------------------------------------------------ //
	//  Thread-queue monitoring                                             //
	//                                                                      //
	//  Injects the live bean instances rather than calling the factory     //
	//  methods directly. The original code called executor() etc. as plain //
	//  Java methods, bypassing Spring's bean proxy, which created new      //
	//  empty executor instances on every tick — so the monitor was always  //
	//  reading metrics from freshly-created, zero-task pools.              //
	// ------------------------------------------------------------------ //

	@Autowired
	@Qualifier("executor")
	private ThreadPoolTaskExecutor primaryExecutor;

	@Autowired
	@Qualifier("webSubHelperExecutor")
	private ThreadPoolTaskExecutor injectedWebSubExecutor;

	@Autowired
	@Qualifier("credentialStatusManagerJobExecutor")
	private ThreadPoolTaskExecutor injectedCredStatusExecutor;

	@Autowired
	@Qualifier("anonymousProfileExecutor")
	private ThreadPoolTaskExecutor injectedAnonProfileExecutor;

	@Autowired
	@Qualifier("securityContextDelegateExecutor")
	private ThreadPoolTaskExecutor injectedSecCtxExecutor;

	/**
	 * Logs a warning when any pool's queue depth exceeds the configured
	 * threshold.  Executes on a fixed-rate schedule so that ops teams can
	 * detect saturation before tasks start being rejected.
	 *
	 * <p>Controlled by {@code mosip.idrepo.monitor-thread-queue-in-ms}
	 * (default 10 000 ms) and {@code mosip.idrepo.monitor-thread-queue}
	 * (set to a non-blank value to enable).
	 */
	@Scheduled(fixedRateString = "${mosip.idrepo.monitor-thread-queue-in-ms:10000}")
	public void monitorThreadQueueLimit() {
		if (!StringUtils.isNotBlank(EnvUtil.getMonitorAsyncThreadQueue())) {
			return;
		}
		logThreadQueueDetails(primaryExecutor);
		logThreadQueueDetails(injectedWebSubExecutor);
		logThreadQueueDetails(injectedCredStatusExecutor);
		logThreadQueueDetails(injectedAnonProfileExecutor);
		logThreadQueueDetails(injectedSecCtxExecutor);
	}

	// ------------------------------------------------------------------ //
	//  Private helpers                                                     //
	// ------------------------------------------------------------------ //

	/**
	 * Builds a fully configured {@link ThreadPoolTaskExecutor} with:
	 * <ul>
	 *   <li>A {@link } rejection handler so that, when the
	 *       queue is full, the submitting thread executes the task itself
	 *       rather than throwing {@link java.util.concurrent.RejectedExecutionException}.
	 *       This applies natural back-pressure without dropping work.</li>
	 *   <li>Graceful shutdown with a configurable termination timeout so
	 *       that in-progress tasks are not interrupted on application stop.</li>
	 * </ul>
	 */
	private ThreadPoolTaskExecutor buildExecutor(
			String threadNamePrefix,
			int corePoolSize,
			int maxPoolSize,
			int queueCapacity,
			int awaitTerminationSeconds) {

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix(threadNamePrefix);
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(awaitTerminationSeconds);

		/*
		 * CallerRunsPolicy: when the queue is full the calling thread runs
		 * the task itself.  This throttles the producer naturally and
		 * prevents silent task loss — much safer than the default AbortPolicy
		 * which throws RejectedExecutionException without any warning.
		 */
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		executor.initialize();

		mosipLogger.info("Initialized thread pool | prefix={} core={} max={} queue={}",
				threadNamePrefix, corePoolSize, maxPoolSize, queueCapacity);

		return executor;
	}

	/**
	 * Logs pool metrics when the queue depth exceeds the configured threshold.
	 */
	private void logThreadQueueDetails(ThreadPoolTaskExecutor pool) {
		int queueSize = pool.getThreadPoolExecutor().getQueue().size();
		if (queueSize > EnvUtil.getAsyncThreadQueueThreshold()) {
			mosipLogger.warn(
					"Thread pool queue approaching capacity | prefix={} active={} tasks={} queued={}",
					pool.getThreadNamePrefix(),
					pool.getActiveCount(),
					pool.getThreadPoolExecutor().getTaskCount(),
					queueSize);
		}
	}
}