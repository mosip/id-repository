package io.mosip.idrepository.credential.request.exception;

import io.mosip.idrepository.credential.request.constant.CredentialRequestErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

/**
 * Raised when outbound HTTP to credential store, cryptomanager, or audit services fails.
 */
public class ApiNotAccessibleException extends BaseCheckedException {

	private static final long serialVersionUID = 3847562910475829103L;

	/** Creates exception with default {@link CredentialRequestErrorCodes#API_NOT_ACCESSIBLE_EXCEPTION} message. */
	public ApiNotAccessibleException() {
		super(CredentialRequestErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
				CredentialRequestErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage());
	}

	/**
	 * Creates exception with custom message (typically HTTP response body).
	 *
	 * @param message error detail from downstream service
	 */
	public ApiNotAccessibleException(String message) {
		super(CredentialRequestErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(), message);
	}

	/**
	 * Creates exception with default message and root cause.
	 *
	 * @param e underlying HTTP client exception
	 */
	public ApiNotAccessibleException(Throwable e) {
		super(CredentialRequestErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
				CredentialRequestErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage(), e);
	}

	/**
	 * Creates exception with custom message and root cause.
	 *
	 * @param errorMessage error detail
	 * @param t            root cause
	 */
	public ApiNotAccessibleException(String errorMessage, Throwable t) {
		super(CredentialRequestErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(), errorMessage, t);
	}
}
