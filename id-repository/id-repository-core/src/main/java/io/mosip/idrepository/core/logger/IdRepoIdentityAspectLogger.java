package io.mosip.idrepository.core.logger;

import javax.annotation.PostConstruct;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;

@Aspect
@Configuration
@ConditionalOnBean(name = { "s3Adapter" })
@ConditionalOnProperty(value = "mosip.idrepo.aspect-logging.enabled", havingValue = "true", matchIfMissing = true)
public class IdRepoIdentityAspectLogger {

	@PostConstruct
	public void init() {
		System.err.println("IdRepoIdentityAspectLogger");
	}

	private transient Logger mosipLogger = IdRepoLogger.getLogger(IdRepoIdentityAspectLogger.class);

	@Pointcut("execution(* io.mosip.commons.khazana.impl.S3Adapter.exists(..))")
	public void objectStoreExistsMethods() {
	}

	@Around("objectStoreExistsMethods()")
	public void objectStoreExistsaroundAdvice(ProceedingJoinPoint jp) throws Throwable {
		long beforeExecutionTime = System.currentTimeMillis();
		jp.proceed();
		long afterExecutionTime = System.currentTimeMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), jp.getSignature().getDeclaringTypeName(),
				jp.getSignature().getName(),
				" Time taken to respond in ms: " + (afterExecutionTime - beforeExecutionTime));
	}

	@Pointcut("execution(* io.mosip.commons.khazana.impl.S3Adapter.getObject(..))")
	public void objectStoreGetObjectMethods() {
	}

	@Around("objectStoreGetObjectMethods()")
	public void objectStoreGetObjectaroundAdvice(ProceedingJoinPoint jp) throws Throwable {
		long beforeExecutionTime = System.currentTimeMillis();
		jp.proceed();
		long afterExecutionTime = System.currentTimeMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), jp.getSignature().getDeclaringTypeName(),
				jp.getSignature().getName(),
				" Time taken to respond in ms: " + (afterExecutionTime - beforeExecutionTime));
	}

	@Pointcut("execution(* io.mosip.commons.khazana.impl.S3Adapter.putObject(..))")
	public void objectStorePutObjectMethods() {
	}

	@Around("objectStorePutObjectMethods()")
	public void objectStorePutObjectaroundAdvice(ProceedingJoinPoint jp) throws Throwable {
		long beforeExecutionTime = System.currentTimeMillis();
		jp.proceed();
		long afterExecutionTime = System.currentTimeMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), jp.getSignature().getDeclaringTypeName(),
				jp.getSignature().getName(),
				" Time taken to respond in ms: " + (afterExecutionTime - beforeExecutionTime));
	}

	@Pointcut("execution(* io.mosip.kernel.idobjectvalidator.impl.IdObjectSchemaValidator.validateIdObject(..))")
	public void idObjectValidatorMethods() {
	}

	@Around("idObjectValidatorMethods()")
	public void idObjectValidatoraroundAdvice(ProceedingJoinPoint jp) throws Throwable {
		long beforeExecutionTime = System.currentTimeMillis();
		jp.proceed();
		long afterExecutionTime = System.currentTimeMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), jp.getSignature().getDeclaringTypeName(),
				jp.getSignature().getName(),
				" Time taken to respond in ms: " + (afterExecutionTime - beforeExecutionTime));
	}

	@Pointcut("execution(* io.mosip.kernel.idobjectvalidator.impl.IdObjectCompositeValidator.validateIdObject(..))")
	public void idObjectReferenceValidatorMethods() {
	}

	@Around("idObjectReferenceValidatorMethods()")
	public void idObjectReferenceValidatoraroundAdvice(ProceedingJoinPoint jp) throws Throwable {
		long beforeExecutionTime = System.currentTimeMillis();
		jp.proceed();
		long afterExecutionTime = System.currentTimeMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), jp.getSignature().getDeclaringTypeName(),
				jp.getSignature().getName(),
				" Time taken to respond in ms: " + (afterExecutionTime - beforeExecutionTime));
	}

}
