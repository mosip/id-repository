package io.mosip.credentialstore.exception;


import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class SignatureException extends BaseCheckedException {
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new file not found in destination exception.
	 */
	public SignatureException() {
		super();

	}


	public SignatureException(String errorMessage) {
		super(CredentialServiceErrorCodes.SIGNATURE_EXCEPTION.getErrorCode(), errorMessage);
	}


	public SignatureException(String message, Throwable cause) {
		super(CredentialServiceErrorCodes.SIGNATURE_EXCEPTION.getErrorCode(),message, cause);

	}

	public SignatureException(Throwable t) {
		super(CredentialServiceErrorCodes.SIGNATURE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.SIGNATURE_EXCEPTION.getErrorMessage(), t);
	}
}
