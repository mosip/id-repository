/**
 * 
 */
package io.mosip.idrepository.core.exception;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;

/**
 * @author Loganathan Sekar
 *
 */
public class BiometricExtractionException extends IdRepoAppException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -527809804505218573L;

	/**
	 * @param errorCode
	 * @param errorMessage
	 */
	public BiometricExtractionException(IdRepoErrorConstants errConst) {
		this(errConst.getErrorCode(), errConst.getErrorMessage());
	}

	/**
	 * @param errorCode
	 * @param errorMessage
	 * @param rootCause
	 */
	public BiometricExtractionException(IdRepoErrorConstants errConst, Throwable rootCause) {
		super(errConst.getErrorCode(), errConst.getErrorMessage(), rootCause);
	}
	
	/**
	 * @param errorCode
	 * @param errorMessage
	 */
	public BiometricExtractionException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * @param errorCode
	 * @param errorMessage
	 * @param rootCause
	 */
	public BiometricExtractionException(String errorCode, String errorMessage, Throwable rootCause) {
		super(errorCode, errorMessage, rootCause);
	}

}
