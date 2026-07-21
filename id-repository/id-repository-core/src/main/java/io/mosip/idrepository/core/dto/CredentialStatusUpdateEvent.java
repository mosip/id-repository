package io.mosip.idrepository.core.dto;

import lombok.Data;

/**
 * WebSub payload notifying subscribers of credential-request queue status changes.
 *
 * <p>
 * Published on the {@code CREDENTIAL_STATUS_UPDATE} topic when batch processing,
 * identity-driven status updates, or credential-request lifecycle transitions
 * change the status of a queued issuance request.
 * </p>
 *
 * <h2>WebSub context</h2>
 * <p>
 * Topic name is part of the external contract (see parent AGENTS.md). Partners
 * and IDA-related flows may subscribe to learn when a request moves to states
 * such as ISSUED or FAILED. Often wrapped in {@link EventModel} for transport.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code CredentialStatusManager} / credential-request publishers</li>
 *   <li>WebSub subscribers on {@code CREDENTIAL_STATUS_UPDATE}</li>
 *   <li>Partner status UIs and polling clients correlating via {@link #requestId}</li>
 * </ul>
 *
 * <h2>IDA stability</h2>
 * <p>
 * Credential / Datashare and WebSub payload shapes are IDA-sensitive. Do not
 * rename JSON fields ({@code id}, {@code requestId}, {@code status},
 * {@code timestamp}) without coordinating downstream consumers. IDA does not
 * read id-repo salt tables; it observes outcomes via WebSub / Datashare / REST.
 * </p>
 *
 * @author Nagarjuna
 * @see CredentialIssueStatusResponse
 * @see EventModel
 * @see io.mosip.idrepository.manager.CredentialStatusManager
 */
@Data
public class CredentialStatusUpdateEvent {

	/** Individual identifier (UIN or VID) associated with the credential request. */
	private String id;

	/** Correlation identifier of the credential issuance request. */
	private String requestId;

	/** New status code after the update (for example, ISSUED or FAILED). */
	private String status;

	/** ISO-8601 timestamp when the status change occurred. */
	private String timestamp;
}
