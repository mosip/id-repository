package io.mosip.idrepository.credential.store.exception;

import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * Unchecked exception raised when credential JSON assembly cannot instantiate a
 * {@code JsonValue} wrapper for an identity attribute.
 * <p>
 * Identity attributes in MOSIP are often represented as {@code JsonValue} objects (language-tagged
 * demographic nodes). During credential construction,
 * {@link io.mosip.idrepository.credential.store.util.JsonUtil} reflectively creates
 * {@code JsonValue} instances to populate the issuance envelope. This exception is thrown when
 * instantiation fails—commonly due to a missing no-arg constructor, access restrictions, or
 * incompatible attribute type mapping.
 * </p>
 * <p>
 * Maps to error code {@link CredentialServiceErrorCodes#INSTANTIATION_EXCEPTION}
 * ({@code IDR-CRS-004}). Reflects a structural or schema mismatch rather than a downstream
 * service outage.
 * </p>
 *
 * @see CredentialServiceErrorCodes#INSTANTIATION_EXCEPTION
 * @see io.mosip.idrepository.credential.store.util.JsonUtil
 */
public class InstantanceCreationException extends BaseUncheckedException {

	private static final long serialVersionUID = -8629401753840192736L;

	/**
	 * Creates an exception with the default instantiation error code and message.
	 */
	public InstantanceCreationException() {
		super(CredentialServiceErrorCodes.INSTANTIATION_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.INSTANTIATION_EXCEPTION.getErrorMessage());
	}

	/**
	 * Creates an exception with the default error code and a custom detail message.
	 *
	 * @param message typically describes which attribute class could not be instantiated
	 */
	public InstantanceCreationException(String message) {
		super(CredentialServiceErrorCodes.INSTANTIATION_EXCEPTION.getErrorCode(),
				message);
	}

	/**
	 * Creates an exception with the default error code and message, wrapping the root cause.
	 *
	 * @param e usually {@link java.lang.InstantiationException} or
	 *          {@link java.lang.IllegalAccessException}
	 */
	public InstantanceCreationException(Throwable e) {
		super(CredentialServiceErrorCodes.INSTANTIATION_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.INSTANTIATION_EXCEPTION.getErrorMessage(), e);
	}

	/**
	 * Creates an exception with the default error code, a custom message, and root cause.
	 *
	 * @param errorMessage detail describing the failed {@code JsonValue} creation
	 * @param t            underlying reflection/instantiation failure
	 */
	public InstantanceCreationException(String errorMessage, Throwable t) {
		super(CredentialServiceErrorCodes.INSTANTIATION_EXCEPTION.getErrorCode(), errorMessage, t);
	}
}
