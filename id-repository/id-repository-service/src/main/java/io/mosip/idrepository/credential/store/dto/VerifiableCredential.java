package io.mosip.idrepository.credential.store.dto;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * W3C Verifiable Credential JSON model produced by {@link io.mosip.idrepository.credential.store.provider.impl.VerCredProvider}.
 * <p>
 * Maps issuer, credential subject, issuance date, and proof fields for verifiable-credential partners.
 * </p>
 *
 * @see Proof
 * @see Verifiable
 */
public class VerifiableCredential extends Verifiable {

	/** JSON-LD key for credential subject block. */
	public static final String JSONLD_KEY_CREDENTIAL_SUBJECT = "credentialSubject";

	/** JSON-LD key for issuer URI or object. */
	public static final String JSONLD_KEY_ISSUSER = "issuer";

	/** JSON-LD key for issuance date. */
	public static final String JSONLD_KEY_ISSUANCE_DATE = "issuanceDate";

	/** JSON-LD key for expiration date. */
	public static final String JSONLD_KEY_EXPIRATION_DATE = "expirationDate";

	/** JSON-LD key for credential status list entry. */
	public static final String JSONLD_KEY_CREDENTIALS_STATUS = "credentialStatus";

	/** Primary VC type string. */
	public static final String JSONLD_TYPE_CREDENTIAL = "VerifiableCredential";

	/** Creates an empty verifiable credential with default W3C context. */
	public VerifiableCredential() {
		super();
	}

	/**
	 * Wraps existing VC JSON-LD content.
	 *
	 * @param jsonObject source map
	 */
	public VerifiableCredential(Map<String, Object> jsonObject) {
		super(jsonObject);
	}

	/**
	 * Returns the W3C VerifiableCredential type constant.
	 *
	 * @return {@link #JSONLD_TYPE_CREDENTIAL}
	 */
	@Override
	public String getType() {
		return JSONLD_TYPE_CREDENTIAL;
	}

	/**
	 * Sets the credential issuer URI.
	 *
	 * @param issuer issuing authority URI
	 */
	public void setIssuer(URI issuer) {
		jsonObject.put(JSONLD_KEY_ISSUSER, issuer.toString());
	}

	/**
	 * Returns the credential issuer URI.
	 *
	 * @return issuer URI or {@code null}
	 */
	public URI getIssuer() {
		String iss = (String) jsonObject.get(JSONLD_KEY_ISSUSER);
		return iss == null ? null : URI.create(iss);
	}

	/**
	 * Sets the ISO-8601 issuance date string.
	 *
	 * @param date issuance timestamp
	 */
	public void setIssuanceDate(String date) {
		jsonObject.put(JSONLD_KEY_ISSUANCE_DATE, date);
	}

	/**
	 * Returns the ISO-8601 issuance date string.
	 *
	 * @return issuance date or {@code null}
	 */
	public String getIssunaceDate() {
		return (String) jsonObject.get(JSONLD_KEY_ISSUANCE_DATE);
	}

	/**
	 * Sets the credential subject (identity attributes map or nested object).
	 *
	 * @param subject credential subject payload
	 */
	public void setCredentialSubject(Object subject) {
		jsonObject.put(JSONLD_KEY_CREDENTIAL_SUBJECT, subject);
	}

	/**
	 * Returns the credential subject object.
	 *
	 * @return subject map or structured object
	 */
	public Object getCredentialSubject() {
		return jsonObject.get(JSONLD_KEY_CREDENTIAL_SUBJECT);
	}

	/**
	 * Returns the full JSON-LD document map for serialization.
	 *
	 * @return ordered VC JSON object
	 */
	public LinkedHashMap<String, Object> getJsonObject() {
		return jsonObject;
	}
}
