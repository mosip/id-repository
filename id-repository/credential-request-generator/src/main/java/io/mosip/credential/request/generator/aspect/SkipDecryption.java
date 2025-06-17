package io.mosip.credential.request.generator.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is designed for method. This used to provide an indication to skip the entity
 * decryption while loading an entity using the JPA. Suggested to be used operations on JPA
 * entity.
 *
 * @author tarique-azeez
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SkipDecryption {
}
