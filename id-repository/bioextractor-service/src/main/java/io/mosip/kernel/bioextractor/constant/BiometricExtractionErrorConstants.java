package io.mosip.kernel.bioextractor.constant;

public enum BiometricExtractionErrorConstants {
	
	UNKNOWN_ERROR("BIE-UNK-001", "Unknown Error"),
	MISSING_INPUT_PARAMETER("BIE-DAV-001", "Missing Input Parameter - %s"),
	INVALID_INPUT_PARAMETER("BIE-DAV-002", "Invalid Input Parameter - %s"),
	INVALID_CBEFF("BIE-DAV-001", "Invalid CBEFF"),
	TECHNICAL_ERROR("BIE-SDK-001", "Technical Error in Biometric Extraction"),

	;
	
	private final String errorCode;
	private final String errorMessage;

	private BiometricExtractionErrorConstants(String errorCode, String errorMessage) {
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
