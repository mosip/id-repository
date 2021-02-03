package io.mosip.credential.request.generator.constants;

/**
 * 
 * @author Sowmya
 *
 */
public enum CredentialRequestErrorCodes {
	DATA_ACCESS_LAYER_EXCEPTION("IDR-CRG-001", "data access layer exception"),
	API_NOT_ACCESSIBLE_EXCEPTION("IDR-CRG-002", "API not accessible"),
	REQUEST_ID_ERROR("IDR-CRG-003", "request id is not present"),
	REQUEST_ID_PROCESSED_ERROR("IDR-CRG-004", "request id already processed"),

	UNKNOWN_EXCEPTION("IDR-CRG-004", "unknown exception"),
	CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION("IDR-CRG-005", "Credential status update failed"),
	IO_EXCEPTION("IDR-CRG-006", "IO exception"), RETRY_COUNT_EXCEEDED("IDR-CRG-007", "retry count exceeded"),
	DATE_PARSE_ERROR("IDR-CRG-008", "Date Parsing Error format should be yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
	DATA_NOT_FOUND("IDR-CRG-009", "No records found");
	
	private final String errorCode;
	private final String errorMessage;

	private CredentialRequestErrorCodes(final String errorCode, final String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
