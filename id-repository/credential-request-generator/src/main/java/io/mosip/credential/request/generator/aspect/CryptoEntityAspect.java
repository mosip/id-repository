package io.mosip.credential.request.generator.aspect;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * This class is designed for keeping the AOP aspects related to entities.
 * Example: AOP aspect is created for intercepting the method call annotated
 * with @SkipDecryption, to avoid decryption for an entity in hibernate interceptor
 * using the JPA method.
 *
 * @author tarique-azeez
 */

@Aspect
@Component
public class CryptoEntityAspect {

    private static final Logger LOGGER = IdRepoLogger.getLogger(CryptoEntityAspect.class);

    @Around("@annotation(skipDecryption)")
    public Object aroundSkipDecryption(ProceedingJoinPoint pjp, SkipDecryption skipDecryption) throws Throwable {
        LOGGER.debug("aroundSkipDecryption() method called. Method signature: " + pjp.getSignature());

        try {
            CryptoContext.setSkipDecryption(true);
            return pjp.proceed();
        } finally {
            CryptoContext.setSkipDecryption(false);
        }
    }
}
