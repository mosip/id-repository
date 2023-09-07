package io.mosip.idrepository.core.logger;

import javax.annotation.PostConstruct;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;

@Aspect
@Configuration
@ConditionalOnClass(name = { "io.mosip.idrepository.core.helper.RestHelper" })
@ConditionalOnProperty(value = "mosip.idrepo.aspect-logging.enabled", havingValue = "true", matchIfMissing = true)
public class IdRepoAspectLogger {

	@PostConstruct
	public void init() {
		System.err.println("IdRepoAspectLogger");
	}

	private transient Logger mosipLogger = IdRepoLogger.getLogger(IdRepoAspectLogger.class);

	@Pointcut("execution(* io.mosip.idrepository.core.helper.RestHelper.requestSync(..))")
	public void restHelperMethods() {
	}

	@Around("restHelperMethods()")
	public void restHelperaroundAdvice(ProceedingJoinPoint jp) throws Throwable {
		mosipLogger.info("Executing method => " + jp.getSignature());
		long beforeExecutionTime = System.currentTimeMillis();
		jp.proceed();
		long afterExecutionTime = System.currentTimeMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), jp.getSignature().getDeclaringTypeName(),
				jp.getSignature().getName(), ((RestRequestDTO) jp.getArgs()[0]).getUri()
						+ " - Time taken to respond in ms: " + (afterExecutionTime - beforeExecutionTime));
	}

	@Pointcut("execution(* io.mosip.kernel.websub.api.client.PublisherClientImpl.registerTopic(..))")
	public void registerTopicMethods() {
	}

	@Around("registerTopicMethods()")
	public void registerTopicaroundAdvice(ProceedingJoinPoint jp) throws Throwable {
		mosipLogger.info("Executing method => " + jp.getSignature());
		long beforeExecutionTime = System.currentTimeMillis();
		jp.proceed();
		long afterExecutionTime = System.currentTimeMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), jp.getSignature().getDeclaringTypeName(),
				jp.getSignature().getName(), jp.getArgs()[0].toString() + " - Time taken to respond in ms: "
						+ (afterExecutionTime - beforeExecutionTime));
	}

	@Pointcut("execution(* io.mosip.kernel.websub.api.client.PublisherClientImpl.publishUpdate(..))")
	public void publishUpdateMethods() {
	}

	@Around("publishUpdateMethods()")
	public void publishUpdatearoundAdvice(ProceedingJoinPoint jp) throws Throwable {
		mosipLogger.info("Executing method => " + jp.getSignature());
		long beforeExecutionTime = System.currentTimeMillis();
		jp.proceed();
		long afterExecutionTime = System.currentTimeMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), jp.getSignature().getDeclaringTypeName(),
				jp.getSignature().getName(), jp.getArgs()[0].toString() + " - Time taken to respond in ms: "
						+ (afterExecutionTime - beforeExecutionTime));
	}

	@Pointcut("execution(* io.mosip.kernel.auth.defaultadapter.handler.AuthHandler.getValidatedUserResponse(..))")
	public void validatedUserResponseMethods() {
	}

	@Around("validatedUserResponseMethods()")
	public void validatedUserResponsearoundAdvice(ProceedingJoinPoint jp) throws Throwable {
		mosipLogger.info("Executing method => " + jp.getSignature());
		long beforeExecutionTime = System.currentTimeMillis();
		jp.proceed();
		long afterExecutionTime = System.currentTimeMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), jp.getSignature().getDeclaringTypeName(),
				jp.getSignature().getName(),
				" Time taken to respond in ms: " + (afterExecutionTime - beforeExecutionTime));
	}

	@Pointcut("execution(* io.mosip.kernel.auth.defaultadapter.handler.AuthHandler.getKeycloakValidatedUserResponse(..))")
	public void keycloakValidatedUserResponseMethods() {
	}

	@Around("keycloakValidatedUserResponseMethods()")
	public void keycloakValidatedUserResponsearoundAdvice(ProceedingJoinPoint jp) throws Throwable {
		mosipLogger.info("Executing method => " + jp.getSignature());
		long beforeExecutionTime = System.currentTimeMillis();
		jp.proceed();
		long afterExecutionTime = System.currentTimeMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), jp.getSignature().getDeclaringTypeName(),
				jp.getSignature().getName(),
				" Time taken to respond in ms: " + (afterExecutionTime - beforeExecutionTime));
	}
}
