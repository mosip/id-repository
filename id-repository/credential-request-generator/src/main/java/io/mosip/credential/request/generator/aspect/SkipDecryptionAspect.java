package io.mosip.credential.request.generator.aspect;

import io.mosip.credential.request.generator.api.annotation.SkipDecryption;
import io.mosip.credential.request.generator.batch.config.CredentialItemTasklet;
import io.mosip.credential.request.generator.util.SkipDecryptionContext;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SkipDecryptionAspect {

    private static final Logger LOGGER = IdRepoLogger.getLogger(SkipDecryptionAspect.class);

    @Around("@annotation(skipDecryption)")
    public Object aroundSkipDecryption(ProceedingJoinPoint pjp, SkipDecryption skipDecryption) throws Throwable {
        LOGGER.info(">>>>> AOP activated for SkipDecryption on method: " + pjp.getSignature());
        try {
            SkipDecryptionContext.setSkipDecryption(true);
            return pjp.proceed();
        } finally {
            SkipDecryptionContext.setSkipDecryption(false);
        }
    }
}
