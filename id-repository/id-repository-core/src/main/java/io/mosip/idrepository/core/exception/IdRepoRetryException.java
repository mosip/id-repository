package io.mosip.idrepository.core.exception;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.kernel.core.exception.BaseCheckedException;
import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * The Class IdRepoRetryException - Unchecked exception used to trigger retry
 * in RestHelper.
 *
 * @author Manoj SP
 */
public class IdRepoRetryException extends BaseUncheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 6748760277721155095L;

	/**
	 * Instantiates a new id repo retry exception.
	 */
	public IdRepoRetryException() {
		super();
	}

	/**
	 * Instantiates a new id repo retry exception.
	 *
	 * @param errorCode    the error code
	 * @param errorMessage the error message
	 */
	public IdRepoRetryException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Instantiates a new id repo retry exception.
	 *
	 * @param errorCode    the error code
	 * @param errorMessage the error message
	 * @param rootCause    the root cause
	 */
	public IdRepoRetryException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}

	/**
	 * Instantiates a new id repo retry exception.
	 *
	 * @param exceptionConstant the exception constant
	 */
	public IdRepoRetryException(IdRepoErrorConstants exceptionConstant) {
		this(exceptionConstant.getErrorCode(), exceptionConstant.getErrorMessage());
	}

	/**
	 * Instantiates a new id repo retry exception.
	 *
	 * @param BaseCheckedException the root cause
	 */
	public IdRepoRetryException(BaseCheckedException rootCause) {
		this(rootCause.getErrorCode(), rootCause.getErrorText(), rootCause);
	}

	/**
	 * Instantiates a new id repo retry exception.
	 *
	 * @param BaseUncheckedException the root cause
	 */
	public IdRepoRetryException(BaseUncheckedException rootCause) {
		this(rootCause.getErrorCode(), rootCause.getErrorText(), rootCause);
	}
}
