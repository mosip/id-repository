package io.mosip.idrepository.core.dto;

import java.util.Map;

import lombok.Data;

/**
 * WebSub event payload published when a credential has been issued to a partner.
 *
 * <p>
 * Carries the datashare download location and any partner-specific extensions
 * echoed from the issuance request. Published on partner-scoped
 * {@code {partnerId}/CREDENTIAL_ISSUED} topics.
 * </p>
 *
 * <h2>WebSub context</h2>
 * <p>
 * Topic naming is an external contract (see parent AGENTS.md). Often wrapped in
 * {@link EventModel} for transport. IDA and partners retrieve the credential
 * via Datashare using {@link #dataShareUrl} — they do not read id-repo salt tables.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code CredentialServiceManager} / credential-store WebSub publishers</li>
 *   <li><strong>IDA</strong> and partner WebSub subscribers</li>
 *   <li>Datashare download clients</li>
 * </ul>
 *
 * <h2>IDA stability</h2>
 * <p>
 * Credential / Datashare payload shape must remain stable. Do not rename
 * {@code dataShareUrl} or {@code additionalData} without coordinating IDA and
 * partner releases.
 * </p>
 *
 * @see CredentialServiceResponse
 * @see EventModel
 * @see io.mosip.idrepository.manager.CredentialServiceManager
 */
@Data
public class CredentialServiceEventResponse {

	/** URL where the partner can download the issued credential via datashare. */
	private String dataShareUrl;

	/** Partner-specific extensions echoed from the issuance request. */
	private Map<String, Object> additionalData;
}
