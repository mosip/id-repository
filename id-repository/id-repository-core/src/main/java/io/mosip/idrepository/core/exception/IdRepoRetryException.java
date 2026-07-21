package io.mosip.idrepository.core.exception;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * Unchecked exception used to signal that an outbound REST call should be retried.
 * <p>
 * Thrown by {@link io.mosip.idrepository.core.helper.RestHelper} on transient failures
 * (connection timeout, HTTP 403, HTTP 5xx) and consumed by the kernel {@code @WithRetry}
 * aspect to trigger automatic retry with backoff.
 * </p>
 *
 * @see io.mosip.idrepository.core.helper.RestHelper#requestSync(io.mosip.idrepository.core.dto.RestRequestDTO)
 * @see RestServiceException
 * @see AuthenticationException
 *
 * @author Manoj SP
 */
public class IdRepoRetryException extends BaseUncheckedException {

	/** Serialization version UID. */
	private static final long serialVersionUID = 6748760277721155095L;

	/**
	 * Creates an empty retry exception instance.
	 */
	public IdRepoRetryException() {
		super();
	}

	/**
	 * Creates a retry exception with the given error code and message.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable error message
	 */
	public IdRepoRetryException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Creates a retry exception with error details and root cause.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable error message
	 * @param rootCause    underlying transient failure
	 */
	public IdRepoRetryException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}

	/**
	 * Creates a retry exception from a predefined {@link IdRepoErrorConstants} entry.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 */
	public IdRepoRetryException(IdRepoErrorConstants exceptionConstant) {
		this(exceptionConstant.getErrorCode(), exceptionConstant.getErrorMessage());
	}

	/**
	 * Wraps a checked kernel exception as a retry-triggering unchecked exception.
	 *
	 * @param rootCause checked exception from a failed REST or service call
	 */
	public IdRepoRetryException(BaseCheckedException rootCause) {
		this(rootCause.getErrorCode(), rootCause.getErrorText(), rootCause);
	}

	/**
	 * Wraps an unchecked kernel exception as a retry-triggering unchecked exception.
	 *
	 * @param rootCause unchecked exception from a failed REST or service call
	 */
	public IdRepoRetryException(BaseUncheckedException rootCause) {
		this(rootCause.getErrorCode(), rootCause.getErrorText(), rootCause);
	}
}