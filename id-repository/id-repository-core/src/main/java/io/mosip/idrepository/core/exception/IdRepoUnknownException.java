package io.mosip.idrepository.core.exception;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;

/**
 * Checked exception representing an unclassified or unexpected ID Repository failure.
 * <p>
 * Used as a fallback when the root cause cannot be mapped to a more specific
 * {@link IdRepoAppException} subtype. Typically thrown or wrapped by
 * {@link IdRepoExceptionHandler#handleAllExceptions(Exception, org.springframework.web.context.request.WebRequest)}
 * with {@link IdRepoErrorConstants#UNKNOWN_ERROR}.
 * </p>
 *
 * @see IdRepoAppException
 * @see IdRepoExceptionHandler
 * @see IdRepoErrorConstants#UNKNOWN_ERROR
 *
 * @author Manoj SP
 */
public class IdRepoUnknownException extends IdRepoAppException {

	/** Serialization version UID. */
	private static final long serialVersionUID = 4349577830351654726L;

	/**
	 * Creates an empty unknown exception instance.
	 */
	public IdRepoUnknownException() {
		super();
	}

	/**
	 * Creates an unknown exception with the given error code and message.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable error message
	 */
	public IdRepoUnknownException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Creates an unknown exception with error details and root cause.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable error message
	 * @param rootCause    underlying cause of the failure
	 */
	public IdRepoUnknownException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}

	/**
	 * Creates an unknown exception from a predefined {@link IdRepoErrorConstants} entry.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 */
	public IdRepoUnknownException(IdRepoErrorConstants exceptionConstant) {
		super(exceptionConstant);
	}

	/**
	 * Creates an unknown exception from a predefined constant with root cause.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 * @param rootCause         underlying cause of the failure
	 */
	public IdRepoUnknownException(IdRepoErrorConstants exceptionConstant, Throwable rootCause) {
		super(exceptionConstant, rootCause);
	}
}