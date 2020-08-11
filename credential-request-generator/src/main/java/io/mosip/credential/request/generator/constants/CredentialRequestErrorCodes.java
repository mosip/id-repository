package io.mosip.credential.request.generator.constants;

public enum CredentialRequestErrorCodes {
	DATA_ACCESS_LAYER_EXCEPTION("CRE-REQ-001", "data access layer exception"),
	API_NOT_ACCESSIBLE_EXCEPTION("CRE-REQ-002", "API not accessible");
	
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
