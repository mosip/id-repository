package io.mosip.credentialstore.exception;

import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;


public class ApiNotAccessibleException extends BaseCheckedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ApiNotAccessibleException() {
		super(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage());
    }

    public ApiNotAccessibleException(String message) {
		super(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
                message);
    }

    public ApiNotAccessibleException(Throwable e) {
		super(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage(), e);
    }

    public ApiNotAccessibleException(String errorMessage, Throwable t) {
		super(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(), errorMessage, t);
    }


}
