package io.mosip.idrepository.core.constant;

import lombok.Getter;

/**
 * MOSIP audit event identifiers for ID Repository API operations.
 *
 * <p>
 * Each constant carries an event ID ({@link #getEventId()}, e.g. {@code IDR-001})
 * and event category ({@link #getEventType()}, typically {@code System Event}) sent to
 * the kernel audit manager. Event names ({@link #getEventName()}) match the enum
 * constant name and are recorded alongside {@link AuditModules} in audit payloads.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Provides a stable catalogue of audit event codes for identity, VID, draft, credential,
 * and auth-type APIs so every successful or failed request can be attributed in the
 * MOSIP audit trail without hard-coding string IDs in controllers or helpers.
 * </p>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * Audit event IDs are internal to ID Repository and the kernel audit manager. IDA does
 * not consume these constants. Renaming enum constants or changing {@code IDR-xxx} values
 * affects audit reports and dashboards, not the IDA WebSub/REST contract.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * AuditRequestBuilder builder = new AuditRequestBuilder();
 * builder.setAction(AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE)
 *        .setModule(AuditModules.ID_REPO_CORE_SERVICE);
 * </pre>
 * <p>
 * Prefer {@link io.mosip.idrepository.core.helper.AuditHelper} from service layers; it
 * selects the matching {@link AuditEvents} / {@link AuditModules} pair and publishes
 * via {@link io.mosip.idrepository.core.builder.AuditRequestBuilder}.
 * </p>
 *
 * @author Manoj SP
 * @see AuditModules
 * @see io.mosip.idrepository.core.builder.AuditRequestBuilder
 * @see io.mosip.idrepository.core.helper.AuditHelper
 */
@Getter
public enum AuditEvents {

	/** Identity create API request/response. */
	CREATE_IDENTITY_REQUEST_RESPONSE("IDR-001", "System Event"),

	/** Identity update API request/response. */
	UPDATE_IDENTITY_REQUEST_RESPONSE("IDR-002", "System Event"),

	/** Identity retrieve by UIN request/response. */
	RETRIEVE_IDENTITY_REQUEST_RESPONSE_UIN("IDR-003", "System Event"),

	/** Identity retrieve by RID request/response. */
	RETRIEVE_IDENTITY_REQUEST_RESPONSE_RID("IDR-004", "System Event"),

	/** VID creation. */
	CREATE_VID("IDR-005", "System Event"),

	/** VID retrieve by UIN. */
	RETRIEVE_VID_UIN("IDR-006", "System Event"),

	/** VID revocation. */
	REVOKE_VID("IDR-007", "System Event"),

	/** VID regeneration. */
	REGENERATE_VID("IDR-008", "System Event"),

	/** VID status update. */
	UPDATE_VID_STATUS("IDR-009", "System Event"),

	/** VID deactivation. */
	DEACTIVATE_VID("IDR-010", "System Event"),

	/** VID reactivation. */
	REACTIVATE_VID("IDR-011", "System Event"),

	/** UIN retrieve by VID. */
	RETRIEVE_UIN_VID("IDR-012", "System Event"),

	/** Credential request creation. */
	CREATING_CREDENTIAL_REQUEST("IDR-013", "System Event"),

	/** Credential request cancellation. */
	CANCEL_CREDENTIAL_REQUEST("IDR-014", "System Event"),

	/** Credential issuance (credential-service). */
	CREATE_CREDENTIAL("IDR-015", "System Event"),

	/** Auth-type status update request/response. */
	UPDATE_AUTH_TYPE_STATUS_REQUEST_RESPONSE("IDR-016", "System Event"),

	/** Credential request update. */
	UPDATE_CREDENTIAL_REQUEST("IDR-017", "System Event"),

	/** Credential request retry. */
	RETRY_CREDENTIAL_REQUEST("IDR-018", "System Event"),

	/** Draft identity create request/response. */
	CREATE_DRAFT_REQUEST_RESPONSE("IDR-019", "System Event"),

	/** Draft identity update request/response. */
	UPDATE_DRAFT_REQUEST_RESPONSE("IDR-020", "System Event"),

	/** Draft publish request/response. */
	PUBLISH_DRAFT_REQUEST_RESPONSE("IDR-021", "System Event"),

	/** Draft discard request/response. */
	DISCARD_DRAFT_REQUEST_RESPONSE("IDR-022", "System Event"),

	/** Draft existence check request/response. */
	HAS_DRAFT_REQUEST_RESPONSE("IDR-023", "System Event"),

	/** Draft retrieve request/response. */
	GET_DRAFT_REQUEST_RESPONSE("IDR-024", "System Event"),

	/** Draft biometric extraction request/response. */
	EXTRACT_BIOMETRICS_DRAFT_REQUEST_RESPONSE("IDR-025", "System Event"),

	/** RID lookup by individual ID. */
	GET_RID_BY_INDIVIDUALID("IDR-026", "System Event"),

	/** Draft UIN retrieve request/response. */
	GET_DRAFT_UIN_REQUEST_RESPONSE("IDR-027", "System Event"),

	/** ID/VID metadata retrieval. */
	ID_VID_METADATA("IDR-029", "System Event");

	/**
	 * MOSIP audit event ID (e.g. {@code IDR-001}).
	 * -- GETTER --
	 *
	 * @return the MOSIP audit event ID
	 */
	private final String eventId;

	/**
	 * Audit event category (typically {@code System Event}).
	 * -- GETTER --
	 *
	 * @return the audit event category
	 */
	private final String eventType;

	/**
	 * Creates an audit event constant with its MOSIP event ID and category.
	 *
	 * @param eventId   MOSIP audit event ID (e.g. {@code IDR-001})
	 * @param eventType audit event category (typically {@code System Event})
	 */
	private AuditEvents(String eventId, String eventType) {
		this.eventId = eventId;
		this.eventType = eventType;
	}

	/**
	 * Returns the enum constant name as the human-readable audit event name.
	 *
	 * @return the enum constant name as the event name
	 */
	public String getEventName() {
		return this.name();
	}
}
