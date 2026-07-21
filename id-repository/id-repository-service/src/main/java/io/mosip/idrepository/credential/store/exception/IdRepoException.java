package io.mosip.idrepository.credential.store.exception;

import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

/**
 * Checked exception raised when identity data required for credential issuance cannot be
 * retrieved from ID Repository.
 * <p>
 * Credential-store must fetch the resident's identity (and sometimes VID) before applying
 * partner policy filters and building the credential payload.
 * {@link io.mosip.idrepository.credential.store.util.IdrepositaryUtil} and
 * {@link io.mosip.idrepository.credential.store.util.VIDUtil} throw this exception when the
 * identity retrieve API returns a {@code null} body, carries a MOSIP error list, or fails during
 * response parsing—distinct from pure transport failures which are surfaced as
 * {@link ApiNotAccessibleException}.
 * </p>
 * <p>
 * Maps to error code {@link CredentialServiceErrorCodes#IPREPO_EXCEPTION}
 * ({@code IDR-CRS-003}). Without a valid identity snapshot, issuance cannot proceed and the
 * credential-request queue row is typically marked failed.
 * </p>
 *
 * @see CredentialServiceErrorCodes#IPREPO_EXCEPTION
 * @see io.mosip.idrepository.credential.store.util.IdrepositaryUtil
 */
public class IdRepoException extends BaseCheckedException {

	private static final long serialVersionUID = 2918475630192847561L;

	/**
	 * Creates an exception with the default ID Repository error code and message.
	 */
	public IdRepoException() {
		super(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorMessage());
	}

	/**
	 * Creates an exception with the default error code and a custom detail message.
	 *
	 * @param message often includes identity-service error text or UIN/VID context
	 */
	public IdRepoException(String message) {
		super(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorCode(),
				message);
	}

	/**
	 * Creates an exception with the default error code and message, wrapping the root cause.
	 *
	 * @param e underlying parsing or service error
	 */
	public IdRepoException(Throwable e) {
		super(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorCode(),
				CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorMessage(), e);
	}

	/**
	 * Creates an exception with the default error code, a custom message, and root cause.
	 *
	 * @param errorMessage detail describing the identity retrieve failure
	 * @param t            underlying cause
	 */
	public IdRepoException(String errorMessage, Throwable t) {
		super(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorCode(), errorMessage, t);
	}
}
