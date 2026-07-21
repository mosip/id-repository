package io.mosip.idrepository.identity.service.impl;

import java.util.concurrent.TimeUnit;

import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Shared timing helper for identity {@code service.impl} public operations.
 * <p>
 * Logs wall-clock {@code durationMs} at INFO after each timed call for performance analysis.
 * </p>
 */
final class IdentityServicePerformanceLog {

	private IdentityServicePerformanceLog() {
	}

	/**
	 * Logs elapsed time since {@code startNanos}.
	 *
	 * @param logger     MOSIP logger
	 * @param className  service id / class name
	 * @param method     operation name
	 * @param startNanos {@link System#nanoTime()} captured at start
	 */
	static void log(Logger logger, String className, String method, long startNanos) {
		long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
		logger.info(IdRepoSecurityManager.getUser(), className, method,
				"Time taken for " + method + ": " + durationMs + " ms");
	}
}
