package io.mosip.idrepository.core.builder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.AuditRequestDTO;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;
import lombok.NoArgsConstructor;

/**
 * Builds {@link RequestWrapper}{@link AuditRequestDTO} payloads for the MOSIP
 * audit-manager service.
 *
 * <p>
 * This builder only constructs the request envelope. {@link AuditHelper} hashes the
 * subject identifier, posts via {@link RestRequestBuilder} /
 * {@link io.mosip.idrepository.core.helper.RestHelper}, and swallows transport failures
 * so business flows are never blocked by audit unavailability.
 * </p>
 *
 * <h2>Host resolution</h2>
 * <p>
 * Host name and IP are resolved once in a static initializer because
 * {@link InetAddress#getLocalHost()} performs DNS lookups and must not run on every
 * audit event. Resolution order for the host name:
 * </p>
 * <ol>
 *   <li>{@code HOSTNAME} environment variable (set by Kubernetes / Docker)</li>
 *   <li>{@link InetAddress#getHostName()} when the env var is blank</li>
 * </ol>
 * <p>
 * If DNS resolution fails, both name and address are left empty and the failure is
 * logged; audit requests still proceed.
 * </p>
 *
 * <h2>Fields populated</h2>
 * <ul>
 *   <li>Event metadata from {@link AuditEvents} ({@code eventId}, {@code eventName},
 *       {@code eventType})</li>
 *   <li>Module metadata from {@link AuditModules}</li>
 *   <li>Application id/name from {@link EnvUtil}</li>
 *   <li>Actor from {@link IdRepoSecurityManager#getUser()}</li>
 *   <li>Action timestamp truncated to millisecond precision via
 *       {@link EnvUtil#getDateTimePattern()}</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * RequestWrapper&lt;AuditRequestDTO&gt; request = auditRequestBuilder.buildRequest(
 *     AuditModules.ID_REPO_CORE_SERVICE,
 *     AuditEvents.CREATE_IDENTITY,
 *     hashedUin,
 *     IdType.UIN,
 *     "Identity created");
 * </pre>
 *
 * @author Manoj SP
 * @see AuditRequestDTO
 * @see AuditModules
 * @see AuditEvents
 * @see AuditHelper
 */
@Component
@NoArgsConstructor
public class AuditRequestBuilder {

	/** Structured logger for host-resolution failures during class initialization. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(AuditRequestBuilder.class);

	/**
	 * Cached local host name. Prefers the {@code HOSTNAME} environment variable
	 * (set in Kubernetes/Docker) before falling back to {@link InetAddress#getHostName()}.
	 */
	private static final String HOST_NAME;

	/**
	 * Cached local host IP address from a single {@link InetAddress#getLocalHost()} call.
	 */
	private static final String HOST_ADDRESS;

	static {
		HostDetails hostDetails = resolveHostDetails(System.getenv("HOSTNAME"));
		HOST_NAME = hostDetails.hostName();
		HOST_ADDRESS = hostDetails.hostAddress();
	}

	/**
	 * Resolves host name and IP for audit payloads.
	 * <p>
	 * Prefer {@code envHost} when non-blank (typical pod hostname). Otherwise use
	 * {@link InetAddress#getLocalHost()}. On {@link UnknownHostException}, returns empty
	 * strings so callers can still build audit requests.
	 * </p>
	 *
	 * @param envHost value of the {@code HOSTNAME} environment variable; may be {@code null}
	 *                or blank
	 * @return immutable host name / address pair (never {@code null}; fields may be empty)
	 */
	public static HostDetails resolveHostDetails(String envHost) {
		String hostName = envHost != null && !envHost.isBlank() ? envHost : "";
		String hostAddress = "";
		try {
			InetAddress inetAddress = InetAddress.getLocalHost();
			if (hostName.isEmpty()) {
				hostName = inetAddress.getHostName();
			}
			hostAddress = inetAddress.getHostAddress();
		} catch (UnknownHostException ex) {
			mosipLogger.error(IdRepoSecurityManager.getUser(),
					"AuditRequestFactory", ex.getClass().getName(),
					"Exception : " + ExceptionUtils.getStackTrace(ex));
		}
		return new HostDetails(hostName, hostAddress);
	}

	/**
	 * Immutable host identity used in audit request {@code hostName} / {@code hostIp} fields.
	 *
	 * @param hostName    resolved host name (may be empty when DNS fails)
	 * @param hostAddress resolved IP address (may be empty when DNS fails)
	 */
	public record HostDetails(String hostName, String hostAddress) {}

	/**
	 * Builds a wrapped audit request ready to post to the audit manager.
	 * <p>
	 * The wrapper uses fixed {@code id=audit} and {@code version=1.0}. Session user fields
	 * are currently placeholders ({@code sessionUserId} / {@code sessionUserName}); the
	 * authenticated actor is recorded in {@code createdBy} via
	 * {@link IdRepoSecurityManager#getUser()}.
	 * </p>
	 * <p>
	 * Callers that must not leak raw UIN/VID should pass an already-hashed {@code id}
	 * (see {@link AuditHelper#audit}).
	 * </p>
	 *
	 * @param module the audit module (source component) for this event
	 * @param event  the audit event type and metadata
	 * @param id     the entity identifier being audited (e.g. hashed UIN, VID, RID);
	 *               may be {@code null} for rare system events with no subject
	 * @param idType the type of {@code id}; may be {@code null} (then {@code idType} on
	 *               the DTO is left {@code null})
	 * @param desc   human-readable description of the audited action
	 * @return {@link RequestWrapper} containing a populated {@link AuditRequestDTO};
	 *         never {@code null}
	 */
	public RequestWrapper<AuditRequestDTO> buildRequest(AuditModules module, AuditEvents event, String id, IdType idType,
			String desc) {
		RequestWrapper<AuditRequestDTO> request = new RequestWrapper<>();
		AuditRequestDTO auditRequest = new AuditRequestDTO();
		LocalDateTime actionTime = utcNowMillisPrecision();

		auditRequest.setEventId(event.getEventId());
		auditRequest.setEventName(event.getEventName());
		auditRequest.setEventType(event.getEventType());
		auditRequest.setActionTimeStamp(actionTime);
		auditRequest.setHostName(HOST_NAME);
		auditRequest.setHostIp(HOST_ADDRESS);
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
		request.setRequesttime(actionTime);

		return request;
	}

	/**
	 * Returns the current UTC timestamp truncated to millisecond precision.
	 * <p>
	 * Precision follows {@link EnvUtil#getDateTimePattern()} via
	 * {@link DateUtils2#getUTCCurrentDateTime(String)} — no string round-trip.
	 * </p>
	 *
	 * @return UTC {@link LocalDateTime} at millisecond precision
	 */
	private static LocalDateTime utcNowMillisPrecision() {
		return DateUtils2.getUTCCurrentDateTime(EnvUtil.getDateTimePattern());
	}
}
