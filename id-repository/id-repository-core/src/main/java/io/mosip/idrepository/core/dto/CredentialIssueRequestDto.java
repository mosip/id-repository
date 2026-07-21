package io.mosip.idrepository.core.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Persisted and API-facing credential issuance request including correlation id.
 *
 * <p>
 * Extends {@link CredentialIssueRequest} semantics with {@link #requestId} for
 * queue status tracking and WebSub correlation. Serialized into the credential
 * queue table and returned by credential-request REST APIs. The field name
 * {@code recepiant} is a historical spelling retained for JSON compatibility.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Nested in {@link CredentialIssueRequestWrapperDto} for
 * {@code /v1/credentialrequest/} create/issue endpoints. Status polling and
 * {@link CredentialStatusUpdateEvent} use {@link #requestId} as the key.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code CredentialRequestServiceImpl} — persist and issue synchronously</li>
 *   <li>{@code CredentialIssuanceProcessor} — in-process issuance</li>
 *   <li>Partner clients submitting issuance requests</li>
 * </ul>
 *
 * @see CredentialIssueRequest
 * @see CredentialIssueRequestWrapperDto
 * @see CredentialRequestIdsDto
 * @see io.mosip.idrepository.credential.request.service.impl.CredentialRequestServiceImpl
 */
@Data
public class CredentialIssueRequestDto {

	/** Individual identifier (UIN or VID) for whom the credential is issued. */
	private String id;

	/** Partner-defined credential type. */
	private String credentialType;

	/** Partner (issuer) identifier registered in PMS. */
	private String issuer;

	/** Recipient identifier or endpoint for credential delivery. */
	private String recepiant;

	/** User or system initiating the issuance request. */
	private String user;

	/** {@code true} when the credential payload must be encrypted for the partner. */
	private boolean encrypt;

	/** Partner public key or key reference used when {@link #encrypt} is {@code true}. */
	private String encryptionKey;

	/** Identity attribute names to include in the issued credential. */
	private List<String> sharableAttributes;

	/** Partner-specific extensions not covered by standard fields. */
	private Map<String, Object> additionalData;

	/** Unique request identifier for queue status and WebSub correlation. */
	private String requestId;
}
