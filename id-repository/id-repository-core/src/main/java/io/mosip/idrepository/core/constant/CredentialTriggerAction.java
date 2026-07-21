package io.mosip.idrepository.core.constant;

/**
 * Describes why a credential re-issuance was triggered for a UIN.
 *
 * <p>
 * Stored in {@code credential_request_status.trigger_action} and compared by
 * {@link io.mosip.idrepository.manager.CredentialStatusManager} when polling
 * {@link CredentialRequestStatusLifecycle#NEW} rows.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Distinguishes first-time credential issuance after identity creation from
 * re-issuance after an identity update, so the status manager and downstream
 * credential pipeline can apply the correct partner/policy behaviour.
 * </p>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * Trigger actions are internal to ID Repository. IDA receives credential events via
 * WebSub ({@link IDAEventType}) and does not consume these enum values. Renaming
 * constants requires a data migration for existing {@code trigger_action} columns.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * // On identity create
 * status.setTriggerAction(CredentialTriggerAction.CREATE.name());
 * // On identity update
 * status.setTriggerAction(CredentialTriggerAction.UPDATE.name());
 * </pre>
 *
 * @see CredentialRequestStatusLifecycle
 * @see io.mosip.idrepository.identity.service.impl.IdRepoServiceImpl
 * @see io.mosip.idrepository.manager.CredentialStatusManager
 */
public enum CredentialTriggerAction {

	/** First-time credential issuance after identity creation. */
	CREATE,

	/** Credential re-issuance after an identity update. */
	UPDATE;
}
