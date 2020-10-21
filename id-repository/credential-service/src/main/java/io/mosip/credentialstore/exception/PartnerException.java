package io.mosip.credentialstore.exception;

import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

public class PartnerException extends BaseCheckedException {
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new file not found in destination exception.
	 */
	public PartnerException() {
		super();

	}

	public PartnerException(String errorMessage) {
		super(CredentialServiceErrorCodes.PARTNER_EXCEPTION.getErrorCode(), errorMessage);
	}

	public PartnerException(String message, Throwable cause) {
		super(CredentialServiceErrorCodes.PARTNER_EXCEPTION.getErrorCode() + "", message, cause);

	}

	public PartnerException(Throwable t) {
		super(CredentialServiceErrorCodes.PARTNER_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.PARTNER_EXCEPTION.getErrorMessage(), t);
	}

	public PartnerException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}
}
