package io.mosip.credential.request.generator.aspect;

/**
 * Holds cryptographic context information (e.g., whether to skip encryption/decryption)
 * using ThreadLocal to ensure it's isolated per request.
 * Commonly used with annotations like @SkipDecryption to control behavior dynamically
 * during AOP or interceptor execution.
 * Always clear the context after use to avoid memory leaks.
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
