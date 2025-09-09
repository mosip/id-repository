package io.mosip.credential.request.generator.context;
/**
 * This class is used for passing the context information inside a thread.
 * We must clear the context information post usage because it can cause issues in thread pool
 * environment where threads are being shared.
 *
 * @author tarique-azeez
 */
public final class CryptoContext implements AutoCloseable {
    private static final ThreadLocal<Boolean> SKIP_DECRYPTION =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private CryptoContext() {
        // Prevent instantiation
    }
    /**
     * Set the per-thread skipDecryption flag.
     *
     * @param value true if decryption should be skipped for this thread
     */
    public static void setSkipDecryption(boolean value) {
        SKIP_DECRYPTION.set(value);
    }
    /**
     * Get the per-thread skipDecryption flag.
     *
     * @return true if decryption should be skipped for this thread
     */
    public static boolean isSkipDecryption() {
        return Boolean.TRUE.equals(SKIP_DECRYPTION.get());
    }
    /**
     * Clear the skipDecryption flag for the current thread.
     * Must be called after use to avoid memory leaks in pooled threads.
     */
    public static void clear() {
        SKIP_DECRYPTION.remove();
    }
    /**
     * Provide a scoped CryptoContext for try-with-resources usage.
     * The flag is set, and automatically cleared when the resource is closed.
     *
     * @param value true if decryption should be skipped for this scope
     * @return a CryptoContext instance
     */
    public static CryptoContext scope(boolean value) {
        setSkipDecryption(value);
        return new CryptoContext();
    }
    /**
     * AutoCloseable implementation.
     * Ensures the ThreadLocal flag is removed when the resource closes.
     */
    @Override
    public void close() {
        clear();
    }
}