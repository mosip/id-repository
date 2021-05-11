package io.mosip.idrepository.core.logger;

import java.time.Duration;
import java.time.LocalDateTime;

import javax.annotation.PostConstruct;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;

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

	private LocalDateTime restHelperTime;
	private LocalDateTime websubRegister;
	private LocalDateTime websubPublish;
	private LocalDateTime auth;
	private LocalDateTime keycloak;

	@Before(value = "execution(* io.mosip.idrepository.core.helper.RestHelper.requestSync(..))")
	public void restHelperBefore(JoinPoint joinPoint) {
		restHelperTime = DateUtils.getUTCCurrentDateTime();
	}

	@After(value = "execution(* io.mosip.idrepository.core.helper.RestHelper.requestSync(..))")
	public void restHelperAfter(JoinPoint joinPoint) {
		long duration = Duration.between(restHelperTime, DateUtils.getUTCCurrentDateTime()).toMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), joinPoint.getSignature().getDeclaringTypeName(),
				joinPoint.getSignature().getName(),
				((RestRequestDTO) joinPoint.getArgs()[0]).getUri() + " - Time taken to respond in ms: " + duration);
	}

	@Before(value = "execution(* io.mosip.kernel.websub.api.client.PublisherClientImpl.registerTopic(..))")
	public void registerBefore(JoinPoint joinPoint) {
		websubRegister = DateUtils.getUTCCurrentDateTime();
	}

	@After(value = "execution(* io.mosip.kernel.websub.api.client.PublisherClientImpl.registerTopic(..))")
	public void registerAfter(JoinPoint joinPoint) {
		long duration = Duration.between(websubRegister, DateUtils.getUTCCurrentDateTime()).toMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), joinPoint.getSignature().getDeclaringTypeName(),
				joinPoint.getSignature().getName(),
				joinPoint.getArgs()[0].toString() + " - Time taken to respond in ms: " + duration);
	}

	@Before(value = "execution(* io.mosip.kernel.websub.api.client.PublisherClientImpl.publishUpdate(..))")
	public void publishBefore(JoinPoint joinPoint) {
		websubPublish = DateUtils.getUTCCurrentDateTime();
	}

	@After(value = "execution(* io.mosip.kernel.websub.api.client.PublisherClientImpl.publishUpdate(..))")
	public void publishAfter(JoinPoint joinPoint) {
		long duration = Duration.between(websubPublish, DateUtils.getUTCCurrentDateTime()).toMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), joinPoint.getSignature().getDeclaringTypeName(),
				joinPoint.getSignature().getName(),
				joinPoint.getArgs()[0].toString() + " - Time taken to respond in ms: " + duration);
	}

	@Before(value = "execution(* io.mosip.kernel.auth.defaultadapter.handler.AuthHandler.getValidatedUserResponse(..))")
	public void authBefore(JoinPoint joinPoint) {
		auth = DateUtils.getUTCCurrentDateTime();
	}

	@After(value = "execution(* io.mosip.kernel.auth.defaultadapter.handler.AuthHandler.getValidatedUserResponse(..))")
	public void authAfter(JoinPoint joinPoint) {
		long duration = Duration.between(auth, DateUtils.getUTCCurrentDateTime()).toMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), joinPoint.getSignature().getDeclaringTypeName(),
				joinPoint.getSignature().getName(), " Time taken to respond in ms: " + duration);
	}

	@Before(value = "execution(* io.mosip.kernel.auth.defaultadapter.handler.AuthHandler.getKeycloakValidatedUserResponse(..))")
	public void keycloakAuthBefore(JoinPoint joinPoint) {
		keycloak = DateUtils.getUTCCurrentDateTime();
	}

	@After(value = "execution(* io.mosip.kernel.auth.defaultadapter.handler.AuthHandler.getKeycloakValidatedUserResponse(..))")
	public void keycloakAuthAfter(JoinPoint joinPoint) {
		long duration = Duration.between(keycloak, DateUtils.getUTCCurrentDateTime()).toMillis();
		mosipLogger.info(IdRepoSecurityManager.getUser(), joinPoint.getSignature().getDeclaringTypeName(),
				joinPoint.getSignature().getName(), " Time taken to respond in ms: " + duration);
	}
}
