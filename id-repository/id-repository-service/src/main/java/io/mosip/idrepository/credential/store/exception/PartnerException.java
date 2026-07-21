package io.mosip.idrepository.credential.store.exception;

import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

/**
 * Checked exception raised when Partner Management Service (PMS) partner or extraction-policy
 * data required for credential issuance cannot be obtained or is invalid.
 * <p>
 * Before building a partner-specific credential, credential-store loads the partner record and
 * biometric extraction policy from PMS ({@link io.mosip.idrepository.credential.store.util.PolicyUtil}).
 * This exception is thrown when PMS returns a business error, the partner is unknown, or
 * extraction policy metadata cannot be parsed—after HTTP-level failures have been ruled out
 * (those surface as {@link ApiNotAccessibleException}).
 * </p>
 * <p>
 * Maps to error code {@link CredentialServiceErrorCodes#PARTNER_EXCEPTION}
 * ({@code IDR-CRS-008}). Without valid partner policy, attribute filtering and CBEFF extraction
 * format selection cannot proceed.
 * </p>
 *
 * @see CredentialServiceErrorCodes#PARTNER_EXCEPTION
 * @see io.mosip.idrepository.credential.store.util.PolicyUtil
 */
public class PartnerException extends BaseCheckedException {

	private static final long serialVersionUID = -7351940284650192837L;

	/**
	 * Creates an exception with no preset error code or message (kernel base defaults).
	 * Prefer the {@link #PartnerException(String)} overload for consistent {@code IDR-CRS-*} codes.
	 */
	public PartnerException() {
		super();
	}

	/**
	 * Creates an exception with the standard partner error code and a custom detail message.
	 *
	 * @param errorMessage description of the PMS partner or extraction-policy failure
	 */
	public PartnerException(String errorMessage) {
		super(CredentialServiceErrorCodes.PARTNER_EXCEPTION.getErrorCode(), errorMessage);
	}

	/**
	 * Creates an exception with the standard partner error code, a custom message, and root cause.
	 *
	 * @param message human-readable failure detail
	 * @param cause   underlying parsing or service error
	 */
	public PartnerException(String message, Throwable cause) {
		super(CredentialServiceErrorCodes.PARTNER_EXCEPTION.getErrorCode() + "", message, cause);
	}

	/**
	 * Creates an exception with the standard partner error code and default message, wrapping
	 * the root cause.
	 *
	 * @param t underlying failure
	 */
	public PartnerException(Throwable t) {
		super(CredentialServiceErrorCodes.PARTNER_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.PARTNER_EXCEPTION.getErrorMessage(), t);
	}

	/**
	 * Creates an exception with an explicit MOSIP error code and message (for propagated
	 * downstream errors).
	 *
	 * @param errorCode    MOSIP error code string
	 * @param errorMessage associated error message
	 */
	public PartnerException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}
}
