package io.mosip.idrepository.core.builder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.AuditRequestDTO;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;
import lombok.NoArgsConstructor;

/**
 * A builder for creating and building AuditRequest objects from
 * properties.
 *
 * @author Manoj SP
 */
@Component
@NoArgsConstructor
public class AuditRequestBuilder {

	/** The mosipLogger. */
	private static Logger mosipLogger = IdRepoLogger.getLogger(AuditRequestBuilder.class);

	/**
	 * Builds the audit request for audit service.
	 *
	 * @param module the module
	 * @param event  the event
	 * @param id     the id
	 * @param idType the id type
	 * @param desc   the desc
	 * @return the audit request dto
	 */
	public RequestWrapper<AuditRequestDTO> buildRequest(AuditModules module, AuditEvents event, String id, IdType idType,
			String desc) {
		RequestWrapper<AuditRequestDTO> request = new RequestWrapper<>();
		AuditRequestDTO auditRequest = new AuditRequestDTO();
		String hostName;
		String hostAddress;

		try {
			InetAddress inetAddress = InetAddress.getLocalHost();
			hostName = inetAddress.getHostName();
			hostAddress = inetAddress.getHostAddress();
		} catch (UnknownHostException ex) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), "AuditRequestFactory", ex.getClass().getName(),
					"Exception : " + ExceptionUtils.getStackTrace(ex));
			hostName = "";
			hostAddress = "";
		}

		auditRequest.setEventId(event.getEventId());
		auditRequest.setEventName(event.getEventName());
		auditRequest.setEventType(event.getEventType());
		auditRequest.setActionTimeStamp(DateUtils2.getUTCCurrentDateTime());
		auditRequest.setHostName(hostName);
		auditRequest.setHostIp(hostAddress);
		auditRequest.setApplicationId(EnvUtil.getAppId());
		auditRequest.setApplicationName(EnvUtil.getAppName());
		auditRequest.setSessionUserId("sessionUserId");
		auditRequest.setSessionUserName("sessionUserName");
		auditRequest.setId(id);
		auditRequest.setIdType(Objects.isNull(idType) ? null : idType.getIdType());
		auditRequest.setCreatedBy(IdRepoSecurityManager.getUser());
		auditRequest.setModuleName(module.getModuleName());
		auditRequest.setModuleId(module.getModuleId());
		auditRequest.setDescription(desc);

		request.setId("audit");
		request.setRequest(auditRequest);
		request.setVersion("1.0");
		request.setRequesttime(DateUtils2.getUTCCurrentDateTime());

		return request;
	}
}
