package io.mosip.idrepository.core.dto;

import lombok.Data;

/**
 * Standard MOSIP request envelope for authentication and authorization APIs.
 *
 * <p>
 * Wraps a typed {@link #request} body with id, version, timestamp, and optional
 * {@link Metadata} used across ID Repository outbound token calls. Fields are
 * public for historical Jackson binding with credential-request utilities.
 * </p>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>{@code TokenRequestDTO<SecretKeyRequest>} — client-credentials grant</li>
 *   <li>{@code TokenRequestDTO<PasswordRequest>} — password grant</li>
 * </ul>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code CredReqRestUtil} and credential-store token helpers</li>
 *   <li>Any outbound call that needs a MOSIP auth-manager token envelope</li>
 * </ul>
 *
 * @param <T> type of the nested request payload (for example, {@link SecretKeyRequest})
 * @see SecretKeyRequest
 * @see PasswordRequest
 * @see Metadata
 * @see io.mosip.idrepository.credential.request.util.CredReqRestUtil
 */
@Data
public class TokenRequestDTO<T> {

	/** Unique request identifier for tracing and idempotency. */
	public String id;

	/** Optional request metadata (device, location, etc.). */
	public Metadata metadata;

	/** Typed request body (credentials, password grant, or other auth payload). */
	public T request;

	/** ISO-8601 timestamp when the request was created. */
	public String requesttime;

	/** MOSIP API version string. */
	public String version;
}
