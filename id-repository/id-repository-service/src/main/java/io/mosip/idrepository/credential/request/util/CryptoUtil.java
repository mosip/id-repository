package io.mosip.idrepository.credential.request.util;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.credential.request.constant.CredentialRequestErrorCodes;
import io.mosip.idrepository.credential.request.exception.CredentialRequestGeneratorUncheckedException;
import io.mosip.idrepository.credential.request.interceptor.CredentialTransactionInterceptor;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;

/**
 * Encrypts and decrypts credential queue payloads via kernel cryptomanager.
 * <p>
 * Uses {@link IdRepoSecurityManager} (self-token {@link io.mosip.idrepository.core.helper.RestHelper})
 * instead of legacy {@link CredReqRestUtil} cookie token flow. Used by
 * {@link CredentialTransactionInterceptor} for at-rest protection of
 * {@code mosip_credential.credential_transaction.request} rows.
 * </p>
 */
@Component("credReqCryptoUtil")
public class CryptoUtil {

	@Autowired
	private IdRepoSecurityManager securityManager;

	/**
	 * Decrypts URL-safe Base64 ciphertext returned from the database.
	 *
	 * @param data encrypted payload from {@code credential_transaction.request}
	 * @return URL-safe Base64 of decrypted plaintext (consumed by the Hibernate interceptor)
	 */
	public String decryptData(String data) {
		try {
			byte[] plainBytes = securityManager.decrypt(data.getBytes(StandardCharsets.UTF_8),
					EnvUtil.getCredCryptoRefId());
			return io.mosip.kernel.core.util.CryptoUtil.encodeToURLSafeBase64(plainBytes);
		} catch (IdRepoAppException e) {
			IdRepoLogger.getLogger(CredentialTransactionInterceptor.class)
					.error(ExceptionUtils.getStackTrace(e));
			throw new CredentialRequestGeneratorUncheckedException(
					CredentialRequestErrorCodes.ENCRYPTION_DECRYPTION_FAILED, e);
		}
	}

	/**
	 * Encrypts URL-safe Base64-encoded plaintext for database storage.
	 *
	 * @param data URL-safe Base64-encoded bytes to encrypt
	 * @return ciphertext suitable for the {@code request} column
	 */
	public String encryptData(String data) {
		try {
			byte[] cipherBytes = securityManager.encrypt(
					io.mosip.kernel.core.util.CryptoUtil.decodeURLSafeBase64(data),
					EnvUtil.getCredCryptoRefId());
			return new String(cipherBytes, StandardCharsets.UTF_8);
		} catch (IdRepoAppException e) {
			IdRepoLogger.getLogger(CredentialTransactionInterceptor.class)
					.error(ExceptionUtils.getStackTrace(e));
			throw new CredentialRequestGeneratorUncheckedException(
					CredentialRequestErrorCodes.ENCRYPTION_DECRYPTION_FAILED, e);
		}
	}
}
