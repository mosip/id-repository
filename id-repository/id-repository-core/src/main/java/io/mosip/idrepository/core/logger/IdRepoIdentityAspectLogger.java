package io.mosip.idrepository.core.logger;

import java.time.Duration;
import java.time.LocalDateTime;

import javax.annotation.PostConstruct;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;

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

	private LocalDateTime objectStoreExists;
	private LocalDateTime objectStoreGetObject;
	private LocalDateTime objectStorePutObject;
	private LocalDateTime idObjectValidator;
	private LocalDateTime idObjectReferenceValidator;

	@Before(value = "execution(* io.mosip.commons.khazana.impl.S3Adapter.exists(..))")
	public void existsBfore(JoinPoint joinPoint) {
		objectStoreExists = DateUtils.getUTCCurrentDateTime();
	}

	@After(value = "execution(* io.mosip.commons.khazana.impl.S3Adapter.exists(..))")
	public void existsAfter(JoinPoint joinPoint) {
		long duration = Duration.between(objectStoreExists, DateUtils.getUTCCurrentDateTime()).toMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), joinPoint.getSignature().getDeclaringTypeName(),
				joinPoint.getSignature().getName(), " Time taken to respond in ms: " + duration);
	}

	@Before(value = "execution(* io.mosip.commons.khazana.impl.S3Adapter.getObject(..))")
	public void getObjectBefore(JoinPoint joinPoint) {
		objectStoreGetObject = DateUtils.getUTCCurrentDateTime();
	}

	@After(value = "execution(* io.mosip.commons.khazana.impl.S3Adapter.getObject(..))")
	public void getObjectAfter(JoinPoint joinPoint) {
		long duration = Duration.between(objectStoreGetObject, DateUtils.getUTCCurrentDateTime()).toMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), joinPoint.getSignature().getDeclaringTypeName(),
				joinPoint.getSignature().getName(), " Time taken to respond in ms: " + duration);
	}

	@Before(value = "execution(* io.mosip.commons.khazana.impl.S3Adapter.putObject(..))")
	public void putObjectBefore(JoinPoint joinPoint) {
		objectStorePutObject = DateUtils.getUTCCurrentDateTime();
	}

	@After(value = "execution(* io.mosip.commons.khazana.impl.S3Adapter.putObject(..))")
	public void putObjectAfter(JoinPoint joinPoint) {
		long duration = Duration.between(objectStorePutObject, DateUtils.getUTCCurrentDateTime()).toMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), joinPoint.getSignature().getDeclaringTypeName(),
				joinPoint.getSignature().getName(), " Time taken to respond in ms: " + duration);
	}

	@Before(value = "execution(* io.mosip.kernel.idobjectvalidator.impl.IdObjectSchemaValidator.validateIdObject(..))")
	public void idObjectValidatorBefore(JoinPoint joinPoint) {
		idObjectValidator = DateUtils.getUTCCurrentDateTime();
	}

	@After(value = "execution(* io.mosip.kernel.idobjectvalidator.impl.IdObjectSchemaValidator.validateIdObject(..))")
	public void idObjectValidatorAfter(JoinPoint joinPoint) {
		long duration = Duration.between(idObjectValidator, DateUtils.getUTCCurrentDateTime()).toMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), joinPoint.getSignature().getDeclaringTypeName(),
				joinPoint.getSignature().getName(), " Time taken to respond in ms: " + duration);
	}

	@Before(value = "execution(* io.mosip.kernel.idobjectvalidator.impl.IdObjectCompositeValidator.validateIdObject(..))")
	public void idObjectRefValidatorBefore(JoinPoint joinPoint) {
		idObjectReferenceValidator = DateUtils.getUTCCurrentDateTime();
	}

	@After(value = "execution(* io.mosip.kernel.idobjectvalidator.impl.IdObjectCompositeValidator.validateIdObject(..))")
	public void idObjectRefValidatorAfter(JoinPoint joinPoint) {
		long duration = Duration.between(idObjectReferenceValidator, DateUtils.getUTCCurrentDateTime()).toMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), joinPoint.getSignature().getDeclaringTypeName(),
				joinPoint.getSignature().getName(), " Time taken to respond in ms: " + duration);
	}

}
