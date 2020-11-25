package io.mosip.bioextractor.logger;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.appender.RollingFileAppender;
import io.mosip.kernel.logger.logback.factory.Logfactory;

/**
 * Logger for IDA which provides implementation from kernel logback.
 * 
 * @author Manoj SP
 *
 */
public final class BioExtractorLogger {


	/**
	 * Instantiates a new ida logger.
	 */
	private BioExtractorLogger() {
	}

	/**
	 * Method to get the logger for the class provided.
	 *
	 * @param clazz
	 *            the clazz
	 * @return the logger
	 */
	public static Logger getLogger(Class<?> clazz) {
		return Logfactory.getSlf4jLogger(clazz);
	}
}
