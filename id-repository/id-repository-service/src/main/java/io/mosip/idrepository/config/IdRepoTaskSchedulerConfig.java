package io.mosip.idrepository.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import io.mosip.idrepository.core.bootstrap.ContextClassLoaderRunnable;

/**
 * Shared {@link ThreadPoolTaskScheduler} for lightweight deferred startup work in the credential module.
 * <p>
 * Used by credential WebSub subscription scheduling and one-shot initializers that must not block
 * the main application thread. Thread factory delegates through
 * {@link ContextClassLoaderRunnable} so scheduled tasks see the same class loader as HTTP workers
 * (required when {@code KernelAuthSpringFactoriesFilteringClassLoader} is installed).
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code io.mosip.idrepository.credential.request.init.SubscribeEvent}</li>
 *   <li>{@code io.mosip.idrepository.credential.request.init.CredentialInstializer}</li>
 * </ul>
 *
 * @see io.mosip.idrepository.identity.config.IdentityWebsubConfig
 */
@Configuration
public class IdRepoTaskSchedulerConfig {

	/**
	 * Primary task scheduler with pool size 2 and graceful shutdown.
	 * <p>
	 * Marked {@code @Primary} so Spring's default scheduler injection resolves here instead of
	 * ad-hoc instances in domain modules.
	 * </p>
	 *
	 * @return initialized {@link ThreadPoolTaskScheduler}
	 */
	@Bean
	@Primary
	public ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(2);
		scheduler.setThreadNamePrefix("idrepo-task-scheduler-");
		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.setThreadFactory(ContextClassLoaderRunnable.delegatingThreadFactory(runnable -> {
			Thread thread = new Thread(runnable);
			thread.setName("idrepo-task-scheduler-" + thread.threadId());
			return thread;
		}));
		scheduler.initialize();
		return scheduler;
	}
}