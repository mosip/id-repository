package io.mosip.idrepository.credential.store.constant;

/**
 * Enumerated MOSIP error codes ({@code IDR-CRS-*}) for the credential-store issuance module.
 * <p>
 * Each constant pairs a stable error code with a default English message. Matching checked and
 * unchecked exception types in {@code io.mosip.idrepository.credential.store.exception} bind to
 * these codes when surfacing failures to REST callers, audit logs, and the credential-request
 * queue status updater. Codes are assigned sequentially as new failure categories are introduced
 * in the issuance pipeline (policy fetch → identity retrieve → format → sign → datashare → WebSub).
 * </p>
 *
 * @see io.mosip.idrepository.credential.store.exception.ApiNotAccessibleException
 * @see io.mosip.idrepository.credential.store.exception.DataShareException
 * @see io.mosip.idrepository.credential.store.exception.IdRepoException
 * @see io.mosip.idrepository.credential.store.exception.VerCredException
 */
public enum CredentialServiceErrorCodes {

	/** Downstream REST API returned HTTP 4xx/5xx or was unreachable during issuance. */
	API_NOT_ACCESSIBLE_EXCEPTION("IDR-CRS-001", "API not accessible"),

	/** Identity retrieve returned null or an error payload when building the credential. */
	IPREPO_EXCEPTION("IDR-CRS-003", "ID repo response is null"),

	/** Reflection could not instantiate a {@code JsonValue} wrapper for an identity attribute. */
	INSTANTIATION_EXCEPTION("IDR-CRS-004", "Error while creating object of JsonValue class"),

	/** Required identity attribute field was not found via reflection during JSON assembly. */
	NO_SUCH_FIELD_EXCEPTION("IDR-CRS-005", "Could not find the field"),

	/** MVEL or template formatter failed while shaping a shareable attribute value. */
	CREDENTIAL_FORMATTER_EXCEPTION("IDR-CRS-006", "exception while formatting"),

	/** Unclassified issuance failure not mapped to a more specific code. */
	UNKNOWN_EXCEPTION("IDR-CRS-007", "unknown exception"),

	/** PMS credential policy could not be fetched or parsed. */
	POLICY_EXCEPTION("IDR-CRS-008", "Failed to get policy details"),

	/** IO error reading credential templates, streams, or temporary files. */
	IO_EXCEPTION("IDR-CRS-009", "IO exception"),

	/** Datashare service returned null or an invalid upload response. */
	DATASHARE_EXCEPTION("IDR-CRS-011", "Datashare response is null"),

	/** Keymanager JWT or COSE signing call failed or returned no signature. */
	SIGNATURE_EXCEPTION("IDR-CRS-012", "Failed to generate digital signature"),

	/** Symmetric or asymmetric encryption of credential attributes via Keymanager failed. */
	DATA_ENCRYPTION_FAILURE_EXCEPTION("IDR-CRS-013", "Data Encryption failed"),

	/** Publishing the {@code CREDENTIAL_ISSUED} WebSub event failed after successful issuance. */
	WEBSUB_FAIL_EXCEPTION("IDR-CRS-014", "Websub event failed"),

	/** Partner credential policy JSON failed schema validation against PMS policy model. */
	POLICY_SCHEMA_VALIDATION_EXCEPTION("IDR-CRS-015", "Policy Schema validation failed"),

	/** VC JSON-LD context file download or parse failed during Verifiable Credential build. */
	VC_CONTEXT_FILE_NOT_FOUND("IDR-CRS-016", "Error downloading VC Context file or JSON parsing error."),

	/** Resident PIN required for VC attribute encryption was not available in the request context. */
	PIN_NOT_PROVIDER("IDR-CRS-017", "Pin not available to encrypt the data."),

	/** PMS partner extraction policy could not be fetched or parsed for biometric formatting. */
	PARTNER_EXCEPTION("IDR-CRS-008", "Failed to get partner extraction policy details");

	/** Stable MOSIP error code returned to API clients and audit. */
	private final String errorCode;

	/** Default English error message paired with {@link #errorCode}. */
	private final String errorMessage;

	/**
	 * Binds a MOSIP error code to its default message.
	 *
	 * @param errorCode    stable {@code IDR-CRS-*} identifier
	 * @param errorMessage default human-readable description
	 */
	private CredentialServiceErrorCodes(final String errorCode, final String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	/**
	 * Returns the MOSIP error code (for example {@code IDR-CRS-001}).
	 *
	 * @return error code string
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Returns the default English error message for this code.
	 *
	 * @return error message
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
}
