package io.mosip.idrepository.core.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.AuditRequestBuilder;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
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
 * The Class AuditHelper - helper class that makes async rest call to audit
 * service with provided audit details .
 *
 * @author Manoj SP
 */
@Component
public class AuditHelper {

	/** The mosipLogger. */
	private static Logger mosipLogger = IdRepoLogger.getLogger(AuditHelper.class);

	/** The rest helper. */
	@Autowired
	private RestHelper restHelper;

	/** The audit factory. */
	@Autowired
	private AuditRequestBuilder auditBuilder;

	/** The rest factory. */
	@Autowired
	private RestRequestBuilder restBuilder;
	
	/** The security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;
	
	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	/**
	 * Audit - method to call audit service and store audit details.
	 *
	 * @param module the module
	 * @param event  the event
	 * @param id     the id
	 * @param idType the id type
	 * @param desc   the desc
	 */
	public void audit(AuditModules module, AuditEvents event, String id, IdType idType, String desc) {
		RequestWrapper<AuditRequestDTO> auditRequest = auditBuilder.buildRequest(module, event,
				securityManager.hash(id.getBytes()), idType, desc);
		RestRequestDTO restRequest;
		try {
			restRequest = restBuilder.buildRequest(RestServicesConstants.AUDIT_MANAGER_SERVICE, auditRequest,
					AuditResponseDTO.class);
			restHelper.requestAsync(restRequest);
		} catch (IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "AuditRequestBuilder", "audit",
					"Exception : " + ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "AuditRequestBuilder", "audit",
					"Exception : " + ExceptionUtils.getStackTrace(e));
		}
	}
	
	/**
	 * Audit error.
	 *
	 * @param module the module
	 * @param event the event
	 * @param id the id
	 * @param idType the id type
	 * @param e the e
	 */
	public void auditError(AuditModules module, AuditEvents event, String id, IdType idType, Throwable e) {
		try {
			this.audit(module, event, id, idType, mapper.writeValueAsString(IdRepoExceptionHandler.getAllErrors(e)));
		} catch (JsonProcessingException ex) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "AuditRequestBuilder", "auditError",
					"Exception : " + ExceptionUtils.getStackTrace(ex));
		}
	}

}
