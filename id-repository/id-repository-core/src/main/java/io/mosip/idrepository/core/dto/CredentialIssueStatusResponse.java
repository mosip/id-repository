package io.mosip.idrepository.core.dto;

import lombok.Data;

/**
 * Status snapshot for a credential issuance request after processing.
 *
 * <p>
 * Returned when partners poll issuance status or receive notifications that
 * include a datashare download link. Complements the lighter
 * {@link CredentialIssueResponse} acknowledgement and the WebSub
 * {@link CredentialStatusUpdateEvent}.
 * </p>
 *
 * <h2>API / WebSub context</h2>
 * <p>
 * Used by credential-request status APIs under {@code /v1/credentialrequest/}.
 * When {@link #statusCode} is ISSUED, {@link #url} typically holds the datashare
 * location also published on partner {@code CREDENTIAL_ISSUED} topics.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Credential-request status endpoints</li>
 *   <li>Partner clients and IDA-related Datashare consumers</li>
 *   <li>List APIs that summarize rows as {@link CredentialRequestIdsDto}</li>
 * </ul>
 *
 * @see CredentialStatusUpdateEvent
 * @see CredentialRequestIdsDto
 * @see CredentialServiceEventResponse
 */
@Data
public class CredentialIssueStatusResponse {

	/** Correlation identifier of the issuance request. */
	private String requestId;

	/** Individual identifier (UIN or VID) associated with the request. */
	private String id;

	/** Processing status code (for example, ISSUED, FAILED, PRINTING). */
	private String statusCode;

	/** Datashare URL where the issued credential can be retrieved, when available. */
	private String url;
}
