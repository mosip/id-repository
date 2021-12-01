package io.mosip.idrepository.core.util;

import org.springframework.util.Assert;

/**
 * The Class SaltUtil.
 * 
 *  @author Loganathan S
 */
public final class SaltUtil {
	
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

}
