package io.mosip.credentialstore.exception;

import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

public class IdRepoException extends BaseCheckedException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IdRepoException() {
		super(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorMessage());
    }

	public IdRepoException(String message) {
		super(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorCode(),
                message);
    }

	public IdRepoException(Throwable e) {
		super(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorMessage(), e);
    }

	public IdRepoException(String errorMessage, Throwable t) {
		super(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorCode(), errorMessage, t);
    }
}
