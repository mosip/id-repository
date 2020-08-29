package io.mosip.credentialstore.exception;

import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

public class DataShareException  extends BaseCheckedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DataShareException() {
		super(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage());
    }

    public DataShareException(String message) {
		super(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(),
                message);
    }

    public DataShareException(Throwable e) {
		super(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage(), e);
    }

    public DataShareException(String errorMessage, Throwable t) {
		super(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(), errorMessage, t);
    }

}
