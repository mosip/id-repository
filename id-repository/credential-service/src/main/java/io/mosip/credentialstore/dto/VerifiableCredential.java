package io.mosip.credentialstore.dto;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public class VerifiableCredential extends Verifiable {

	public static final String JSONLD_KEY_CREDENTIAL_SUBJECT = "credentialSubject";
	public static final String JSONLD_KEY_ISSUSER = "issuer";
	public static final String JSONLD_KEY_ISSUANCE_DATE = "issuanceDate";
	public static final String JSONLD_KEY_EXPIRATION_DATE = "expirationDate";
	public static final String JSONLD_KEY_CREDENTIALS_STATUS = "credentialStatus";

	// type
	public static final String JSONLD_TYPE_CREDENTIAL = "VerifiableCredential";

	public VerifiableCredential() {
		super();
	}

	public VerifiableCredential(Map<String, Object> jsonObject) {
		super(jsonObject);
	}

	@Override
	public String getType() {
		return JSONLD_TYPE_CREDENTIAL;
	}

	/**
	 * Set issuer
	 * 
	 * @param issuer issuer
	 */
	public void setIssuer(URI issuer) {
		jsonObject.put(JSONLD_KEY_ISSUSER, issuer.toString());
	}

	/**
	 * Get issuer
	 * 
	 * @return issuer of credential
	 */
	public URI getIssuer() {
		String iss = (String) jsonObject.get(JSONLD_KEY_ISSUSER);
		return iss == null ? null : URI.create(iss);
	}

	/**
	 * Set issued date of credential
	 * 
	 * @param date date
	 */
	public void setIssuanceDate(String date) {
		jsonObject.put(JSONLD_KEY_ISSUANCE_DATE, date);
	}

	/**
	 * Get issued date of credential
	 * 
	 * @return issued date
	 */
	public String getIssunaceDate() {
		String date = ((String) jsonObject.get(JSONLD_KEY_ISSUANCE_DATE));
		if (date != null) {
			return date;
		}

		return null;
	}



	/**
	 * Set credential subject
	 * 
	 * @param subject credential subject
	 */
	public void setCredentialSubject(Object subject) {
		jsonObject.put(JSONLD_KEY_CREDENTIAL_SUBJECT, subject);
	}

	/**
	 * Get credential subject
	 * 
	 * @return credential subject
	 */
	public Object getCredentialSubject() {
		return jsonObject.get(JSONLD_KEY_CREDENTIAL_SUBJECT);
	}



	/**
	 * Get json object
	 * 
	 * @return json object
	 */
	public LinkedHashMap<String, Object> getJsonObject() {
		return jsonObject;
	}
}
