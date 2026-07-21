package io.mosip.idrepository.credential.request.constant;

/**
 * Business and integration error codes for credential-request module ({@code IDR-CRG-*}).
 */
public enum CredentialRequestErrorCodes {

	/** JPA or DAO failure accessing {@code credential_transaction}. */
	DATA_ACCESS_LAYER_EXCEPTION("IDR-CRG-001", "data access layer exception"),

	/** Outbound REST call to credential store or cryptomanager failed. */
	API_NOT_ACCESSIBLE_EXCEPTION("IDR-CRG-002", "API not accessible"),

	/** Required request id path/query parameter is missing. */
	REQUEST_ID_ERROR("IDR-CRG-003", "request id is not present"),

	/** Request id is already in a terminal or non-cancellable state. */
	REQUEST_ID_PROCESSED_ERROR("IDR-CRG-004", "request id already processed"),

	/** Unclassified failure in credential-request processing. */
	UNKNOWN_EXCEPTION("IDR-CRG-005", "unknown exception"),

	/** WebSub status callback could not update queue row. */
	CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION("IDR-CRG-006", "Credential status update failed"),

	/** JSON or stream IO failure during request handling. */
	IO_EXCEPTION("IDR-CRG-007", "IO exception"),

	/** Batch reprocess retry ceiling exceeded for a request id. */
	RETRY_COUNT_EXCEEDED("IDR-CRG-007", "retry count exceeded"),

	/** Effective-time filter parameter could not be parsed as ISO datetime. */
	DATE_PARSE_ERROR("IDR-CRG-008", "Date Parsing Error format should be yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),

	/** Paginated status query returned no rows. */
	DATA_NOT_FOUND("IDR-CRG-009", "No records found"),

	/** Queue payload encrypt/decrypt via keymanager failed. */
	ENCRYPTION_DECRYPTION_FAILED("IDR-CRG-010", "Failed to encrypt/decrypt data using Keymanager");

	private final String errorCode;
	private final String errorMessage;

	CredentialRequestErrorCodes(final String errorCode, final String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	/**
	 * Returns MOSIP error code (e.g. {@code IDR-CRG-002}).
	 *
	 * @return error code string
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Returns default English error message.
	 *
	 * @return error message
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
}
