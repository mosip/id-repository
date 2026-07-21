package io.mosip.idrepository.core.dto;

import lombok.Data;

/**
 * Password-grant payload for MOSIP authentication token requests.
 *
 * <p>
 * Alternative to {@link SecretKeyRequest} when obtaining a service token via
 * username/password grant. Nested as {@code request} inside
 * {@link TokenRequestDTO}. Fields are public for historical Jackson binding
 * compatibility with credential-request utilities.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Used by credential-request REST utilities when client-credentials are not
 * configured and password grant is required against auth-manager / Keycloak.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link TokenRequestDTO}</li>
 *   <li>{@code CredReqRestUtil} and related token helpers</li>
 * </ul>
 *
 * @see TokenRequestDTO
 * @see SecretKeyRequest
 * @see io.mosip.idrepository.credential.request.util.CredReqRestUtil
 */
@Data
public class PasswordRequest {

	/** MOSIP application id requesting the token. */
	public String appId;

	/** User password for the password grant flow. */
	public String password;

	/** Username for the password grant flow. */
	public String userName;
}
