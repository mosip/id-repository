package io.mosip.credential.request.generator.aspect;

import io.mosip.credential.request.generator.api.annotation.SkipDecryption;
import io.mosip.credential.request.generator.util.SkipDecryptionContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SkipDecryptionAspect {

    @Around("@annotation(skipDecryption)")
    public Object aroundSkipDecryption(ProceedingJoinPoint pjp, SkipDecryption skipDecryption) throws Throwable {
        try {
            SkipDecryptionContext.setSkipDecryption(true);
            return pjp.proceed();
        } finally {
            SkipDecryptionContext.setSkipDecryption(false);
        }
    }
}
