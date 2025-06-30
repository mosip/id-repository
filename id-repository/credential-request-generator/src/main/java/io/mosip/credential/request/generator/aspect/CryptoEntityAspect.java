package io.mosip.credential.request.generator.aspect;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect to handle encryption and decryption of entity fields based on custom annotations.
 * Intercepts methods or entities annotated with encryption-related annotations (e.g.,@SkipDecryption)
 * and dynamically applies cryptographic logic before or after method execution.
 * Typically used with AOP to centralize encryption/decryption concerns outside of business logic.
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
