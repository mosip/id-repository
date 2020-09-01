package io.mosip.credential.request.generator.exception;

import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;


public class ApiNotAccessibleException extends BaseCheckedException {

    public ApiNotAccessibleException() {
        super(CredentialRequestErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
        		CredentialRequestErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage());
    }

    public ApiNotAccessibleException(String message) {
        super(CredentialRequestErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
                message);
    }

    public ApiNotAccessibleException(Throwable e) {
        super(CredentialRequestErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
        		CredentialRequestErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage(), e);
    }

    public ApiNotAccessibleException(String errorMessage, Throwable t) {
        super(CredentialRequestErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(), errorMessage, t);
    }


}
