package io.mosip.credential.request.generator.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation used to indicate that decryption should be skipped
 * for the annotated method.
 * Typically used in conjunction with AOP to dynamically bypass decryption logic
 * by setting a flag in {@code CryptoContext}.
 * This is useful for internal processes or batch jobs where decrypted data is not required.
 *
 * @author tarique-azeez
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SkipDecryption {
}
