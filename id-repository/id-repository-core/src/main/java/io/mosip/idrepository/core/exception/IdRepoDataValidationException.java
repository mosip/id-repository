package io.mosip.idrepository.core.exception;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;

/**
 * Checked exception for input data validation failures in ID Repository.
 * <p>
 * Thrown when request payloads, identity attributes, or business rules fail
 * validation before persistence or downstream processing. Handled by
 * {@link IdRepoExceptionHandler#handleIdAppException(IdRepoAppException, org.springframework.web.context.request.WebRequest)}.
 * </p>
 *
 * @see IdRepoAppException
 * @see IdRepoErrorConstants#INVALID_INPUT_PARAMETER
 * @see IdRepoErrorConstants#MISSING_INPUT_PARAMETER
 *
 * @author Manoj SP
 */
public class IdRepoDataValidationException extends IdRepoAppException {

	/** Serialization version UID. */
	private static final long serialVersionUID = -637919650941847283L;

	/**
	 * Creates an empty data validation exception instance.
	 */
	public IdRepoDataValidationException() {
		super();
	}

	/**
	 * Creates a data validation exception with the given error code and message.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable validation error message
	 */
	public IdRepoDataValidationException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Creates a data validation exception with error details and root cause.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable validation error message
	 * @param rootCause    underlying parsing or constraint failure
	 */
	public IdRepoDataValidationException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}

	/**
	 * Creates a data validation exception from a predefined {@link IdRepoErrorConstants} entry.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 */
	public IdRepoDataValidationException(IdRepoErrorConstants exceptionConstant) {
		super(exceptionConstant);
	}

	/**
	 * Creates a data validation exception from a predefined constant with root cause.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 * @param rootCause         underlying parsing or constraint failure
	 */
	public IdRepoDataValidationException(IdRepoErrorConstants exceptionConstant, Throwable rootCause) {
		super(exceptionConstant, rootCause);
	}

}
