package io.mosip.idrepository.core.constant;

/**
 * Lifecycle states for rows in the {@code credential_request_status} table.
 *
 * <p>
 * {@link io.mosip.idrepository.manager.CredentialStatusManager} polls
 * {@link #NEW} and {@link #FAILED} rows, transitions them to {@link #REQUESTED}
 * after a successful credreq hand-off, and marks stale rows {@link #INVALID} or
 * {@link #DELETED} when identity status changes.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Models the credential-request status state machine used by the identity credential
 * status job and related repositories. Status strings are persisted as enum names in
 * {@code credential_request_status.status}.
 * </p>
 *
 * <h2>State transitions (typical)</h2>
 * <ul>
 *   <li>{@link #NEW} → {@link #REQUESTED} after successful hand-off to credreq</li>
 *   <li>{@link #NEW} / {@link #REQUESTED} → {@link #FAILED} on pipeline error</li>
 *   <li>Any active state → {@link #DELETED} when identity is deactivated / credential no longer needed</li>
 *   <li>Any active state → {@link #INVALID} when the request cannot be processed (e.g. missing policy)</li>
 * </ul>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * These lifecycle values are internal to ID Repository persistence and jobs. IDA does
 * not read {@code credential_request_status}. Do not change enum names without a DB
 * migration for existing status rows.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * statusEntity.setStatus(CredentialRequestStatusLifecycle.NEW.name());
 * // CredentialStatusManager polls NEW / FAILED and sets REQUESTED after hand-off
 * </pre>
 *
 * @author Manoj SP
 * @author Nagarjuna
 * @see CredentialTriggerAction
 * @see io.mosip.idrepository.manager.CredentialStatusManager
 */
public enum CredentialRequestStatusLifecycle {

	/** Queued for credential issuance; picked up by the status manager job. */
	NEW,

	/** Handed off to credential-request-generator successfully. */
	REQUESTED,

	/** Credreq or credential pipeline returned an error for this request. */
	FAILED,

	/** Identity deactivated or credential no longer required; row ignored by batch. */
	DELETED,

	/** Request could not be processed (e.g. missing partner policy). */
	INVALID;
}
