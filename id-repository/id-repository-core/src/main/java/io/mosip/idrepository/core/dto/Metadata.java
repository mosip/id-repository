package io.mosip.idrepository.core.dto;

/**
 * Optional metadata container for MOSIP request envelopes.
 *
 * <p>
 * Placeholder type referenced by {@link TokenRequestDTO#metadata}. Intended for
 * device, location, or other contextual attributes when outbound auth/token
 * calls need to propagate extra envelope data. Currently empty; extend fields
 * carefully to avoid breaking Jackson deserialization of existing clients.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Set on {@link TokenRequestDTO} when calling auth-manager / Keycloak token
 * endpoints from credential-request utilities. May be {@code null} when no
 * metadata is required.
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
 * @see PasswordRequest
 */
public class Metadata {

}
