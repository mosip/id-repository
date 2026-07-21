package io.mosip.idrepository.credential.store.exception;

import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

/**
 * Checked exception raised when Partner Management Service (PMS) credential policy data cannot
 * be fetched, parsed, or validated during issuance.
 * <p>
 * Each partner credential type is governed by a PMS policy that defines allowed KYC attributes,
 * encryption requirements, and provider selection. {@link io.mosip.idrepository.credential.store.util.PolicyUtil}
 * retrieves and caches these policies; this exception is thrown when PMS returns an error
 * payload, the policy JSON is malformed, or schema validation fails—distinct from transport
 * errors ({@link ApiNotAccessibleException}) and partner-record errors ({@link PartnerException}).
 * </p>
 * <p>
 * Maps to error code {@link CredentialServiceErrorCodes#POLICY_EXCEPTION}
 * ({@code IDR-CRS-008}). Policy resolution is a prerequisite step before identity retrieve and
 * credential provider dispatch in {@link io.mosip.idrepository.credential.store.service.impl.CredentialStoreServiceImpl}.
 * </p>
 *
 * @see CredentialServiceErrorCodes#POLICY_EXCEPTION
 * @see io.mosip.idrepository.credential.store.util.PolicyUtil
 */
public class PolicyException extends BaseCheckedException {

	private static final long serialVersionUID = 5102938475619283740L;

	/**
	 * Creates an exception with no preset error code or message (kernel base defaults).
	 * Prefer the {@link #PolicyException(String)} overload for consistent {@code IDR-CRS-*} codes.
	 */
	public PolicyException() {
		super();
	}

	/**
	 * Creates an exception with the standard policy error code and a custom detail message.
	 *
	 * @param errorMessage description of the PMS policy fetch or validation failure
	 */
	public PolicyException(String errorMessage) {
		super(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorCode(), errorMessage);
	}

	/**
	 * Creates an exception with the standard policy error code, a custom message, and root cause.
	 *
	 * @param message human-readable failure detail
	 * @param cause   underlying parsing or service error
	 */
	public PolicyException(String message, Throwable cause) {
		super(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorCode() + "", message, cause);
	}

	/**
	 * Creates an exception with the standard policy error code and default message, wrapping
	 * the root cause.
	 *
	 * @param t underlying failure
	 */
	public PolicyException(Throwable t) {
		super(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorMessage(), t);
	}

	/**
	 * Creates an exception with an explicit MOSIP error code and message (for propagated
	 * downstream errors).
	 *
	 * @param errorCode    MOSIP error code string
	 * @param errorMessage associated error message
	 */
	public PolicyException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}
}
