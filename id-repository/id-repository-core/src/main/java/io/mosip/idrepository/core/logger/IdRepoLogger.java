package io.mosip.idrepository.core.logger;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.factory.Logfactory;

/**
 * Logger for IdRepo which provides implementation from kernel logback.
 * 
 * @author Manoj SP
 *
 */
public final class IdRepoLogger {


	/**
	 * Instantiates a new id repo logger.
	 */
	private IdRepoLogger() {
	}

	/**
	 * Method to get the logger for the class provided.
	 *
	 * @param clazz the clazz
	 * @return the logger
	 */
	public static Logger getLogger(Class<?> clazz) {
		return Logfactory.getSlf4jLogger(clazz);
	}
}
