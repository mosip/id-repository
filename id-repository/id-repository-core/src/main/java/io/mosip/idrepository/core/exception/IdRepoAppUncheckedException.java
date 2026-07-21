package io.mosip.idrepository.core.exception;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * Base unchecked exception for ID Repository runtime errors.
 * <p>
 * Extends {@link BaseUncheckedException} and is handled globally by
 * {@link IdRepoExceptionHandler#handleIdAppUncheckedException(IdRepoAppUncheckedException, org.springframework.web.context.request.WebRequest)}.
 * Used for non-recoverable failures that should not force callers to declare checked exceptions.
 * </p>
 *
 * @see IdRepoAppException
 * @see IdRepoExceptionHandler
 * @see IdRepoErrorConstants
 *
 * @author Manoj SP
 */
public class IdRepoAppUncheckedException extends BaseUncheckedException {

	/** Serialization version UID. */
	private static final long serialVersionUID = 6748760277721155095L;

	/**
	 * Creates an empty unchecked exception instance.
	 */
	public IdRepoAppUncheckedException() {
		super();
	}

	/**
	 * Creates an unchecked exception with the given error code and message.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable error message
	 */
	public IdRepoAppUncheckedException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Creates an unchecked exception with error details and root cause.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable error message
	 * @param rootCause    underlying cause of the failure
	 */
	public IdRepoAppUncheckedException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}

	/**
	 * Creates an unchecked exception from a predefined {@link IdRepoErrorConstants} entry.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 */
	public IdRepoAppUncheckedException(IdRepoErrorConstants exceptionConstant) {
		this(exceptionConstant.getErrorCode(), exceptionConstant.getErrorMessage());
	}

	/**
	 * Creates an unchecked exception from a predefined constant with root cause.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 * @param rootCause         underlying cause of the failure
	 */
	public IdRepoAppUncheckedException(IdRepoErrorConstants exceptionConstant, Throwable rootCause) {
		this(exceptionConstant.getErrorCode(), exceptionConstant.getErrorMessage(), rootCause);
	}
}