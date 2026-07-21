package io.mosip.idrepository.credential.store.constant;

/**
 * Outbound REST service identifiers for credential-store integrations.
 * <p>
 * Each constant name matches a property key in Spring Cloud Config / {@code RestServicesConstants}.
 * </p>
 */
public enum ApiName {

	/** Identity retrieve by id (UIN/VID) for credential packaging. */
	IDREPOGETIDBYID,

	/** Alternate identity retrieve endpoint key. */
	IDREPORETRIEVEIDBYID,

	/** Data Share multipart upload endpoint. */
	CREATEDATASHARE,

	/** Kernel JWT signature service. */
	KEYMANAGER_JWTSIGN,

	/** Verifiable credential JWS signature service. */
	KEYMANAGER_VERCRED_SIGN,

	/** PIN-based attribute encryption. */
	KEYMANAGER_ENCRYPT_PIN,

	/** Zero-knowledge attribute encryption. */
	KEYMANAGER_ENCRYPT_ZK,

	/** PMS partner credential-type policy fetch. */
	PARTNER_POLICY,

	/** PMS partner biometric extractor policy fetch. */
	PARTNER_EXTRACTION_POLICY,

	/** Partner-certificate payload encryption. */
	CRYPTOMANAGER_ENCRYPT,

	/** Keymanager certificate download. */
	KEYMANAGER_GET_CERTIFICATE,

	/** Partner certificate download from PMS. */
	GET_PARTNER_CERTIFICATE,

	/** Upload partner domain certificate to keymanager. */
	KEYMANAGER_UPLOAD_OTHER_DOMAIN_CERTIFICATE,

	/** VID generation API. */
	GENERATE_VID,

	/** VID retrieve-by-UIN API. */
	RETRIEVE_VID
}
