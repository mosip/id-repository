package io.mosip.idrepository.core.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * Successful credential issuance result from the credential store service.
 *
 * <p>
 * Returned after identity retrieval, partner policy evaluation, signing, and
 * Datashare upload complete. Includes credential id, issuance timestamp,
 * signature, and the datashare URL later echoed in
 * {@link CredentialServiceEventResponse}.
 * </p>
 *
 * <h2>API / WebSub context</h2>
 * <p>
 * Nested in {@link CredentialServiceResponseDto} for {@code /v1/credentialservice/}
 * issue responses. {@link #dataShareUrl} is the same location partners and IDA
 * use to download the credential.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code CredentialStoreService} — builds the result</li>
 *   <li>Credential-request status updaters</li>
 *   <li>WebSub publishers mapping to {@link CredentialServiceEventResponse}</li>
 * </ul>
 *
 * @see CredentialServiceResponseDto
 * @see CredentialServiceEventResponse
 */
@Data
public class CredentialServiceResponse {

	/** High-level outcome status (for example, ISSUED). */
	private String status;

	/** Unique identifier assigned to the stored credential record. */
	private String credentialId;

	/** Timestamp when the credential was signed and persisted. */
	private LocalDateTime issuanceDate;

	/** Digital signature over the credential payload. */
	private String signature;

	/** Datashare URL where the partner retrieves the credential. */
	private String dataShareUrl;
}
