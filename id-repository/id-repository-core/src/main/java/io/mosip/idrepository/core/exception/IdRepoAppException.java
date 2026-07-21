package io.mosip.idrepository.core.exception;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.kernel.core.exception.BaseCheckedException;

/**
 * Base checked exception for ID Repository application errors.
 * <p>
 * Extends {@link io.mosip.kernel.core.exception.BaseCheckedException} and carries
 * MOSIP-standard error codes and messages. Optionally records the API operation
 * (e.g. {@code create}, {@code read}, {@code update}) so that
 * {@link IdRepoExceptionHandler} can populate the correct response {@code id} field.
 * </p>
 *
 * @see IdRepoExceptionHandler#handleIdAppException(IdRepoAppException, org.springframework.web.context.request.WebRequest)
 * @see IdRepoErrorConstants
 * @see IdRepoDataValidationException
 * @see RestServiceException
 *
 * @author Manoj SP
 */
public class IdRepoAppException extends BaseCheckedException {

	/** Serialization version UID. */
	private static final long serialVersionUID = 6748760277721155095L;

	/**
	 * Optional API operation name used to resolve the response {@code id} in error payloads
	 * (e.g. {@code create}, {@code read}, {@code update}, {@code deactivate}).
	 */
	private String operation;

	/**
	 * Creates an empty exception instance.
	 */
	public IdRepoAppException() {
		super();
	}

	/**
	 * Creates an exception with the given error code and message.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable error message
	 */
	public IdRepoAppException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Creates an exception with the given error code, message, and root cause.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable error message
	 * @param rootCause    underlying cause of the failure
	 */
	public IdRepoAppException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}

	/**
	 * Creates an exception from a predefined {@link IdRepoErrorConstants} entry.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 */
	public IdRepoAppException(IdRepoErrorConstants exceptionConstant) {
		this(exceptionConstant.getErrorCode(), exceptionConstant.getErrorMessage());
	}

	/**
	 * Creates an exception from a predefined constant with an underlying cause.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 * @param rootCause         underlying cause of the failure
	 */
	public IdRepoAppException(IdRepoErrorConstants exceptionConstant, Throwable rootCause) {
		this(exceptionConstant.getErrorCode(), exceptionConstant.getErrorMessage(), rootCause);
	}

	/**
	 * Creates an exception with error details and the API operation context.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable error message
	 * @param operation    API operation name for response {@code id} resolution
	 */
	public IdRepoAppException(String errorCode, String errorMessage, String operation) {
		super(errorCode, errorMessage);
		this.operation = operation;
	}

	/**
	 * Creates an exception with error details, root cause, and API operation context.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable error message
	 * @param rootCause    underlying cause of the failure
	 * @param operation    API operation name for response {@code id} resolution
	 */
	public IdRepoAppException(String errorCode, String errorMessage, Throwable rootCause, String operation) {
		super(errorCode, errorMessage, rootCause);
		this.operation = operation;
	}

	/**
	 * Creates an exception from a predefined constant with API operation context.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 * @param operation         API operation name for response {@code id} resolution
	 */
	public IdRepoAppException(IdRepoErrorConstants exceptionConstant, String operation) {
		this(exceptionConstant.getErrorCode(), exceptionConstant.getErrorMessage());
		this.operation = operation;
	}

	/**
	 * Creates an exception from a predefined constant with root cause and operation context.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 * @param rootCause         underlying cause of the failure
	 * @param operation         API operation name for response {@code id} resolution
	 */
	public IdRepoAppException(IdRepoErrorConstants exceptionConstant, Throwable rootCause, String operation) {
		this(exceptionConstant.getErrorCode(), exceptionConstant.getErrorMessage(), rootCause);
		this.operation = operation;
	}

	/**
	 * Returns the API operation associated with this exception, if set.
	 *
	 * @return operation name (e.g. {@code create}, {@code read}), or {@code null} if not set
	 */
	public String getOperation() {
		return operation;
	}
}