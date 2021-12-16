package io.mosip.credential.request.generator.exception;

import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class CredentialRequestGeneratorUncheckedException extends BaseUncheckedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CredentialRequestGeneratorUncheckedException() {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(),
				CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorMessage());
	}

	public CredentialRequestGeneratorUncheckedException(String message) {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(), message);
	}

	public CredentialRequestGeneratorUncheckedException(Throwable e) {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(),
				CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorMessage(), e);
	}

	public CredentialRequestGeneratorUncheckedException(String errorMessage, Throwable t) {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(), errorMessage,
				t);
	}

	public CredentialRequestGeneratorUncheckedException(CredentialRequestErrorCodes error) {
		super(error.getErrorCode(), error.getErrorMessage());
	}

	public CredentialRequestGeneratorUncheckedException(CredentialRequestErrorCodes error, Throwable e) {
		super(error.getErrorCode(), error.getErrorMessage(), e);
	}
}
