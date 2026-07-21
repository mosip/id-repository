package io.mosip.idrepository.core.entity;

/**
 * Extension of {@link UinInfo} for entities that also store a user-chosen handle.
 *
 * <p>
 * Implemented by {@link Handle}. The identity entity interceptor encrypts the
 * handle value on save and supports handle-hash lookups during credential
 * issuance when the individual is addressed by handle rather than UIN/VID.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Provides a narrow contract so crypto / interceptor code can read and write
 * handle plaintext and handle hash without depending on the concrete
 * {@link Handle} JPA type. Extends {@link UinInfo} so the same interceptor
 * path can treat handle rows uniformly with UIN entities where applicable
 * (handle rows typically no-op UIN payload accessors).
 * </p>
 *
 * <h2>Table / persistence unit</h2>
 * <p>
 * Concrete implementor {@link Handle} maps to {@code mosip_idrepo.handle} on
 * PU1 ({@code mosip_idrepo} / {@code idRepoDataSource}). This interface itself
 * is not a JPA entity.
 * </p>
 *
 * <h2>Salt routing notes</h2>
 * <p>
 * {@link #getHandleHash()} / {@link #setHandleHash(String)} values are salted
 * with idrepo {@code uin_hash_salt} (via {@code IdRepoSecurityManager} /
 * {@code SaltUtil}). Do not route handle hashing through idmap VID salt
 * repositories. Plaintext {@link #getHandle()} is encrypted before persistence
 * by {@code IdRepoEntityInterceptor}.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link Handle} — sole JPA implementor in core</li>
 *   <li>{@code IdRepoEntityInterceptor} — encrypt/decrypt handle on flush/load</li>
 *   <li>{@code CredentialServiceManager} / identity services — resolve and
 *       persist handles during registration and credential flows</li>
 * </ul>
 *
 * <h2>IDA note</h2>
 * <p>
 * This interface is part of the shared identity persistence contract inside
 * id-repository. IDA does <strong>not</strong> consume handle entities or
 * id-repo salt tables; keep the published core surface stable if IDA or other
 * modules depend on related DTOs/utilities, but do not assume IDA reads handles.
 * </p>
 *
 * @see Handle
 * @see UinInfo
 * @see io.mosip.idrepository.identity.interceptor.IdRepoEntityInterceptor
 */
public interface HandleInfo extends UinInfo {

	/**
	 * Returns the user-chosen handle value.
	 * <p>
	 * In memory this is typically plaintext; the interceptor encrypts before
	 * writing to the database and decrypts on load.
	 * </p>
	 *
	 * @return plaintext handle (transient relative to DB ciphertext)
	 */
	String getHandle();

	/**
	 * Sets the user-chosen handle value prior to persistence encryption.
	 *
	 * @param handle plaintext user-chosen handle
	 */
	void setHandle(String handle);

	/**
	 * Returns the salted hash of the handle used for uniqueness and reverse
	 * lookup.
	 *
	 * @return salted handle hash
	 */
	String getHandleHash();

	/**
	 * Sets the salted hash of the handle.
	 *
	 * @param handleHash salted handle hash derived with idrepo hash salt
	 */
	void setHandleHash(String handleHash);
}
