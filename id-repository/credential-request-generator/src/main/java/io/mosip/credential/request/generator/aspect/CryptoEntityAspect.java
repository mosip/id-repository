package io.mosip.credential.request.generator.aspect;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class CryptoEntityAspect {

    private static final Logger LOGGER = IdRepoLogger.getLogger(CryptoEntityAspect.class);

    @Around("@annotation(skipDecryption)")
    public Object aroundSkipDecryption(ProceedingJoinPoint pjp, SkipDecryption skipDecryption) throws Throwable {
        LOGGER.debug("AOP activated for @SkipDecryption on method: " + pjp.getSignature());

        try {
            CryptoContext.setSkipDecryption(true);
            return pjp.proceed();
        } finally {
            CryptoContext.setSkipDecryption(false);
        }
    }
}
