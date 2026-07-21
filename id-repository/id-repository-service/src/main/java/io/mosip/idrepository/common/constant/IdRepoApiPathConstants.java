package io.mosip.idrepository.common.constant;

/**
 * Legacy public URL path prefixes for the consolidated ID-Repository HTTP deployable.
 * <p>
 * Controllers keep relative mappings; {@link io.mosip.idrepository.config.IdRepoApiPathConfig}
 * applies these prefixes so external clients (Reg Processor, IDA, partners, api-test) see
 * unchanged URLs after the multi-service merge.
 * </p>
 *
 * @see io.mosip.idrepository.config.IdRepoApiPathConfig
 */
public final class IdRepoApiPathConstants {

	/** Prefix for identity, draft, and VID event-callback controllers. */
	public static final String IDENTITY_PATH_PREFIX = "/idrepository/v1/identity";

	/** Prefix for credential store (issuance) controllers. */
	public static final String CREDENTIAL_SERVICE_PATH_PREFIX = "/v1/credentialservice";

	/** Prefix for credential request generator controllers. */
	public static final String CREDENTIAL_REQUEST_PATH_PREFIX = "/v1/credentialrequest";

	/** Prefix for VID lifecycle controllers ({@code /vid}, draft VID, etc.). */
	public static final String VID_PATH_PREFIX = "/idrepository/v1";

	private IdRepoApiPathConstants() {
	}
}
