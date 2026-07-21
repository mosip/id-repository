package io.mosip.idrepository.core.helper;

import java.util.concurrent.Executor;

import io.mosip.idrepository.core.exception.RestServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.AuditRequestBuilder;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.AuditRequestDTO;
import io.mosip.idrepository.core.dto.AuditResponseDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.IdRepoExceptionHandler;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Best-effort helper for posting audit events to the MOSIP audit-manager service.
 * <p>
 * Builds {@link AuditRequestDTO} payloads via {@link AuditRequestBuilder}, hashes the subject
 * identifier with {@link IdRepoSecurityManager#hash(byte[])} so raw UIN/VID never leave the
 * service in audit payloads, and posts through {@link RestHelper} to
 * {@link RestServicesConstants#AUDIT_MANAGER_SERVICE}. Failures are logged and never rethrown —
 * business flows must not fail because audit is unavailable.
 * </p>
 * <p>
 * Dispatch mode is controlled by {@link IdRepoConstants#AUDIT_ASYNC_ENABLED}
 * ({@code mosip.idrepo.audit.async-enabled}). When async, work runs on the
 * {@code withSecurityContext} executor so outbound auth headers are preserved.
 * </p>
 *
 * @see AuditRequestBuilder
 * @see RestHelper#requestSync(RestRequestDTO)
 * @see RestServicesConstants#AUDIT_MANAGER_SERVICE
 * @see IdRepoExceptionHandler#getAllErrors(Throwable)
 * @author Manoj SP
 */
@Component
public class AuditHelper {

	/** Structured logger for audit helper operations. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(AuditHelper.class);

	/** REST client for outbound calls to audit-manager. */
	@Autowired
	private RestHelper restHelper;

	/** Factory for constructing MOSIP audit request wrappers. */
	@Autowired
	private AuditRequestBuilder auditBuilder;

	/** Factory for building REST request DTOs with service URLs and headers. */
	@Autowired
	private RestRequestBuilder restBuilder;

	/** Provides HMAC hashing of subject IDs before audit submission. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	/** JSON serializer for error detail payloads in {@link #auditError}. */
	@Autowired
	private ObjectMapper mapper;

	/**
	 * When {@code true}, audit REST posts run asynchronously. Property:
	 * {@link IdRepoConstants#AUDIT_ASYNC_ENABLED}.
	 */
	@Value("${" + IdRepoConstants.AUDIT_ASYNC_ENABLED + ":true}")
	private boolean asyncEnabled;

	/**
	 * Security-context-aware executor used when {@link #asyncEnabled} is true.
	 * Optional so unit tests without the bean still run synchronously.
	 */
	@Autowired(required = false)
	@Qualifier("withSecurityContext")
	private Executor securityContextExecutor;

	/**
	 * Sends an audit event to the audit-manager service.
	 * <p>
	 * The subject {@code id} is hashed on the calling thread before dispatch.
	 * {@link IdRepoDataValidationException}, {@link RestServiceException}, and any other
	 * exception during the REST call are logged and swallowed.
	 * </p>
	 *
	 * @param module MOSIP audit module (for example ID Repository core / identity)
	 * @param event  audit event type from {@link AuditEvents}
	 * @param id     subject identifier (UIN, VID, request id, etc.); hashed before transmission;
	 *               may be {@code null}
	 * @param idType type of the subject identifier
	 * @param desc   free-text audit description (or serialized error JSON from {@link #auditError})
	 */
	public void audit(AuditModules module, AuditEvents event, String id, IdType idType, String desc) {
		String requestId = null;
		if (id != null) {
			requestId = securityManager.hash(id.getBytes());
		}
		RequestWrapper<AuditRequestDTO> auditRequest = auditBuilder.buildRequest(module, event,
				requestId, idType, desc);
		dispatch(() -> sendAudit(auditRequest));
	}

	/**
	 * Sends an audit event describing a failure, with serialized MOSIP error details.
	 *
	 * @param module MOSIP audit module
	 * @param event  audit event type
	 * @param id     subject identifier; hashed before transmission
	 * @param idType type of the subject identifier
	 * @param e      exception whose errors should be recorded in the audit description
	 */
	public void auditError(AuditModules module, AuditEvents event, String id, IdType idType, Throwable e) {
		try {
			this.audit(module, event, id, idType, mapper.writeValueAsString(IdRepoExceptionHandler.getAllErrors(e)));
		} catch (JsonProcessingException ex) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "AuditRequestBuilder", "auditError",
					"Exception : " + ExceptionUtils.getStackTrace(ex));
		}
	}

	private void dispatch(Runnable task) {
		if (asyncEnabled && securityContextExecutor != null) {
			securityContextExecutor.execute(task);
			return;
		}
		if (asyncEnabled && securityContextExecutor == null) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), "AuditHelper", "dispatch",
					"mosip.idrepo.audit.async-enabled=true but withSecurityContext executor missing; running sync");
		}
		task.run();
	}

	private void sendAudit(RequestWrapper<AuditRequestDTO> auditRequest) {
		try {
			RestRequestDTO restRequest = restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE,
					auditRequest, AuditResponseDTO.class);
			restHelper.requestSync(restRequest);
		} catch (IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "AuditRequestBuilder", "audit",
					"Exception : " + ExceptionUtils.getStackTrace(e));
		} catch (RestServiceException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "AuditRequestBuilder", "audit",
					"Exception : " + ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "AuditRequestBuilder", "audit",
					"Exception : " + ExceptionUtils.getStackTrace(e));
		}
	}
}
