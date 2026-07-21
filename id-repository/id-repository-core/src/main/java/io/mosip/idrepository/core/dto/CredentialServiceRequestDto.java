package io.mosip.idrepository.core.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Request body for the credential store {@code /issue} API.
 *
 * <p>
 * Carries the same issuance parameters as {@link CredentialIssueRequestDto},
 * used when credential-request invokes credential-service over HTTP or via
 * {@code InProcessCredentialClient}. Field {@code recepiant} retains its
 * historical spelling for JSON compatibility.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Inbound to {@code /v1/credentialservice/issue}. After identity retrieve,
 * policy evaluation, signing, and Datashare upload, the store returns
 * {@link CredentialServiceResponseDto} and may publish
 * {@link CredentialServiceEventResponse} on WebSub.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code CredentialStoreService}</li>
 *   <li>Credential-request batch / in-process clients</li>
 *   <li>Direct partner callers of credential-service (if exposed)</li>
 * </ul>
 *
 * @see CredentialServiceResponseDto
 * @see CredentialIssueRequestDto
 * @see io.mosip.idrepository.credential.store.service.CredentialStoreService
 */
@Data
public class CredentialServiceRequestDto {

	/** Individual identifier (UIN or VID) for whom the credential is issued. */
	private String id;

	/** Partner-defined credential type. */
	private String credentialType;

	/** Correlation identifier linking back to the credential-request queue entry. */
	private String requestId;

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
