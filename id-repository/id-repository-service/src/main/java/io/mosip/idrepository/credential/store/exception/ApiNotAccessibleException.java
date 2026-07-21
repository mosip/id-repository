package io.mosip.idrepository.credential.store.exception;

import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

/**
 * Checked exception raised when an outbound REST call required for credential issuance cannot
 * be completed successfully.
 * <p>
 * During credential-store processing the service calls multiple MOSIP kernel and platform
 * modules—Partner Management (policy/partner APIs), Keymanager (signing and encryption),
 * Datashare, Identity retrieve, and VID service. This exception is thrown when those calls
 * return HTTP 4xx/5xx responses or are otherwise unreachable, as detected in utilities such as
 * {@link io.mosip.idrepository.credential.store.util.PolicyUtil},
 * {@link io.mosip.idrepository.credential.store.util.EncryptionUtil},
 * {@link io.mosip.idrepository.credential.store.util.DigitalSignatureUtil}, and
 * {@link io.mosip.idrepository.credential.store.util.DataShareUtil}.
 * </p>
 * <p>
 * Maps to error code {@link CredentialServiceErrorCodes#API_NOT_ACCESSIBLE_EXCEPTION}
 * ({@code IDR-CRS-001}). Callers in {@link io.mosip.idrepository.credential.store.service.impl.CredentialStoreServiceImpl}
 * typically convert this into a failed issuance response and audit the downstream dependency.
 * </p>
 *
 * @see CredentialServiceErrorCodes#API_NOT_ACCESSIBLE_EXCEPTION
 */
public class ApiNotAccessibleException extends BaseCheckedException {

	private static final long serialVersionUID = 4829374650192837465L;

	/**
	 * Creates an exception with the default API-not-accessible error code and message.
	 */
	public ApiNotAccessibleException() {
		super(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage());
	}

	/**
	 * Creates an exception with the default error code and a detail message (often the HTTP
	 * response body from the failed downstream service).
	 *
	 * @param message human-readable or downstream error payload
	 */
	public ApiNotAccessibleException(String message) {
		super(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
				message);
	}

	/**
	 * Creates an exception with the default error code and message, wrapping the root cause.
	 *
	 * @param e underlying HTTP client/server exception or transport failure
	 */
	public ApiNotAccessibleException(Throwable e) {
		super(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage(), e);
	}

	/**
	 * Creates an exception with the default error code, a custom message, and root cause.
	 *
	 * @param errorMessage detail describing which API call failed
	 * @param t            underlying cause
	 */
	public ApiNotAccessibleException(String errorMessage, Throwable t) {
		super(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(), errorMessage, t);
	}
}
