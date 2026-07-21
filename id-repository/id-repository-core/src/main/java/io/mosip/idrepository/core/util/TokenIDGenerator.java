package io.mosip.idrepository.core.util;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.HMACUtils2;

/**
 * Generates opaque, deterministic token IDs for credential WebSub notifications.
 *
 * <p>
 * Partners correlate credential / auth-type events using a token ID instead of the raw
 * UIN. The token is derived by a double HMAC chain and truncated to a configured length so
 * WebSub payloads never expose the resident identifier.
 * </p>
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>{@code uinHash = HMAC(uin + uinSalt)}</li>
 *   <li>{@code hash = HMAC(partnerCodeSalt + partnerCode + uinHash)}</li>
 *   <li>{@code token = BigInteger(hashBytes).toString().substring(0, tokenIDLength)}</li>
 * </ol>
 * <p>
 * The same {@code (uin, partnerCode)} pair always yields the same token for a given
 * deployment’s salt and length configuration.
 * </p>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li>{@link IdRepoConstants#KERNEL_TOKENID_UIN_SALT} —
 *       {@code mosip.kernel.tokenid.uin.salt}</li>
 *   <li>{@link IdRepoConstants#KERNEL_TOKENID_PARTNERCODE_SALT} —
 *       {@code mosip.kernel.tokenid.partnercode.salt}</li>
 *   <li>{@link IdRepoConstants#KERNEL_TOKENID_LENGTH} —
 *       {@code mosip.kernel.tokenid.length}</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * String tokenId = tokenIDGenerator.generateTokenID(uin, partnerId);
 * // embed in AuthTypeStatusEventDTO / IDA WebSub event data
 * </pre>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link IdRepoWebSubHelper#publishAuthTypeStatusUpdateEvent} — partner-scoped
 *       token in auth-type status events</li>
 *   <li>Credential / IDA notification paths that must not leak UIN</li>
 * </ul>
 *
 * @see IdRepoWebSubHelper
 * @see HMACUtils2
 * @see IdRepoConstants#KERNEL_TOKENID_UIN_SALT
 */
@Component
public class TokenIDGenerator {

	/** Logger for digest algorithm failures. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(TokenIDGenerator.class);

	/**
	 * Salt appended to the UIN before the first HMAC digest.
	 * <p>
	 * Config key: {@code mosip.kernel.tokenid.uin.salt}.
	 * </p>
	 */
	@Value("${" + IdRepoConstants.KERNEL_TOKENID_UIN_SALT + "}")
	private String uinSalt;

	/**
	 * Length of the returned token ID string after truncation.
	 * <p>
	 * Config key: {@code mosip.kernel.tokenid.length}.
	 * </p>
	 */
	@Value("${" + IdRepoConstants.KERNEL_TOKENID_LENGTH + "}")
	private int tokenIDLength;

	/**
	 * Salt prefixed to the partner code before the second HMAC digest.
	 * <p>
	 * Config key: {@code mosip.kernel.tokenid.partnercode.salt}.
	 * </p>
	 */
	@Value("${" + IdRepoConstants.KERNEL_TOKENID_PARTNERCODE_SALT + "}")
	private String partnerCodeSalt;

	/**
	 * Generates a deterministic token ID for the given UIN and partner code.
	 * <p>
	 * See class-level documentation for the full hash chain. Truncation uses
	 * {@link #tokenIDLength}; callers must ensure the decimal string is at least that long
	 * for the configured salts (standard MOSIP deployments satisfy this).
	 * </p>
	 *
	 * @param uin         resident UIN used in the hash chain; must not be {@code null}
	 * @param partnerCode partner identifier (MISP / OLV partner code); must not be
	 *                    {@code null}
	 * @return token ID string of length {@link #tokenIDLength}
	 * @throws IdRepoAppUncheckedException with {@link IdRepoErrorConstants#UNKNOWN_ERROR}
	 *                                     when the HMAC digest algorithm is unavailable
	 * @throws StringIndexOutOfBoundsException if the decimal digest is shorter than
	 *                                         {@link #tokenIDLength} (misconfigured length)
	 */
	public String generateTokenID(String uin, String partnerCode) {
		try {
			String uinHash = digestAsPlainText((uin + uinSalt).getBytes());
			String hash = digestAsPlainText((partnerCodeSalt + partnerCode + uinHash).getBytes());
			return new BigInteger(hash.getBytes()).toString().substring(0, tokenIDLength);
		} catch (NoSuchAlgorithmException e) {
			mosipLogger.warn("UNKNOWN_ERROR %s ", ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}

	/**
	 * HMAC-digests {@code data} as plain hex text via {@link HMACUtils2}.
	 * <p>
	 * Package-visible for unit tests that stub or spy the digest step.
	 * </p>
	 *
	 * @param data bytes to digest
	 * @return hexadecimal digest string
	 * @throws NoSuchAlgorithmException if the configured digest algorithm is unavailable
	 */
	String digestAsPlainText(byte[] data) throws NoSuchAlgorithmException {
		return HMACUtils2.digestAsPlainText(data);
	}
}