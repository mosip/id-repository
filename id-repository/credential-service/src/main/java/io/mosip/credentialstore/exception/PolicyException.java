package io.mosip.credentialstore.exception;


import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

public class PolicyException extends BaseCheckedException {
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new file not found in destination exception.
	 */
	public PolicyException() {
		super();

	}

	public PolicyException(String errorMessage) {
		super(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorCode(), errorMessage);
	}

	public PolicyException(String message, Throwable cause) {
		super(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorCode() + "", message, cause);

	}

	public PolicyException(Throwable t) {
		super(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorMessage(), t);
	}

	public PolicyException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}
}
