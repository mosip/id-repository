package io.mosip.idrepository.credential.store.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * Unchecked exception for failures while building or sealing a W3C Verifiable Credential (VC)
 * during the credential-store issuance path.
 * <p>
 * When partner policy selects the VC provider ({@link io.mosip.idrepository.credential.store.provider.impl.VerCredProvider}),
 * credential-store assembles a {@code VC-V1} document: downloads JSON-LD context files, applies
 * selective disclosure, encrypts protected attributes, and attaches a JWT proof. This exception
 * is raised for VC-specific business failures—missing resident PIN for attribute encryption,
 * unreadable VC context from config server, or JSON parsing errors during context merge—as
 * implemented in {@link io.mosip.idrepository.credential.store.util.Utilities} and
 * {@code VerCredProvider}.
 * </p>
 * <p>
 * Unlike most credential-store exceptions, callers supply an explicit MOSIP error code (typically
 * from {@link io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes})
 * rather than a fixed default on the no-arg constructor.
 * </p>
 *
 * @see io.mosip.idrepository.credential.store.provider.impl.VerCredProvider
 * @see io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes#VC_CONTEXT_FILE_NOT_FOUND
 * @see io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes#PIN_NOT_PROVIDER
 */
public class VerCredException extends BaseUncheckedException {

	private static final long serialVersionUID = 8462910573840192735L;

	/**
	 * Creates a VC issuance exception with an explicit MOSIP error code and message.
	 *
	 * @param errorCode    MOSIP error code (for example {@code IDR-CRS-016})
	 * @param errorMessage human-readable failure description
	 */
	public VerCredException(String errorCode, String errorMessage) {
		super(errorCode, errorMessage);
	}

	/**
	 * Creates a VC issuance exception with an explicit error code and message, wrapping the
	 * root cause.
	 *
	 * @param errorCode    MOSIP error code
	 * @param errorMessage human-readable failure description
	 * @param e            underlying IO, parsing, or crypto failure
	 */
	public VerCredException(String errorCode, String errorMessage, Throwable e) {
		super(errorCode, errorMessage, e);
	}
}
