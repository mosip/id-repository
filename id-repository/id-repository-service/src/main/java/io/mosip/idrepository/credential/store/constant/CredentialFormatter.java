package io.mosip.idrepository.credential.store.constant;

/**
 * Spring bean names for credential-type-specific {@link io.mosip.idrepository.credential.store.provider.CredentialProvider} implementations.
 */
public enum CredentialFormatter {

	/** IdAuth ZK credential formatter bean. */
	IdAuthProvider,

	/** Default MOSIP JSON credential formatter. */
	CredentialDefaultProvider,

	/** QR code credential formatter bean. */
	QrCodeProvider,

	/** Verifiable credential formatter bean. */
	VerCredProvider
}
