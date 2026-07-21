package io.mosip.idrepository.credential.store.exception;

import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

/**
 * Checked exception raised when digital signing of the credential payload via Keymanager fails.
 * <p>
 * Credential-store signs the issuance envelope (legacy JSON credential or W3C Verifiable
 * Credential proof block) using the kernel signature service.
 * {@link io.mosip.idrepository.credential.store.util.DigitalSignatureUtil} throws this exception
 * when Keymanager returns an error response, the signature field is absent, or JWT/COSE signing
 * cannot be completed. HTTP transport failures to Keymanager are surfaced separately as
 * {@link ApiNotAccessibleException}.
 * </p>
 * <p>
 * Maps to error code {@link CredentialServiceErrorCodes#SIGNATURE_EXCEPTION}
 * ({@code IDR-CRS-012}). Signing failure prevents Datashare upload and WebSub
 * {@code CREDENTIAL_ISSUED} publication.
 * </p>
 *
 * @see CredentialServiceErrorCodes#SIGNATURE_EXCEPTION
 * @see io.mosip.idrepository.credential.store.util.DigitalSignatureUtil
 */
public class SignatureException extends BaseCheckedException {

	private static final long serialVersionUID = -1938475620193847562L;

	/**
	 * Creates an exception with no preset error code or message (kernel base defaults).
	 * Prefer the {@link #SignatureException(String)} overload for consistent {@code IDR-CRS-*} codes.
	 */
	public SignatureException() {
		super();
	}

	/**
	 * Creates an exception with the standard signature error code and a custom detail message.
	 *
	 * @param errorMessage description of the Keymanager signing failure
	 */
	public SignatureException(String errorMessage) {
		super(CredentialServiceErrorCodes.SIGNATURE_EXCEPTION.getErrorCode(), errorMessage);
	}

	/**
	 * Creates an exception with the standard signature error code, a custom message, and root cause.
	 *
	 * @param message human-readable failure detail
	 * @param cause   underlying crypto or parsing error
	 */
	public SignatureException(String message, Throwable cause) {
		super(CredentialServiceErrorCodes.SIGNATURE_EXCEPTION.getErrorCode(), message, cause);
	}

	/**
	 * Creates an exception with the standard signature error code and default message, wrapping
	 * the root cause.
	 *
	 * @param t underlying failure
	 */
	public SignatureException(Throwable t) {
		super(CredentialServiceErrorCodes.SIGNATURE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.SIGNATURE_EXCEPTION.getErrorMessage(), t);
	}
}
