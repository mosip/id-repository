package io.mosip.credentialstore.constants;

/**
 * The Enum CredentialServiceErrorCodes.
 * 
 * @author Sowmya
 */
public enum CredentialServiceErrorCodes {

	/** The api not accessible exception. */
	API_NOT_ACCESSIBLE_EXCEPTION("IDR-CRS-001", "API not accessible"),

	/** The iprepo exception. */
	IPREPO_EXCEPTION("IDR-CRS-003", "ID repo response is null"),

	/** The instantiation exception. */
	INSTANTIATION_EXCEPTION("IDR-CRS-004", "Error while creating object of JsonValue class"),

	/** The no such field exception. */
	NO_SUCH_FIELD_EXCEPTION("IDR-CRS-005", "Could not find the field"),

	/** The credential formatter exception. */
	CREDENTIAL_FORMATTER_EXCEPTION("IDR-CRS-006", "exception while formatting"),

	/** The unknown exception. */
	UNKNOWN_EXCEPTION("IDR-CRS-007", "unknown exception"),

	/** The policy exception. */
	POLICY_EXCEPTION("IDR-CRS-008", "Failed to get policy details"),

	/** The io exception. */
	IO_EXCEPTION("IDR-CRS-009", "IO exception"),
	/** The datashare exception. */
	DATASHARE_EXCEPTION("IDR-CRS-011", "Datashare response is null"),
	
	SIGNATURE_EXCEPTION("IDR-CRS-012", "Failed to generate digital signature"),
	
	DATA_ENCRYPTION_FAILURE_EXCEPTION("IDR-CRS-013", "Data Encryption failed"),
	
	WEBSUB_FAIL_EXCEPTION("IDR-CRS-014", "Websub event failed"),

	POLICY_SCHEMA_VALIDATION_EXCEPTION("IDR-CRS-015", "Policy Schema validation failed"),

	VC_CONTEXT_FILE_NOT_FOUND("IDR-CRS-016", "Error downloading VC Context file or JSON parsing error."),

	PIN_NOT_PROVIDER("IDR-CRS-017", "Pin not available to encrypt the data."),

	PARTNER_EXCEPTION("IDR-CRS-008", "Failed to get partner extraction policy details");
	

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
