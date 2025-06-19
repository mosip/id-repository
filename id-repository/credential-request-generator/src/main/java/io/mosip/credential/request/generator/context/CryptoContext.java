package io.mosip.credential.request.generator.context;

/**
 * This class is used for passing the context information inside a thread.
 * We must clear the context information post usage because it can cause issues in thread pool
 * environment where threads are being shared.
 *
 * @author tarique-azeez
 */

public class CryptoContext {

    private static final ThreadLocal<Boolean> skipDecryption = ThreadLocal.withInitial(() -> false);

    public static void setSkipDecryption(boolean value) {
        skipDecryption.set(value);
    }

    public static boolean isSkipDecryption() {
        return skipDecryption.get();
    }
}
