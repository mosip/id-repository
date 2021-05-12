package io.mosip.idrepository.credentialsfeeder.logger;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.factory.Logfactory;

/**
 * The Class IdRepoLogger.
 *
 * @author Manoj SP
 */
public class IdRepoLogger {



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
