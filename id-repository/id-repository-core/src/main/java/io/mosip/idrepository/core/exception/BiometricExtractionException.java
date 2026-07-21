package io.mosip.idrepository.core.exception;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;

/**
 * Checked exception raised when biometric template extraction or formatting fails.
 * <p>
 * Thrown during identity processing when CBEFF/biometric data cannot be extracted
 * or converted to the format required for credential issuance or storage.
 * </p>
 *
 * @see IdRepoAppException
 * @see IdRepoErrorConstants
 *
 * @author Loganathan Sekar
 */
public class BiometricExtractionException extends IdRepoAppException {

	/** Serialization version UID. */
	private static final long serialVersionUID = -527809804505218573L;

	/**
	 * Creates a biometric extraction exception from a predefined error constant.
	 *
	 * @param errConst predefined error code and message pair
	 */
	public BiometricExtractionException(IdRepoErrorConstants errConst) {
		this(errConst.getErrorCode(), errConst.getErrorMessage());
	}

	/**
	 * Creates a biometric extraction exception from a predefined constant with root cause.
	 *
	 * @param errConst  predefined error code and message pair
	 * @param rootCause underlying extraction or parsing failure
	 */
	public BiometricExtractionException(IdRepoErrorConstants errConst, Throwable rootCause) {
		super(errConst.getErrorCode(), errConst.getErrorMessage(), rootCause);
	}

	/**
	 * Creates a biometric extraction exception with the given error code and message.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable error message
	 */
	public BiometricExtractionException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Creates a biometric extraction exception with error details and root cause.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable error message
	 * @param rootCause    underlying extraction or parsing failure
	 */
	public BiometricExtractionException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}
}