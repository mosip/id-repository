package io.mosip.idrepository.credential.store.dto;

import java.time.LocalDateTime;

import org.json.simple.JSONObject;

/**
 * Intermediate credential payload produced by {@link io.mosip.idrepository.credential.store.provider.CredentialProvider}.
 * <p>
 * Contains the MOSIP credential JSON-LD envelope before data-share upload and partner delivery.
 * </p>
 */
public class DataProviderResponse {

	/** Unique credential identifier assigned at issuance. */
	private String credentialId;

	/** UTC issuance timestamp written to the credential envelope. */
	private LocalDateTime issuanceDate;

	/** Full credential JSON including {@code credentialSubject} and {@code protectedAttributes}. */
	private JSONObject JSON;

	/**
	 * Returns the generated credential identifier.
	 *
	 * @return credential id string
	 */
	public String getCredentialId() {
		return credentialId;
	}

	/**
	 * Sets the credential identifier.
	 *
	 * @param credentialId unique credential id
	 */
	public void setCredentialId(String credentialId) {
		this.credentialId = credentialId;
	}

	/**
	 * Returns the issuance timestamp.
	 *
	 * @return UTC issuance time
	 */
	public LocalDateTime getIssuanceDate() {
		return issuanceDate;
	}

	/**
	 * Sets the issuance timestamp.
	 *
	 * @param issuanceDate UTC issuance time
	 */
	public void setIssuanceDate(LocalDateTime issuanceDate) {
		this.issuanceDate = issuanceDate;
	}

	/**
	 * Returns the credential JSON document.
	 *
	 * @return credential JSONObject envelope
	 */
	public JSONObject getJSON() {
		return JSON;
	}

	/**
	 * Sets the credential JSON document.
	 *
	 * @param jSON credential JSONObject envelope
	 */
	public void setJSON(JSONObject jSON) {
		JSON = jSON;
	}
}
