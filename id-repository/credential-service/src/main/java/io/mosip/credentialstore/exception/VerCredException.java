package io.mosip.credentialstore.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;

public class VerCredException extends BaseUncheckedException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	

	public VerCredException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
    }

	public VerCredException(String errorCode, String errorMessage, Throwable e) {
		super(errorCode, errorMessage, e);
    }
}
