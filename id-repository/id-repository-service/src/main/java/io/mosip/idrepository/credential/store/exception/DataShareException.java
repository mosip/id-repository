package io.mosip.idrepository.credential.store.exception;

import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

/**
 * Checked exception raised when the Datashare service does not return a usable credential
 * payload during issuance.
 * <p>
 * After the credential JSON is built and signed, credential-store uploads the protected
 * artifact to Datashare so partners receive a shareable URL rather than raw bytes.
 * {@link io.mosip.idrepository.credential.store.util.DataShareUtil} throws this exception when
 * the Datashare response is {@code null}, empty, or otherwise invalid, or when a non-HTTP
 * processing error occurs after a successful transport handshake.
 * </p>
 * <p>
 * Maps to error code {@link CredentialServiceErrorCodes#DATASHARE_EXCEPTION}
 * ({@code IDR-CRS-011}). A failure at this step aborts issuance before the
 * {@code CREDENTIAL_ISSUED} WebSub notification is published.
 * </p>
 *
 * @see CredentialServiceErrorCodes#DATASHARE_EXCEPTION
 * @see io.mosip.idrepository.credential.store.util.DataShareUtil
 */
public class DataShareException extends BaseCheckedException {

	private static final long serialVersionUID = 6572940183746501928L;

	/**
	 * Creates an exception with the default Datashare error code and message.
	 */
	public DataShareException() {
		super(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage());
	}

	/**
	 * Creates an exception with the default error code and a custom detail message.
	 *
	 * @param message description of the Datashare failure
	 */
	public DataShareException(String message) {
		super(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(),
				message);
	}

	/**
	 * Creates an exception with the default error code and message, wrapping the root cause.
	 *
	 * @param e underlying parsing, IO, or service error
	 */
	public DataShareException(Throwable e) {
		super(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage(), e);
	}

	/**
	 * Creates an exception with the default error code, a custom message, and root cause.
	 *
	 * @param errorMessage detail describing the Datashare response problem
	 * @param t            underlying cause
	 */
	public DataShareException(String errorMessage, Throwable t) {
		super(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(), errorMessage, t);
	}
}
