package io.mosip.idrepository.credential.request.dto;

import lombok.Data;

/**
 * Credential status change payload nested inside {@link CredentialStatusEvent}.
 * <p>
 * Published on WebSub topic {@code CREDENTIAL_STATUS_UPDATE} when issuance completes or fails.
 * </p>
 */
@Data
public class Event {

	/** Unique event id from the publisher. */
	private String id;

	/** Credential request id ({@code credential_transaction.id}). */
	private String requestId;

	/** ISO-8601 timestamp of the status transition. */
	private String timestamp;

	/** New queue status ({@link io.mosip.idrepository.credential.request.constant.CredentialStatusCode}). */
	private String status;

	/** Data-share URL when status is ISSUED (optional in failure events). */
	private String url;
}
