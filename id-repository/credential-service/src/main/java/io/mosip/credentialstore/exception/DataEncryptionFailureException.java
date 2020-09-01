package io.mosip.credentialstore.exception;


import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;


public class DataEncryptionFailureException extends BaseCheckedException {

	/** Serializable version Id. */
	private static final long serialVersionUID = 1L;

	public DataEncryptionFailureException() {
		super(CredentialServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorMessage());
	}

	public DataEncryptionFailureException(Throwable t) {
		super(CredentialServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorMessage(), t);
	}

	/**
	 * @param message
	 *            Message providing the specific context of the error.
	 * @param cause
	 *            Throwable cause for the specific exception
	 */
	public DataEncryptionFailureException(String message, Throwable cause) {
		super(CredentialServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(), message, cause);

	}

	public DataEncryptionFailureException(String errorMessage) {
		super(CredentialServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(), errorMessage);
	}

}