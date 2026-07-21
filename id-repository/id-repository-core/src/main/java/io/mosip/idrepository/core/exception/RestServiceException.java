package io.mosip.idrepository.core.exception;

import java.util.Optional;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;

/**
 * Checked exception for failures during outbound REST service calls.
 * <p>
 * Thrown by {@link io.mosip.idrepository.core.helper.RestHelper} when a remote
 * MOSIP service returns an error response, times out, or returns a body containing
 * an {@code errors} array. Optionally captures the raw and deserialized response
 * body for downstream error handling and logging.
 * </p>
 *
 * @see io.mosip.idrepository.core.helper.RestHelper
 * @see IdRepoRetryException
 * @see IdRepoErrorConstants#CLIENT_ERROR
 * @see IdRepoErrorConstants#SERVER_ERROR
 *
 * @author Manoj SP
 */
public class RestServiceException extends IdRepoAppException {

	/** Serialization version UID. */
	private static final long serialVersionUID = 372518972095526748L;

	/** Raw response body string from the failed REST call, if available. */
	private transient String responseBodyAsString;

	/** Deserialized response body object from the failed REST call, if available. */
	private transient Object responseBody;

	/**
	 * Creates an empty REST service exception instance.
	 */
	public RestServiceException() {
		super();
	}

	/**
	 * Creates a REST service exception from a predefined error constant.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 */
	public RestServiceException(IdRepoErrorConstants exceptionConstant) {
		super(exceptionConstant);
	}

	/**
	 * Creates a REST service exception from a predefined constant with root cause.
	 *
	 * @param exceptionConstant predefined error code and message pair
	 * @param rootCause         underlying HTTP or parsing failure
	 */
	public RestServiceException(IdRepoErrorConstants exceptionConstant, Throwable rootCause) {
		super(exceptionConstant, rootCause);
	}

	/**
	 * Creates a REST service exception with error constant and captured response bodies.
	 *
	 * @param exceptionConstant    predefined error code and message pair
	 * @param responseBodyAsString raw response body as string
	 * @param responseBody         deserialized response body object
	 */
	public RestServiceException(IdRepoErrorConstants exceptionConstant, String responseBodyAsString,
			Object responseBody) {
		super(exceptionConstant);
		this.responseBody = responseBody;
		this.responseBodyAsString = responseBodyAsString;
	}

	/**
	 * Returns the deserialized response body from the failed REST call.
	 *
	 * @return optional containing the response body, or empty if not captured
	 */
	public Optional<Object> getResponseBody() {
		return Optional.ofNullable(responseBody);
	}

	/**
	 * Returns the raw response body string from the failed REST call.
	 *
	 * @return optional containing the response body string, or empty if not captured
	 */
	public Optional<String> getResponseBodyAsString() {
		return Optional.ofNullable(responseBodyAsString);
	}
}