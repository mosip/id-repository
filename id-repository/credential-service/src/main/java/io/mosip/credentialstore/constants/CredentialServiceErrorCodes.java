package io.mosip.credentialstore.constants;

/**
 * The Enum CredentialServiceErrorCodes.
 * 
 * @author Sowmya
 */
public enum CredentialServiceErrorCodes {

	/** The api not accessible exception. */
	API_NOT_ACCESSIBLE_EXCEPTION("CRE-SER-001", "API not accessible"),

	/** The iprepo exception. */
	IPREPO_EXCEPTION("CRE-SER-003", "ID repo response is null"),

	/** The instantiation exception. */
	INSTANTIATION_EXCEPTION("CRE-SER-004", "Error while creating object of JsonValue class"),

	/** The no such field exception. */
	NO_SUCH_FIELD_EXCEPTION("CRE-SER-005", "Could not find the field"),

	/** The credential formatter exception. */
	CREDENTIAL_FORMATTER_EXCEPTION("CRE-SER-006", "exception while formatting"),

	/** The unknown exception. */
	UNKNOWN_EXCEPTION("CRE-SER-007", "unknown exception"),

	/** The policy exception. */
	POLICY_EXCEPTION("CRE-SER-008", "Policy details not present"),

	/** The io exception. */
	IO_EXCEPTION("CRE-SER-009", "IO exception"),
	/** The datashare exception. */
	DATASHARE_EXCEPTION("CRE-SER-011", "Datashare response is null");

	/** The error code. */
	private final String errorCode;

	/** The error message. */
	private final String errorMessage;

	/**
	 * Instantiates a new credential service error codes.
	 *
	 * @param errorCode    the error code
	 * @param errorMessage the error message
	 */
	private CredentialServiceErrorCodes(final String errorCode, final String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	/**
	 * Gets the error code.
	 *
	 * @return the error code
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Gets the error message.
	 *
	 * @return the error message
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
}
