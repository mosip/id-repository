package io.mosip.idrepository.credential.store.exception;

import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

/**
 * Raised when cryptomanager PIN/ZK encryption fails during credential attribute protection.
 * <p>
 * Error codes are defined in {@link CredentialServiceErrorCodes#DATA_ENCRYPTION_FAILURE_EXCEPTION}.
 * </p>
 */
public class DataEncryptionFailureException extends BaseCheckedException {

	private static final long serialVersionUID = -1592837465019283746L;

	/** Creates exception with default credential-store encryption error code. */
	public DataEncryptionFailureException() {
		super(CredentialServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorMessage());
	}

	/**
	 * Creates exception with default code and the given cause.
	 *
	 * @param t root cause from cryptomanager client
	 */
	public DataEncryptionFailureException(Throwable t) {
		super(CredentialServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorMessage(), t);
	}

	/**
	 * Creates exception with custom message and cause.
	 *
	 * @param message context-specific failure detail
	 * @param cause   root cause
	 */
	public DataEncryptionFailureException(String message, Throwable cause) {
		super(CredentialServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(), message, cause);
	}

	/**
	 * Creates exception with custom message.
	 *
	 * @param errorMessage context-specific failure detail
	 */
	public DataEncryptionFailureException(String errorMessage) {
		super(CredentialServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(), errorMessage);
	}
}
