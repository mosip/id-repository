package io.mosip.idrepository.core.util;

import java.security.NoSuchAlgorithmException;

import org.springframework.util.Assert;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.HMACUtils2;

/**
 * The Class SaltUtil.
 * 
 *  @author Loganathan S
 */
public final class SaltUtil {
	
	private static Logger mosipLogger = IdRepoLogger.getLogger(RestHelper.class);

	
	/**
	 * Instantiates a new salt util.
	 */
	private SaltUtil() {}
	
	/**
	 * A modulo function for fast performance for Individual ID assuming that the divisor
	 * won't be in negative.
	 *
	 * @param idvid the idvid
	 * @param substrigLen the substrig len
	 * @return the modulo
	 */
	public static final int getIdvidModulo(String idvid, int substrigLen) {
		Assert.isTrue(substrigLen > 0, "divisor should be positive integer");
		int length = idvid.length();
		return length <= substrigLen ? Integer.parseInt(idvid)
				: Integer.parseInt(idvid.substring(length - substrigLen));
	}
	
	/**
	 * A modulo function for fast performance for hash of Individual ID assuming that the divisor
	 * won't be in negative.
	 *
	 * @param idvid the idvid
	 * @param substrigLen the substrig len
	 * @return the modulo
	 */
	public static final int getIdvidHashModulo(String idvid, int substrigLen) {
		Assert.isTrue(substrigLen > 0, "divisor should be positive integer");
		
		try {
			String idPlainHash = HMACUtils2.digestAsPlainText(idvid.getBytes());
			int hexToDecimal = getSubstrinInt(idPlainHash, substrigLen, 16);
			String decimalStr = String.valueOf(hexToDecimal);
			return getSubstrinInt(decimalStr, substrigLen, 10);
		} catch (NoSuchAlgorithmException e) {
			mosipLogger.warn("UNKNOWN_ERROR %s " , ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}

	private static int getSubstrinInt(String idvid, int substrigLen, int radix) {
		String hexSubstring = getSubstring(idvid, substrigLen);
		return Integer.parseInt(hexSubstring, radix);
	}

	private static String getSubstring(String string, int substrigLen) {
		int length = string.length();
		return length > substrigLen ? string.substring(length - substrigLen) : string;
	}

}
