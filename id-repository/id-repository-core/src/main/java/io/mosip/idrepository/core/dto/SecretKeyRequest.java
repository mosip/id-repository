package io.mosip.idrepository.core.dto;

import lombok.Data;

/**
 * Client-credentials payload for MOSIP authentication token requests.
 *
 * <p>
 * Exposes Keycloak (or auth-manager) client id, secret, and application id used
 * when credential-request obtains a service token. Nested as {@code request}
 * inside {@link TokenRequestDTO}. Fields are public for historical Jackson
 * binding compatibility.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Preferred grant type for service-to-service calls from credential-request /
 * credential-store utilities. Prefer over {@link PasswordRequest} when a
 * confidential client is configured.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link TokenRequestDTO}</li>
 *   <li>{@code CredReqRestUtil} and related token helpers</li>
 * </ul>
 *
 * @see TokenRequestDTO
 * @see PasswordRequest
 * @see io.mosip.idrepository.credential.request.util.CredReqRestUtil
 */
@Data
public class SecretKeyRequest {

	/** OAuth client identifier registered for the calling service. */
	public String clientId;

	/** OAuth client secret paired with {@link #clientId}. */
	public String secretKey;

	/** MOSIP application id (for example, credential-request module name). */
	public String appId;
}
