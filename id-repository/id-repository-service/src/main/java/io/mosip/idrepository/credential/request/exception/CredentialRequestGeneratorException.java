package io.mosip.idrepository.credential.request.exception;

import io.mosip.idrepository.credential.request.constant.CredentialRequestErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

/**
 * Checked exception for recoverable failures in the credential-request stage of the MOSIP
 * issuance pipeline.
 * <p>
 * The credential-request module sits between identity status updates and credential-store
 * issuance: it persists queue rows in {@code credential_transaction}, runs Spring Batch
 * tasklets, and accepts WebSub callbacks on {@code CREDENTIAL_STATUS_UPDATE}. This exception
 * is raised when those operations fail in a way callers are expected to handle explicitly
 * (for example, translating to a MOSIP error response or aborting a status update).
 * </p>
 * <p>
 * Typical raise points include {@link io.mosip.idrepository.credential.request.service.impl.CredentialRequestServiceImpl#updateCredentialStatus}
 * when a WebSub status event cannot be applied to the queue row, and service-layer validation
 * that must propagate as a checked failure. Error codes are drawn from
 * {@link CredentialRequestErrorCodes}; the default code is
 * {@link CredentialRequestErrorCodes#CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION}.
 * </p>
 *
 * @see CredentialRequestGeneratorUncheckedException
 * @see CredentialRequestErrorCodes
 */
public class CredentialRequestGeneratorException extends BaseCheckedException {

	private static final long serialVersionUID = -6172930485710293846L;

	/**
	 * Creates an exception with the default business error code
	 * ({@link CredentialRequestErrorCodes#CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION}).
	 */
	public CredentialRequestGeneratorException() {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(),
				CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorMessage());
	}

	/**
	 * Creates an exception with the default error code and a custom message.
	 *
	 * @param message detail describing why the credential-request operation failed
	 */
	public CredentialRequestGeneratorException(String message) {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(), message);
	}

	/**
	 * Creates an exception with the default error code and message, wrapping the root cause.
	 *
	 * @param e underlying failure (DAO, REST, parsing, etc.)
	 */
	public CredentialRequestGeneratorException(Throwable e) {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(),
				CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorMessage(), e);
	}

	/**
	 * Creates an exception with the default error code, a custom message, and root cause.
	 *
	 * @param errorMessage detail describing the failure
	 * @param t            underlying cause
	 */
	public CredentialRequestGeneratorException(String errorMessage, Throwable t) {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(), errorMessage,
				t);
	}

	/**
	 * Creates an exception bound to a specific {@link CredentialRequestErrorCodes} entry.
	 *
	 * @param error enumerated error code and default message for the failure category
	 */
	public CredentialRequestGeneratorException(CredentialRequestErrorCodes error) {
		super(error.getErrorCode(), error.getErrorMessage());
	}

	/**
	 * Creates an exception bound to a specific error code, wrapping the root cause.
	 *
	 * @param error enumerated error code and default message
	 * @param e     underlying failure
	 */
	public CredentialRequestGeneratorException(CredentialRequestErrorCodes error, Throwable e) {
		super(error.getErrorCode(), error.getErrorMessage(), e);
	}
}
