package io.mosip.credential.request.generator.exception;

import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

public class CredentialRequestGeneratorException extends BaseCheckedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CredentialRequestGeneratorException() {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(),
				CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorMessage());
	}

	public CredentialRequestGeneratorException(String message) {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(), message);
	}

	public CredentialRequestGeneratorException(Throwable e) {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(),
				CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorMessage(), e);
	}

	public CredentialRequestGeneratorException(String errorMessage, Throwable t) {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(), errorMessage,
				t);
	}

	public CredentialRequestGeneratorException(CredentialRequestErrorCodes error) {
		super(error.getErrorCode(), error.getErrorMessage());
	}

	public CredentialRequestGeneratorException(CredentialRequestErrorCodes error, Throwable e) {
		super(error.getErrorCode(), error.getErrorMessage(), e);
	}
}
