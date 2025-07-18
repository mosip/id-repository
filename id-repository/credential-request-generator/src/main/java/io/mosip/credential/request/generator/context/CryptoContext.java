package io.mosip.credential.request.generator.context;

public class CryptoContext {

    private static final ThreadLocal<Boolean> skipDecryption = ThreadLocal.withInitial(() -> false);

    public static void setSkipDecryption(boolean value) {
        skipDecryption.set(value);
    }

    public static boolean isSkipDecryption() {
        return skipDecryption.get();
    }
}
