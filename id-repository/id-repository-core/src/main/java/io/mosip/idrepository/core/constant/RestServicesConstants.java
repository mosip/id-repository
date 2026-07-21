package io.mosip.idrepository.core.constant;

/**
 * Spring Cloud Config property keys for outbound MOSIP REST service endpoints.
 *
 * <p>
 * Each enum constant's {@link #getServiceName()} is a config-server key whose value
 * is a {@code protocol}://{@code host}:{@code port}{@code path} URL. Resolved at
 * startup by {@link io.mosip.idrepository.core.builder.RestRequestBuilder}.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Centralises the config property names used to build {@code RestRequestDTO} instances
 * for kernel audit, cryptomanager, syncdata, IDA, PMS, VID, UIN generator, biometric
 * extractor, and credential-request endpoints. Domain code references the enum instead
 * of hard-coding property key strings.
 * </p>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * <strong>Critical:</strong> Several keys are part of the operational contract between
 * ID Repository and IDA / partner services:
 * </p>
 * <ul>
 *   <li>{@link #ID_AUTH_SERVICE} — outbound calls toward IDA</li>
 *   <li>{@link #CREDENTIAL_REQUEST_SERVICE} / {@link #CREDENTIAL_REQUEST_SERVICE_V2} —
 *       credential queue APIs (may be in-process after consolidation)</li>
 *   <li>{@link #IDREPO_IDENTITY_SERVICE} — identity retrieve-by-UIN used by credential flows</li>
 *   <li>{@link #PARTNER_SERVICE} — PMS partner lookups for credential issuance</li>
 * </ul>
 * <p>
 * Do not rename {@link #getServiceName()} property keys without updating Spring Cloud
 * Config and any IDA/deployment overlays that still reference them. After consolidation,
 * some services are invoked via in-process clients
 * ({@code InProcessIdentityClient}, {@code InProcessCredentialRequestClient},
 * {@code InProcessVidClient}) while the config keys remain for URL resolution and
 * fallback HTTP paths.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * RestRequestDTO request = restRequestBuilder.buildRequest(
 *     RestServicesConstants.CRYPTO_MANAGER_ENCRYPT, requestBody, ResponseType.class);
 * </pre>
 * <p>
 * Wired via {@link io.mosip.idrepository.config.IdRepoLibraryConfig#restRequestBuilder()}.
 * </p>
 *
 * @author Manoj SP
 * @see io.mosip.idrepository.core.builder.RestRequestBuilder
 * @see io.mosip.idrepository.config.IdRepoLibraryConfig#restRequestBuilder()
 * @see IdRepoConstants
 */
public enum RestServicesConstants {

	/** Kernel audit manager service. */
	AUDIT_MANAGER_SERVICE("mosip.idrepo.audit"),

	/** Identity retrieve-by-UIN (SDK via {@link io.mosip.idrepository.pipeline.InProcessIdentityClient}). */
	IDREPO_IDENTITY_SERVICE("mosip.idrepo.retrieve-by-uin"),

	/** VID generator service. */
	VID_GENERATOR_SERVICE("mosip.idrepo.vid-generator"),

	/** Draft VID generator service. */
	VID_DRAFT_GENERATOR_SERVICE("mosip.idrepo.draft-vid"),

	/** VID update service. */
	VID_UPDATE_SERVICE("mosip.idrepo.update-vid"),

	/** UIN generator service. */
	UIN_GENERATOR_SERVICE("mosip.idrepo.uin-generator"),

	/** Cryptomanager encrypt endpoint. */
	CRYPTO_MANAGER_ENCRYPT("mosip.idrepo.encryptor"),

	/** Cryptomanager decrypt endpoint. */
	CRYPTO_MANAGER_DECRYPT("mosip.idrepo.decryptor"),

	/** Kernel syncdata (identity schema) service. */
	SYNCDATA_SERVICE("mosip.idrepo.syncdata-service"),

	/** IDA (Identity Authentication) service. */
	ID_AUTH_SERVICE("mosip.idrepo.ida"),

	/** Partner management (PMS) service. */
	PARTNER_SERVICE("mosip.idrepo.pmp.partner"),

	/** Credential-request-generator v1 queue API (SDK via {@link io.mosip.idrepository.pipeline.InProcessCredentialRequestClient}). */
	CREDENTIAL_REQUEST_SERVICE("mosip.idrepo.credential.request"),

	/** Credential request cancellation endpoint. */
	CREDENTIAL_CANCEL_SERVICE("mosip.idrepo.credential.cancel-request"),

	/** VID service — retrieve VIDs by UIN (SDK via {@link io.mosip.idrepository.pipeline.InProcessVidClient}). */
	RETRIEVE_VIDS_BY_UIN("mosip.idrepo.vid-service"),

	/** VID service — retrieve UIN by VID. */
	RETRIEVE_UIN_BY_VID("mosip.idrepo.retrieve-uin-by-vid"),

	/** Biometric template extractor service. */
	BIO_EXTRACTOR_SERVICE("mosip.idrepo.bio-extractor-service"),

	/** Credential-request-generator v2 queue API. */
	CREDENTIAL_REQUEST_SERVICE_V2("mosip.idrepo.credential-request-v2");

	/** Spring Cloud Config property key for the service base URL. */
	private final String serviceName;

	/**
	 * Creates a REST service constant bound to a config-server property key.
	 *
	 * @param serviceName config-server property key whose value is the service base URL
	 */
	private RestServicesConstants(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Returns the config-server property key used to resolve the service URL.
	 *
	 * @return the config-server property key used to resolve the service URL
	 */
	public String getServiceName() {
		return serviceName;
	}
}
