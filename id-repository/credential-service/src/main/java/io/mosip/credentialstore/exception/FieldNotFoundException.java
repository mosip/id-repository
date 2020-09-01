package io.mosip.credentialstore.exception;

import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class FieldNotFoundException extends BaseUncheckedException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FieldNotFoundException() {
		super(CredentialServiceErrorCodes.NO_SUCH_FIELD_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.NO_SUCH_FIELD_EXCEPTION.getErrorMessage());
    }

	public FieldNotFoundException(String message) {
		super(CredentialServiceErrorCodes.NO_SUCH_FIELD_EXCEPTION.getErrorCode(),
                message);
    }

	public FieldNotFoundException(Throwable e) {
		super(CredentialServiceErrorCodes.NO_SUCH_FIELD_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.NO_SUCH_FIELD_EXCEPTION.getErrorMessage(), e);
    }

	public FieldNotFoundException(String errorMessage, Throwable t) {
		super(CredentialServiceErrorCodes.NO_SUCH_FIELD_EXCEPTION.getErrorCode(), errorMessage, t);
    }
}
