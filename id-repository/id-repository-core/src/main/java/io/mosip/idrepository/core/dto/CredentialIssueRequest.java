package io.mosip.idrepository.core.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * In-process credential issuance request passed from credential-request to credential store.
 *
 * <p>
 * Same field set as {@link CredentialIssueRequestDto} but without
 * {@code requestId}; used for direct service calls and batch tasklets after
 * validation. The intentional typo {@code recepiant} is part of the external
 * JSON contract and must not be renamed.
 * </p>
 *
 * <h2>API / pipeline context</h2>
 * <p>
 * Used on the in-process hop
 * {@code CredentialRequestService → CredentialStoreService} via
 * {@code InProcessCredentialClient}, and historically for HTTP between the
 * former microservices.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code CredentialRequestService} / batch tasklets</li>
 *   <li>{@code InProcessCredentialClient}</li>
 *   <li>{@code CredentialStoreService} issue path</li>
 * </ul>
 *
 * @see CredentialIssueRequestDto
 * @see CredentialServiceRequestDto
 * @see io.mosip.idrepository.credential.request.service.CredentialRequestService
 * @see io.mosip.idrepository.pipeline.InProcessCredentialClient
 */
@Data
public class CredentialIssueRequest {

	/** Individual identifier (UIN or VID) for whom the credential is issued. */
	private String id;

	/** Partner-defined credential type (for example, verifiable credential profile). */
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
}
