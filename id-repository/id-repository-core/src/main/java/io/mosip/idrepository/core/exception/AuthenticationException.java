package io.mosip.idrepository.core.exception;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * Unchecked exception for outbound REST authentication failures.
 * <p>
 * Thrown by {@link io.mosip.idrepository.core.helper.RestHelper} when a remote
 * service returns HTTP 401 Unauthorized. Carries the HTTP status code so
 * {@link IdRepoExceptionHandler#handleAuthenticationException(AuthenticationException, org.springframework.web.context.request.WebRequest)}
 * can propagate the appropriate response status to the caller.
 * </p>
 *
 * @see io.mosip.idrepository.core.helper.RestHelper
 * @see IdRepoExceptionHandler#handleAuthenticationException(AuthenticationException, org.springframework.web.context.request.WebRequest)
 * @see io.mosip.idrepository.core.constant.AuthAdapterErrorCode
 *
 * @author Manoj SP
 */
public class AuthenticationException extends BaseUncheckedException {

	/** Serialization version UID. */
	private static final long serialVersionUID = 6748760277721155095L;

	/**
	 * HTTP status code from the failed authentication response (e.g. 401, 403).
	 */
	private int statusCode;

	/**
	 * Creates an empty authentication exception instance.
	 */
	public AuthenticationException() {
		super();
	}

	/**
	 * Creates an authentication exception with error details and HTTP status.
	 *
	 * @param errorCode    MOSIP or auth-adapter error code
	 * @param errorMessage human-readable error message
	 * @param statusCode   HTTP status code from the remote response
	 */
	public AuthenticationException(String errorCode, String errorMessage, int statusCode) {
		super(errorCode, errorMessage);
		this.statusCode = statusCode;
	}

	/**
	 * Creates an authentication exception with error details, root cause, and HTTP status.
	 *
	 * @param errorCode    MOSIP or auth-adapter error code
	 * @param errorMessage human-readable error message
	 * @param rootCause    underlying failure (e.g. token refresh error)
	 * @param statusCode   HTTP status code from the remote response
	 */
	public AuthenticationException(String errorCode, String errorMessage, Throwable rootCause, int statusCode) {
		super(errorCode, errorMessage, rootCause);
		this.statusCode = statusCode;
	}

	/**
	 * Creates an authentication exception from a predefined constant and HTTP status.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 * @param statusCode        HTTP status code from the remote response
	 */
	public AuthenticationException(IdRepoErrorConstants exceptionConstant, int statusCode) {
		this(exceptionConstant.getErrorCode(), exceptionConstant.getErrorMessage(), statusCode);
		this.statusCode = statusCode;
	}

	/**
	 * Creates an authentication exception from a predefined constant with root cause and HTTP status.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 * @param rootCause         underlying failure
	 * @param statusCode        HTTP status code from the remote response
	 */
	public AuthenticationException(IdRepoErrorConstants exceptionConstant, Throwable rootCause, int statusCode) {
		this(exceptionConstant.getErrorCode(), exceptionConstant.getErrorMessage(), rootCause, statusCode);
		this.statusCode = statusCode;
	}

	/**
	 * Returns the HTTP status code associated with this authentication failure.
	 *
	 * @return HTTP status code (e.g. 401); {@code 0} if not set
	 */
	public int getStatusCode() {
		return statusCode;
	}
}