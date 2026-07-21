package io.mosip.idrepository.credential.request.dto;

import lombok.Data;

/**
 * WebSub envelope for {@code CREDENTIAL_STATUS_UPDATE} notifications to credential-request service.
 * <p>
 * Received at {@code /v1/credentialrequest/callback/notifyStatus} and applied by
 * {@link io.mosip.idrepository.credential.request.service.CredentialRequestService#updateCredentialStatus}.
 * </p>
 *
 * @see Event
 */
@Data
public class CredentialStatusEvent {

	/** WebSub publisher id (credential store or partner). */
	private String publisher;

	/** Subscribed topic name ({@code CREDENTIAL_STATUS_UPDATE}). */
	private String topic;

	/** ISO timestamp when the event was published. */
	private String publishedOn;

	/** Status change payload with request id and new queue status. */
	private Event event;
}
