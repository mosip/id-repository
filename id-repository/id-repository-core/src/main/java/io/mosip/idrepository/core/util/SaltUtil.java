package io.mosip.idrepository.core.util;

import java.security.NoSuchAlgorithmException;

import org.springframework.util.Assert;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.HMACUtils2;

/**
 * Static helpers for deriving salt-bucket indices from UIN, VID, or other individual
 * identifiers.
 *
 * <p>
 * ID Repository shards cryptographic salt lookups by taking a numeric suffix of the
 * identifier (or a hash-derived suffix). The resulting integer indexes
 * {@code uin_hash_salt} / {@code uin_encrypt_salt} rows. These helpers avoid full
 * {@code BigInteger} modulo on hot encrypt/decrypt paths in {@link IdRepoSecurityManager}.
 * </p>
 *
 * <h2>Routing modes</h2>
 * <ul>
 *   <li>{@link #getIdvidModulo(String, int)} — trailing decimal digits of the raw idvid</li>
 *   <li>{@link #getIdvidHashModulo(String, int)} — trailing digits derived from HMAC of the
 *       idvid (when the raw ID must not drive the bucket key)</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>
 * Callers typically pass {@link EnvUtil#getIdrepoSaltKeyLength()} as
 * {@code substringLen} (property {@code mosip.idrepo.salt.key.length}).
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * int bucket = SaltUtil.getIdvidModulo(uin, EnvUtil.getIdrepoSaltKeyLength());
 * int hashedBucket = SaltUtil.getIdvidHashModulo(uin, EnvUtil.getIdrepoSaltKeyLength());
 * </pre>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * This class is part of the published {@code id-repository-core} API surface referenced by
 * ID Authentication tooling. Do not rename public methods without an IDA-coordinated release.
 * IDA itself does <strong>not</strong> read id-repo salt tables.
 * </p>
 *
 * @author Loganathan S
 * @see IdRepoSecurityManager
 * @see EnvUtil#getIdrepoSaltKeyLength()
 * @see HMACUtils2
 */
public final class SaltUtil {

	/** Logger for unexpected digest algorithm failures. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(SaltUtil.class);

	/**
	 * Prevents instantiation; use static helpers only.
	 */
	private SaltUtil() {}

	/**
	 * Derives a numeric salt-bucket index from the trailing digits of {@code idvid}.
	 * <p>
	 * When {@code idvid} is longer than {@code substringLen}, the rightmost
	 * {@code substringLen} characters are parsed as a base-10 integer. Shorter identifiers
	 * are parsed in full. Used for direct (non-hashed) salt routing.
	 * </p>
	 *
	 * @param idvid        individual identifier (UIN or VID) as a decimal string; must be
	 *                     numeric for the chosen suffix
	 * @param substringLen number of trailing digits to use; must be a positive integer
	 * @return integer derived from the suffix of {@code idvid}, suitable as a salt-table
	 *         index input
	 * @throws IllegalArgumentException if {@code substringLen} is not positive
	 * @throws NumberFormatException    if the selected suffix is not a valid decimal integer
	 * @see IdRepoSecurityManager#getSaltKeyForId(String)
	 * @see #getIdvidHashModulo(String, int)
	 */
	public static int getIdvidModulo(String idvid, int substringLen) {
		Assert.isTrue(substringLen > 0, "divisor should be positive integer");
		int length = idvid.length();
		return length <= substringLen ? Integer.parseInt(idvid)
				: Integer.parseInt(idvid.substring(length - substringLen));
	}

	/**
	 * Derives a numeric salt-bucket index from the HMAC digest of {@code idvid}.
	 * <p>
	 * Algorithm:
	 * </p>
	 * <ol>
	 *   <li>HMAC-digest {@code idvid} bytes via {@link HMACUtils2#digestAsPlainText(byte[])}</li>
	 *   <li>Parse the trailing {@code substringLen} hex characters as base-16</li>
	 *   <li>Convert that integer to decimal text and again take the trailing
	 *       {@code substringLen} decimal digits</li>
	 * </ol>
	 * <p>
	 * Used when the raw ID must not appear in salt-key derivation (hashed salt routing).
	 * </p>
	 *
	 * @param idvid        individual identifier (UIN or VID)
	 * @param substringLen number of trailing hex/decimal characters to use; must be positive
	 * @return integer bucket index derived from the hashed identifier
	 * @throws IllegalArgumentException    if {@code substringLen} is not positive
	 * @throws IdRepoAppUncheckedException with {@link IdRepoErrorConstants#UNKNOWN_ERROR} when
	 *                                     the digest algorithm is unavailable
	 * @see #getIdvidModulo(String, int)
	 * @see IdRepoSecurityManager#getSaltKeyForHashOfId(String)
	 */
	public static int getIdvidHashModulo(String idvid, int substringLen) {
		Assert.isTrue(substringLen > 0, "divisor should be positive integer");
		
		try {
			String idPlainHash = HMACUtils2.digestAsPlainText(idvid.getBytes());
			int hexToDecimal = getSubstringInt(idPlainHash, substringLen, 16);
			String decimalStr = String.valueOf(hexToDecimal);
			return getSubstringInt(decimalStr, substringLen, 10);
		} catch (NoSuchAlgorithmException e) {
			mosipLogger.warn("UNKNOWN_ERROR %s " , ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}

	/**
	 * Parses the trailing {@code substringLen} characters of {@code idvid} using
	 * {@code radix}.
	 *
	 * @param idvid        source string (hex digest or decimal text)
	 * @param substringLen maximum suffix length
	 * @param radix        numeric radix for {@link Integer#parseInt(String, int)}
	 * @return parsed integer value of the suffix
	 */
	private static int getSubstringInt(String idvid, int substringLen, int radix) {
		String hexSubstring = getSubstring(idvid, substringLen);
		return Integer.parseInt(hexSubstring, radix);
	}

	/**
	 * Returns the rightmost {@code substringLen} characters of {@code string}, or the
	 * whole string when shorter.
	 *
	 * @param string       source text
	 * @param substringLen maximum suffix length
	 * @return suffix substring (never {@code null} if {@code string} is non-null)
	 */
	private static String getSubstring(String string, int substringLen) {
		int length = string.length();
		return length > substringLen ? string.substring(length - substringLen) : string;
	}
}
