package io.mosip.idrepository.credential.request.context;

/**
 * Thread-local flag to bypass Hibernate field decryption on credential queue reads.
 * <p>
 * Batch workers decrypt request payloads explicitly; setting {@link #setSkipDecryption(boolean)}
 * avoids double decryption when listing {@code credential_transaction} rows. Must be cleared in
 * {@code finally} blocks because tasklets run on pooled threads.
 * </p>
 *
 * Thread-local flag used by {@link io.mosip.idrepository.credential.request.interceptor.CredentialTransactionInterceptor}.
 * @see io.mosip.idrepository.credential.request.interceptor.CredentialTransactionInterceptor
 * @author tarique-azeez
 */
public class CryptoContext {

	/** Per-thread skip-decryption flag (default {@code false}). */
	private static final ThreadLocal<Boolean> skipDecryption = ThreadLocal.withInitial(() -> false);

	/**
	 * Enables or disables transparent decryption for the current thread.
	 *
	 * @param value {@code true} to skip decryption on entity load
	 */
	public static void setSkipDecryption(boolean value) {
		skipDecryption.set(value);
	}

	/**
	 * Returns whether decryption should be skipped for the current thread.
	 *
	 * @return skip flag
	 */
	public static boolean isSkipDecryption() {
		return skipDecryption.get();
	}

	/**
	 * Removes the thread-local flag; call after DAO operations on pooled threads.
	 */
	public static void clearSkipDecryption() {
		skipDecryption.remove();
	}
}
