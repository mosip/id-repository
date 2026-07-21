package io.mosip.idrepository.core.constant;

/**
 * Marker interface for WebSub / IDA event type identifiers published by ID Repository.
 *
 * <p>
 * Implementations carry the event name used as the WebSub topic suffix (e.g.
 * {@code {partnerId}/CREDENTIAL_ISSUED}). The consolidated deployable uses
 * {@link IDAEventType} as the sole implementation.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * Decouples WebSub helpers and credential managers from a concrete enum so event
 * catalogues can evolve while publish/subscribe APIs accept a common type.
 * </p>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * IDA subscribes to topics whose suffixes match {@link IDAEventType} constant names.
 * Any new {@link EventType} implementation used on partner-scoped topics must keep
 * those names stable for IDA consumers.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * EventType type = IDAEventType.CREDENTIAL_ISSUED;
 * webSubHelper.publishEvent(partnerId + "/" + type, eventModel);
 * </pre>
 * <p>
 * See {@link io.mosip.idrepository.core.helper.IdRepoWebSubHelper} and
 * {@link io.mosip.idrepository.manager.CredentialServiceManager}.
 * </p>
 *
 * @see IDAEventType
 * @see io.mosip.idrepository.core.helper.IdRepoWebSubHelper
 * @see io.mosip.idrepository.manager.CredentialServiceManager
 */
public interface EventType {

}
