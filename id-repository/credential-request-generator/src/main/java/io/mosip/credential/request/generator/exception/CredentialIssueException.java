package io.mosip.credential.request.generator.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;

public class CredentialIssueException extends BaseUncheckedException {
	private static final long serialVersionUID = 1L;

	public CredentialIssueException() {
		super();
	}

	/**
	 * Instantiates a new id repo app exception.
	 *
	 * @param errorCode the error code
	 * @param errorMessage the error message
	 */
	public CredentialIssueException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Instantiates a new id repo app exception.
	 *
	 * @param errorCode the error code
	 * @param errorMessage the error message
	 * @param rootCause the root cause
	 */
	public CredentialIssueException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}
}
