package io.mosip.idrepository.common.constant;

/**
 * Standard MOSIP log-context field names used as the third argument in
 * {@link io.mosip.kernel.core.logger.spi.Logger} calls across ID-Repository credential modules.
 * <p>
 * Consolidated from duplicate enums in credential-store and credential-request-generator after
 * the service merge. Each constant's {@link #name()} (via {@code .toString()}) becomes the
 * structured log field key; the fourth argument carries the field value.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * LOGGER.info(user, LoggerFileConstant.SESSIONID.toString(), sessionId, "fetching policy");
 * LOGGER.error(user, LoggerFileConstant.REQUEST_ID.toString(), requestId, "encryption failed");
 * LOGGER.debug(user, LoggerFileConstant.ID.toString(), uinHash, "credential issued");
 * </pre>
 *
 * <h2>Primary consumers</h2>
 * <ul>
 *   <li>{@code credential.store.util.*} — REST, encryption, datashare, WebSub helpers</li>
 *   <li>{@code credential.store.provider.impl.*} — format-specific credential providers</li>
 *   <li>{@code credential.request.service.impl.CredentialRequestServiceImpl} — queue processing</li>
 * </ul>
 *
 * @see io.mosip.idrepository.core.logger.IdRepoLogger
 * @see io.mosip.idrepository.core.security.IdRepoSecurityManager#getUser()
 */
public enum LoggerFileConstant {

	/**
	 * HTTP session identifier for correlating log lines within a single inbound request.
	 * Value: session id string from the servlet session or security context.
	 */
	SESSIONID,

	/**
	 * Business entity identifier associated with the operation (UIN hash, VID, RID, credential id).
	 * Value: opaque id string; avoid logging raw UIN/VID in production.
	 */
	ID,

	/**
	 * MOSIP request / transaction id for end-to-end tracing across services.
	 * Value: typically the {@code requestId} from credential issuance or credreq APIs.
	 */
	REQUEST_ID
}
