package io.mosip.idrepository.core.constant;

/**
 * The Enum AuditEvents - Contains all the events for auditing.
 *
 * @author Manoj SP
 */
public enum AuditEvents {

	CREATE_IDENTITY_REQUEST_RESPONSE("IDR-001", "System Event"),

	UPDATE_IDENTITY_REQUEST_RESPONSE("IDR-002", "System Event"),

	RETRIEVE_IDENTITY_REQUEST_RESPONSE_UIN("IDR-003", "System Event"),
	
	RETRIEVE_IDENTITY_REQUEST_RESPONSE_RID("IDR-004", "System Event"),
	
	CREATE_VID("IDR-005", "System Event"),
	
	RETRIEVE_VID_UIN("IDR-006", "System Event"),
	
	REVOKE_VID("IDR-007","System Event"),
	
	REGENERATE_VID("IDR-008", "System Event"),
	
	UPDATE_VID_STATUS("IDR-009", "System Event"),
	
	DEACTIVATE_VID("IDR-010", "System Event"),
	
	REACTIVATE_VID("IDR-011", "System Event"),
	
	RETRIEVE_UIN_VID("IDR-012", "System Event"),
	
	CREATING_CREDENTIAL_REQUEST("IDR-013", "System Event"),
	
	CANCEL_CREDENTIAL_REQUEST("IDR-014", "System Event"),
	
	CREATE_CREDENTIAL("IDR-015", "System Event"),
	
	UPDATE_AUTH_TYPE_STATUS_REQUEST_RESPONSE("IDR-016", "System Event"),
	
	UPDATE_CREDENTIAL_REQUEST("IDR-017", "System Event"),

	RETRY_CREDENTIAL_REQUEST("IDR-018", "System Event"),
	
	CREATE_DRAFT_REQUEST_RESPONSE("IDR-019", "System Event"),
	
	UPDATE_DRAFT_REQUEST_RESPONSE("IDR-020", "System Event"),
	
	PUBLISH_DRAFT_REQUEST_RESPONSE("IDR-021", "System Event"),
	
	DISCARD_DRAFT_REQUEST_RESPONSE("IDR-022", "System Event"),
	
	HAS_DRAFT_REQUEST_RESPONSE("IDR-023", "System Event"),
	
	GET_DRAFT_REQUEST_RESPONSE("IDR-024", "System Event"),
	
	EXTRACT_BIOMETRICS_DRAFT_REQUEST_RESPONSE("IDR-025", "System Event"),
	
	GET_RID_BY_INDIVIDUALID("IDR-026", "System Event"),

	GET_DRAFT_UIN_REQUEST_RESPONSE("IDR-027", "System Event"),
	
	REMOVE_ID_STATUS("IDR-028", "System Event");
	
	

	/** The event id. */
	private final String eventId;

	/** The event type. */
	private final String eventType;

	/**
	 * Instantiates a new audit events.
	 *
	 * @param eventId   the event id
	 * @param eventType the event type
	 */
	private AuditEvents(String eventId, String eventType) {
		this.eventId = eventId;
		this.eventType = eventType;
	}

	/**
	 * Gets the event id.
	 *
	 * @return the event id
	 */
	public String getEventId() {
		return eventId;
	}

	/**
	 * Gets the event type.
	 *
	 * @return the event type
	 */
	public String getEventType() {
		return eventType;
	}

	/**
	 * Gets the event name.
	 *
	 * @return the event name
	 */
	public String getEventName() {
		return this.name();
	}

}
	
