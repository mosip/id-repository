package io.mosip.credentialstore.exception;

import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

public class CredentialFormatterException extends BaseCheckedException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CredentialFormatterException() {
		super(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorMessage());
    }

	public CredentialFormatterException(String message) {
		super(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorCode(),
                message);
    }

	public CredentialFormatterException(Throwable e) {
		super(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorMessage(), e);
    }

	public CredentialFormatterException(String errorMessage, Throwable t) {
		super(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorCode(), errorMessage, t);
    }
}
