package io.mosip.idrepository.core.constant;

/**
 * Identifier types accepted by ID Repository retrieve and credential APIs.
 *
 * <p>
 * The string value ({@link #getIdType()}) is the external type label used in
 * request DTOs and audit logs. Enum constant names match the API-facing labels
 * ({@code VID}, {@code UIN}, {@code ID}, {@code HANDLE}).
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Standardises how callers declare which kind of individual identifier they are
 * supplying (UIN, VID, generic ID/RID, or handle) so identity retrieve, credential
 * issuance, and validation layers can route hashing, salt lookup, and DB queries
 * correctly.
 * </p>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * IDA typically authenticates with UIN or VID; the string labels {@code UIN} and
 * {@code VID} appear in credential and Datashare payloads. Do not change
 * {@link #getIdType()} return values without coordinating downstream consumers.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * IdType type = IdType.UIN;
 * String label = type.getIdType(); // "UIN"
 * // Used in retrieveIdentity(id, IdType.VID, ...) and credential request DTOs
 * </pre>
 *
 * @author Prem Kumar
 * @see io.mosip.idrepository.identity.service.impl.IdRepoServiceImpl
 */
public enum IdType {

	/** Virtual ID — temporary alias for a UIN. */
	VID("VID"),

	/** Unique Identification Number — primary national ID. */
	UIN("UIN"),

	/** Generic individual identifier (RID or other mapped ID). */
	ID("ID"),

	/** User-chosen handle linked to a UIN. */
	HANDLE("HANDLE");

	/** External type label sent in API requests. */
	private final String idType;

	/**
	 * Returns the API-facing identifier type string.
	 *
	 * @return the API-facing identifier type string (e.g. {@code UIN}, {@code VID})
	 */
	public String getIdType() {
		return idType;
	}

	/**
	 * Creates an identifier-type constant with its external label.
	 *
	 * @param idType external type label sent in API requests
	 */
	private IdType(String idType) {
		this.idType = idType;
	}
}
