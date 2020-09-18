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
	CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION("IDR-CRG-005", "Credential status update failed");
	
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
