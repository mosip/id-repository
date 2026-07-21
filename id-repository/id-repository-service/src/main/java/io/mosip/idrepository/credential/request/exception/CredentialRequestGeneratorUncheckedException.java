package io.mosip.idrepository.credential.request.exception;

import io.mosip.idrepository.credential.request.constant.CredentialRequestErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * Unchecked exception for non-recoverable or infrastructure failures in the credential-request
 * stage of the MOSIP issuance pipeline.
 * <p>
 * Used where declaring {@link CredentialRequestGeneratorException} on every call site would be
 * impractical—typically low-level utilities that interact with cryptomanager during queue
 * payload encrypt/decrypt ({@link io.mosip.idrepository.credential.request.util.CryptoUtil}).
 * Failures here prevent a credential request from being safely queued or decrypted and generally
 * indicate misconfiguration, keymanager outage, or corrupt encrypted data rather than a
 * business-rule rejection.
 * </p>
 * <p>
 * Error codes are drawn from {@link CredentialRequestErrorCodes}; the default is
 * {@link CredentialRequestErrorCodes#CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION}.
 * </p>
 *
 * @see CredentialRequestGeneratorException
 * @see CredentialRequestErrorCodes
 */
public class CredentialRequestGeneratorUncheckedException extends BaseUncheckedException {

	private static final long serialVersionUID = 5281947360158472930L;

	/**
	 * Creates an exception with the default business error code
	 * ({@link CredentialRequestErrorCodes#CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION}).
	 */
	public CredentialRequestGeneratorUncheckedException() {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(),
				CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorMessage());
	}

	/**
	 * Creates an exception with the default error code and a custom message.
	 *
	 * @param message detail describing the failure (often includes cryptomanager response text)
	 */
	public CredentialRequestGeneratorUncheckedException(String message) {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(), message);
	}

	/**
	 * Creates an exception with the default error code and message, wrapping the root cause.
	 *
	 * @param e underlying failure (HTTP, crypto, IO, etc.)
	 */
	public CredentialRequestGeneratorUncheckedException(Throwable e) {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(),
				CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorMessage(), e);
	}

	/**
	 * Creates an exception with the default error code, a custom message, and root cause.
	 *
	 * @param errorMessage detail describing the failure
	 * @param t            underlying cause
	 */
	public CredentialRequestGeneratorUncheckedException(String errorMessage, Throwable t) {
		super(CredentialRequestErrorCodes.CREDENTIAL_REQUEST_GENERATOR_BUSINESS_EXCEPTION.getErrorCode(), errorMessage,
				t);
	}

	/**
	 * Creates an exception bound to a specific {@link CredentialRequestErrorCodes} entry.
	 *
	 * @param error enumerated error code and default message (for example
	 *              {@link CredentialRequestErrorCodes#ENCRYPTION_DECRYPTION_FAILED})
	 */
	public CredentialRequestGeneratorUncheckedException(CredentialRequestErrorCodes error) {
		super(error.getErrorCode(), error.getErrorMessage());
	}

	/**
	 * Creates an exception bound to a specific error code, wrapping the root cause.
	 *
	 * @param error enumerated error code and default message
	 * @param e     underlying failure
	 */
	public CredentialRequestGeneratorUncheckedException(CredentialRequestErrorCodes error, Throwable e) {
		super(error.getErrorCode(), error.getErrorMessage(), e);
	}
}
