package io.mosip.credential.request.generator.exception;

import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

public class CredentialrRequestGeneratorException  extends BaseCheckedException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CredentialrRequestGeneratorException() {
        super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(),
        		CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorMessage());
    }

    public CredentialrRequestGeneratorException(String message) {
        super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(),
                message);
    }

    public CredentialrRequestGeneratorException(Throwable e) {
        super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(),
        		CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorMessage(), e);
    }

    public CredentialrRequestGeneratorException(String errorMessage, Throwable t) {
        super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(), errorMessage, t);
    }
}
