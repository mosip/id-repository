package io.mosip.idrepository.credential.store.exception;

import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

/**
 * Raised when identity attribute formatting, masking, or biometric filtering fails.
 * <p>
 * Typically wraps JSON, VID, or CBEFF errors from {@link io.mosip.idrepository.credential.store.provider.CredentialProvider}.
 * </p>
 */
public class CredentialFormatterException extends BaseCheckedException {

	private static final long serialVersionUID = 3748592016384750192L;

	/** Creates exception with default formatter error code. */
	public CredentialFormatterException() {
		super(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorMessage());
	}

	/**
	 * Creates exception with custom message.
	 *
	 * @param message formatter failure detail
	 */
	public CredentialFormatterException(String message) {
		super(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorCode(), message);
	}

	/**
	 * Creates exception with default message and root cause.
	 *
	 * @param e underlying formatting error
	 */
	public CredentialFormatterException(Throwable e) {
		super(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorMessage(), e);
	}

	/**
	 * Creates exception with custom message and root cause.
	 *
	 * @param errorMessage formatter failure detail
	 * @param t            underlying error
	 */
	public CredentialFormatterException(String errorMessage, Throwable t) {
		super(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorCode(), errorMessage, t);
	}
}
