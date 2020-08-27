package io.mosip.credentialstore.exception;

import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class CredentialServiceException extends BaseUncheckedException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CredentialServiceException() {
		super(CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
    }

	public CredentialServiceException(String errorCode, String message) {
		super(errorCode,
                message);
    }

	public CredentialServiceException(String errorCode, String message, Throwable e) {
		super(errorCode, message, e);
    }

	public CredentialServiceException(String errorMessage, Throwable t) {
		super(CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorCode(), errorMessage, t);
    }
}
