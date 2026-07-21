package io.mosip.idrepository.credential.store.constant;

/**
 * Credential type codes used in issuance requests and config-server type listings.
 */
public enum CredentialType {

	/** IdAuth partner credential with ZK/PIN-protected attributes. */
	AUTH,

	/** QR code credential payload. */
	QRCODE,

	/** Generic MOSIP credential envelope. */
	MOSIP,

	/** W3C Verifiable Credential (JSON-LD + JWS proof). */
	VERCRED
}
