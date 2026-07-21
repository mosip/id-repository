package io.mosip.idrepository.core.logger;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.factory.Logfactory;

/**
 * Factory for obtaining structured SLF4J loggers across ID Repository modules.
 * <p>
 * Wraps the MOSIP kernel {@link Logfactory} to provide a single entry point for
 * consistent log configuration. All core classes should obtain their logger via
 * {@link #getLogger(Class)} rather than calling {@code Logfactory} directly.
 * </p>
 *
 * @see io.mosip.kernel.logger.logback.factory.Logfactory
 * @see io.mosip.kernel.core.logger.spi.Logger
 *
 * @author Manoj SP
 */
public final class IdRepoLogger {

	/**
	 * Private constructor — utility class, not instantiable.
	 */
	private IdRepoLogger() {
	}

	/**
	 * Returns an SLF4J logger bound to the given class name.
	 *
	 * @param clazz the class requesting a logger; used as the log category
	 * @return configured {@link Logger} instance for structured MOSIP logging
	 */
	public static Logger getLogger(Class<?> clazz) {
		return Logfactory.getSlf4jLogger(clazz);
	}
}