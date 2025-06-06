package io.mosip.credential.request.generator.aspect;

import io.mosip.credential.request.generator.api.annotation.SkipDecryption;
import io.mosip.credential.request.generator.util.SkipDecryptionContext;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.logger.spi.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;

@Aspect
@Component
public class SkipDecryptionAspect {

    private static final Logger LOGGER = IdRepoLogger.getLogger(SkipDecryptionAspect.class);

    @Around("execution(* io.mosip.credential.request.generator.repositary..*(..))")
    public Object aroundSkipDecryption(ProceedingJoinPoint pjp) throws Throwable {
        Method method = resolveMethod(pjp);
        boolean shouldSkip = method != null && method.isAnnotationPresent(SkipDecryption.class);

        if (shouldSkip) {
            LOGGER.info(">>>>> AOP activated for SkipDecryption on method: " + method.getName());
            SkipDecryptionContext.setSkipDecryption(true);
        }

        try {
            return pjp.proceed();
        } finally {
            if (shouldSkip) {
                SkipDecryptionContext.setSkipDecryption(false);
            }
        }
    }

    private Method resolveMethod(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method interfaceMethod = signature.getMethod();

        Class<?> targetClass = AopUtils.getTargetClass(pjp.getTarget());

        try {
            return targetClass.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return interfaceMethod;
        }
    }
}
