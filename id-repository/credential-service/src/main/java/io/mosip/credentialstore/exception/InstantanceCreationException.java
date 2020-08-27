package io.mosip.credentialstore.exception;

import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

public class InstantanceCreationException extends BaseUncheckedException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InstantanceCreationException() {
		super(CredentialServiceErrorCodes.INSTANTIATION_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.INSTANTIATION_EXCEPTION.getErrorMessage());
    }

	public InstantanceCreationException(String message) {
		super(CredentialServiceErrorCodes.INSTANTIATION_EXCEPTION.getErrorCode(),
                message);
    }

	public InstantanceCreationException(Throwable e) {
		super(CredentialServiceErrorCodes.INSTANTIATION_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.INSTANTIATION_EXCEPTION.getErrorMessage(), e);
    }

	public InstantanceCreationException(String errorMessage, Throwable t) {
		super(CredentialServiceErrorCodes.INSTANTIATION_EXCEPTION.getErrorCode(), errorMessage, t);
    }
}
