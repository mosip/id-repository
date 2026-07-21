package io.mosip.idrepository.credential.request.interceptor;

import java.util.Arrays;

import io.mosip.idrepository.credential.request.context.CryptoContext;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;

import io.mosip.idrepository.credential.request.entity.CredentialEntity;
import io.mosip.idrepository.credential.request.util.CredReqRestUtil;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;

/**
 * Hibernate {@link Interceptor} that encrypts and decrypts the {@code request}
 * column of {@link CredentialEntity} at persistence boundaries.
 * <p>
 * On save and flush-dirty, plaintext JSON is URL-safe Base64-encoded and encrypted
 * via {@link io.mosip.idrepository.credential.request.util.CryptoUtil} (cryptomanager).
 * On load, ciphertext is decrypted unless {@link CryptoContext#isSkipDecryption()} is set.
 * Falls back to treating data as plaintext for backward compatibility with MOSIP 1.1.5.5 rows.
 * </p>
 *
 * @see io.mosip.idrepository.credential.request.api.config.CredentialRequestGeneratorConfig
 */
public class CredentialTransactionInterceptor implements Interceptor {

	private static final String REQUEST = "request";

	/**
	 * Cryptomanager client for field-level encrypt/decrypt of queue payloads.
	 */
	private final io.mosip.idrepository.credential.request.util.CryptoUtil cryptoUtil;

	/**
	 * REST util retained for interceptor re-wiring after deserialization; not used in encrypt/decrypt path.
	 */
	private transient CredReqRestUtil restUtil;

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialTransactionInterceptor.class);

	/**
	 * Creates the interceptor with outbound REST and crypto dependencies.
	 *
	 * @param restUtil    credential-request REST client (for post-deserialization wiring)
	 * @param cryptoUtil  cryptomanager encrypt/decrypt helper
	 */
	public CredentialTransactionInterceptor(CredReqRestUtil restUtil, io.mosip.idrepository.credential.request.util.CryptoUtil cryptoUtil) {
		this.restUtil = restUtil;
		this.cryptoUtil = cryptoUtil;
	}

	/**
	 * Encrypts {@link CredentialEntity#getRequest()} before insert.
	 *
	 * @param entity         persisted entity
	 * @param id             primary key
	 * @param state          Hibernate property state array
	 * @param propertyNames  property names parallel to {@code state}
	 * @param types          Hibernate property types
	 * @return delegate result from {@link Interceptor#onSave}
	 */
	@Override
	public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
		encryptData(entity, state, propertyNames);
		return Interceptor.super.onSave(entity, id, state, propertyNames, types);
	}

	/**
	 * Decrypts {@link CredentialEntity#getRequest()} after load unless skip flag is set.
	 *
	 * @param entity         loaded entity
	 * @param id             primary key
	 * @param state          Hibernate property state array
	 * @param propertyNames  property names parallel to {@code state}
	 * @param types          Hibernate property types
	 * @return delegate result from {@link Interceptor#onLoad}
	 */
	@Override
	public boolean onLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
		if (CryptoContext.isSkipDecryption()) {
			LOGGER.debug("SkipDecryption is enabled. Entity class: "+ entity.getClass());
			return Interceptor.super.onLoad(entity, id, state, propertyNames, types);
		}
		if (entity instanceof CredentialEntity) {
			int indexOfData = Arrays.asList(propertyNames).indexOf(REQUEST);
			String decryptedData;
			String requestValue = (String) state[indexOfData];
			try {
				decryptedData = new String(CryptoUtil
						.decodeURLSafeBase64(cryptoUtil.decryptData(requestValue)));
			} catch (Exception e) {
				LOGGER.debug(
						"Decryption failed. Falling back to treat the data as un-encrypted one for backward compatibility with 1.1.5.5\n "
								+ ExceptionUtils.getStackTrace(e));
				decryptedData = requestValue;
			}
			state[indexOfData] = decryptedData;
		}
		return Interceptor.super.onLoad(entity, id, state, propertyNames, types);
	}

	/**
	 * Re-encrypts {@link CredentialEntity#getRequest()} when a dirty entity is flushed.
	 *
	 * @param entity         dirty entity
	 * @param id             primary key
	 * @param currentState   updated property state
	 * @param previousState  previous property state
	 * @param propertyNames  property names parallel to state arrays
	 * @param types          Hibernate property types
	 * @return delegate result from {@link Interceptor#onFlushDirty}
	 */
	@Override
	public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
		encryptData(entity, currentState, propertyNames);
		return Interceptor.super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
	}

	private void encryptData(Object entity, Object[] state, String[] propertyNames) {
		if (entity instanceof CredentialEntity) {
			CredentialEntity credEntity = (CredentialEntity) entity;
			String encryptedData = cryptoUtil.encryptData(CryptoUtil.encodeToURLSafeBase64(credEntity.getRequest().getBytes()));
			credEntity.setRequest(encryptedData);
			int indexOfData = Arrays.asList(propertyNames).indexOf(REQUEST);
			state[indexOfData] = encryptedData;
		}
	}

	/**
	 * Re-injects {@link CredReqRestUtil} after the interceptor is deserialized by Hibernate.
	 *
	 * @param restUtil credential-request REST client instance
	 */
	public void setRestUtil(CredReqRestUtil restUtil) {
		this.restUtil = restUtil;
	}
}
