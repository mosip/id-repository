package io.mosip.idrepository.core.dto;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload describing a single audit event sent to the MOSIP audit manager.
 *
 * <p>
 * Built by {@link io.mosip.idrepository.core.builder.AuditRequestBuilder} and
 * posted by {@link io.mosip.idrepository.core.helper.AuditHelper}. Fields marked
 * {@link NotNull} are required when posting audit records; subject id fields
 * are optional and often carry a hashed identifier for privacy.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Outbound REST call to the kernel audit service. Not part of the public
 * identity/VID/credential HTTP surface, but used on every audited identity and
 * credential operation.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code AuditRequestBuilder} — constructs instances</li>
 *   <li>{@code AuditHelper} — posts to audit manager</li>
 *   <li>Identity, VID, and credential service layers that emit audits</li>
 * </ul>
 *
 * @author Manoj SP
 * @see io.mosip.idrepository.core.builder.AuditRequestBuilder
 * @see io.mosip.idrepository.core.helper.AuditHelper
 * @see AuditResponseDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditRequestDTO {

	/** Unique identifier of the audit event type (configured in audit module). */
	@NotNull
	@Size(min = 1, max = 255)
	private String eventId;

	/** Human-readable name of the audit event. */
	@NotNull
	@Size(min = 1, max = 255)
	private String eventName;

	/** Category of the event (for example, business or system). */
	@NotNull
	@Size(min = 1, max = 255)
	private String eventType;

	/** Timestamp when the audited action occurred. */
	@NotNull
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	private LocalDateTime actionTimeStamp;

	/** Host name of the machine that performed the action. */
	@NotNull
	@Size(min = 1, max = 255)
	private String hostName;

	/** IP address of the host that performed the action. */
	@NotNull
	@Size(min = 1, max = 255)
	private String hostIp;

	/** Application identifier registered with the audit service. */
	@NotNull
	@Size(min = 1, max = 255)
	private String applicationId;

	/** Display name of the application that generated the audit. */
	@NotNull
	@Size(min = 1, max = 255)
	private String applicationName;

	/** Identifier of the user session that triggered the event. */
	@NotNull
	@Size(min = 1, max = 255)
	private String sessionUserId;

	/** Display name of the user associated with the session. */
	@NotNull
	@Size(min = 1, max = 255)
	private String sessionUserName;

	/** Subject identifier related to the event (UIN, VID, RID, etc.); optional. */
	@Size(max = 255)
	private String id;

	/** Type of the subject identifier (for example, UIN or VID); optional. */
	@Size(max = 255)
	private String idType;

	/** User or system component that created the audit record. */
	@NotNull
	@Size(min = 1, max = 255)
	private String createdBy;

	/** Name of the functional module where the event originated; optional. */
	@Size(max = 255)
	private String moduleName;

	/** Identifier of the functional module; optional. */
	@Size(max = 255)
	private String moduleId;

	/** Free-text description of the audited action; optional. */
	@Size(max = 2048)
	private String description;
}
