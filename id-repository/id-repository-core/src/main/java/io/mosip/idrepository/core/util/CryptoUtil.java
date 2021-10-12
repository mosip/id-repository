package io.mosip.idrepository.core.util;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * @author Manoj SP
 * 
 * Temporary overridden CryptoUtil class of Kernel Core.
 */
public class CryptoUtil {
	
	private static Logger mosipLogger = IdRepoLogger.getLogger(CryptoUtil.class);

	public static String encodeToURLSafeBase64(byte[] data) {
		return io.mosip.kernel.core.util.CryptoUtil.encodeToURLSafeBase64(data);
	}

	public static byte[] decodeURLSafeBase64(String data) {
		try {
			return io.mosip.kernel.core.util.CryptoUtil.decodeURLSafeBase64(data);
		} catch (Exception e) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), "CryptoUtil",
					"FAILED TO DECODE USING URL SAFE DECODER. PROCEEDING WITH PLAIN DECODER.",
					ExceptionUtils.getStackTrace(e));
			return io.mosip.kernel.core.util.CryptoUtil.decodePlainBase64(data);
		}
	}
}
