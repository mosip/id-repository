package io.mosip.idrepository.core.constant;

/**
 * WebSub event types published to IDA (Identity Authentication) and partner subscribers.
 *
 * <p>
 * Topic format: {@code {partnerId}/{EVENT_NAME}}. Event names are the enum constant
 * names (e.g. {@code CREDENTIAL_ISSUED}, {@code IDENTITY_UPDATED}). Global topics may
 * use the bare event name without a partner prefix (e.g. {@code IDENTITY_CREATED}).
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Defines the canonical catalogue of IDA-facing and partner-facing WebSub events that
 * ID Repository publishes after credential issuance, identity lifecycle changes, and
 * auth-type status updates. {@link io.mosip.idrepository.core.helper.IdRepoWebSubHelper}
 * builds kernel {@code EventModel} payloads and publishes to the hub using these names.
 * </p>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * <strong>Critical:</strong> This enum is part of the published {@code id-repository-core}
 * API surface consumed by ID Authentication. IDA subscribes to partner-scoped topics such
 * as {@code {partnerId}/CREDENTIAL_ISSUED}, {@code REMOVE_ID}, {@code DEACTIVATE_ID},
 * {@code ACTIVATE_ID}, and {@code AUTH_TYPE_STATUS_UPDATE}.
 * </p>
 * <ul>
 *   <li>Do <strong>not</strong> rename or remove constants without an IDA-coordinated release</li>
 *   <li>Do not change topic suffix spelling; IDA matches on the enum {@link #name()}</li>
 *   <li>Payload shapes for credential and auth-type events are separate contracts — keep
 *       DTO field names stable as well</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * // Partner-scoped publish
 * String topic = partnerId + "/" + IDAEventType.CREDENTIAL_ISSUED.name();
 * webSubHelper.createEventModel(IDAEventType.CREDENTIAL_ISSUED, ...);
 *
 * // Auth-type status
 * webSubHelper.publishAuthTypeStatusUpdateEvent(..., IDAEventType.AUTH_TYPE_STATUS_UPDATE);
 * </pre>
 *
 * @author Manoj SP
 * @see EventType
 * @see io.mosip.idrepository.core.helper.IdRepoWebSubHelper
 * @see io.mosip.idrepository.credential.store.service.impl.CredentialStoreServiceImpl
 * @see io.mosip.idrepository.manager.CredentialServiceManager
 */
public enum IDAEventType implements EventType {

	/** Credential successfully issued and shared with the partner. */
	CREDENTIAL_ISSUED,

	/** UIN/VID permanently removed from IDA (blocked status). */
	REMOVE_ID,

	/** UIN/VID temporarily deactivated in IDA. */
	DEACTIVATE_ID,

	/** Previously deactivated UIN/VID reactivated in IDA. */
	ACTIVATE_ID,

	/** Partner auth-type lock/unlock status changed. */
	AUTH_TYPE_STATUS_UPDATE,

	/** New identity record created in ID Repository. */
	IDENTITY_CREATED,

	/** Existing identity demographics/biometrics updated. */
	IDENTITY_UPDATED;
}
