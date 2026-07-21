package io.mosip.idrepository.core.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * Base MOSIP REST response envelope fields shared by credential-service responses.
 *
 * <p>
 * Provides {@link #id}, {@link #version}, and {@link #responsetime} common to
 * credential-request and credential-store HTTP responses. Subclasses add
 * domain-specific {@code response} and {@code errors} payloads. Implements
 * {@link Serializable} for caching or messaging.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Parent of {@link CredentialIssueResponseDto} and
 * {@link CredentialServiceResponseDto} under {@code /v1/credentialrequest/}
 * and {@code /v1/credentialservice/}.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Credential-request and credential-store controllers</li>
 *   <li>In-process pipeline clients that decode the same JSON shapes</li>
 * </ul>
 *
 * @see CredentialIssueResponseDto
 * @see CredentialServiceResponseDto
 * @see ErrorDTO
 */
@Data
public class BaseRestResponseDTO implements Serializable {

	private static final long serialVersionUID = 4246582347420843195L;

	/** Unique response identifier (UUID). */
	private String id;

	/** API version echoed from the request. */
	private String version;

	/** ISO-8601 timestamp when the response was generated. */
	private String responsetime;
}
