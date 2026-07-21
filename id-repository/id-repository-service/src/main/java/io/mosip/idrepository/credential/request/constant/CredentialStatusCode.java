package io.mosip.idrepository.credential.request.constant;

/**
 * Lifecycle status codes for rows in {@code credential_transaction} ({@code mosip_credential}).
 * <p>
 * Batch tasklets select {@link #NEW} and {@link #FAILED} rows; reprocess job targets
 * {@link #FAILED} and {@link #RETRY} under retry limits.
 * </p>
 */
public enum CredentialStatusCode {

	/** Queued and awaiting batch processing. */
	NEW,

	/** Cancelled by API before issuance completed. */
	CANCELLED,

	/** Credential successfully issued and artifacts stored. */
	ISSUED,

	/** Issuance failed; eligible for retry/reprocess. */
	FAILED,

	/** Marked for explicit reprocess after operator retrigger. */
	RETRY
}
