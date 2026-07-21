package io.mosip.idrepository.credential.store.exception;

import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * Unchecked exception raised when credential JSON assembly cannot locate a required identity
 * attribute field via reflection.
 * <p>
 * Partner credential policies declare shareable KYC attributes that must be copied from the
 * identity JSON into the issuance envelope. {@link io.mosip.idrepository.credential.store.util.JsonUtil}
 * uses reflection to read nested {@code JsonValue} fields; if the attribute path does not exist on
 * the retrieved identity document (schema mismatch, missing demographic data, or policy/identity
 * drift), a {@link java.lang.NoSuchFieldException} is wrapped as this exception.
 * </p>
 * <p>
 * Maps to error code {@link CredentialServiceErrorCodes#NO_SUCH_FIELD_EXCEPTION}
 * ({@code IDR-CRS-005}). Indicates a data or configuration problem rather than a transient
 * infrastructure fault.
 * </p>
 *
 * @see CredentialServiceErrorCodes#NO_SUCH_FIELD_EXCEPTION
 * @see io.mosip.idrepository.credential.store.util.JsonUtil
 */
public class FieldNotFoundException extends BaseUncheckedException {

	private static final long serialVersionUID = -4085729163847501926L;

	/**
	 * Creates an exception with the default field-not-found error code and message.
	 */
	public FieldNotFoundException() {
		super(CredentialServiceErrorCodes.NO_SUCH_FIELD_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.NO_SUCH_FIELD_EXCEPTION.getErrorMessage());
	}

	/**
	 * Creates an exception with the default error code and a custom detail message.
	 *
	 * @param message typically includes the missing field or attribute name
	 */
	public FieldNotFoundException(String message) {
		super(CredentialServiceErrorCodes.NO_SUCH_FIELD_EXCEPTION.getErrorCode(),
				message);
	}

	/**
	 * Creates an exception with the default error code and message, wrapping the root cause.
	 *
	 * @param e usually a {@link java.lang.NoSuchFieldException} from reflection
	 */
	public FieldNotFoundException(Throwable e) {
		super(CredentialServiceErrorCodes.NO_SUCH_FIELD_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.NO_SUCH_FIELD_EXCEPTION.getErrorMessage(), e);
	}

	/**
	 * Creates an exception with the default error code, a custom message, and root cause.
	 *
	 * @param errorMessage detail describing which attribute path was not found
	 * @param t            underlying reflection failure
	 */
	public FieldNotFoundException(String errorMessage, Throwable t) {
		super(CredentialServiceErrorCodes.NO_SUCH_FIELD_EXCEPTION.getErrorCode(), errorMessage, t);
	}
}
