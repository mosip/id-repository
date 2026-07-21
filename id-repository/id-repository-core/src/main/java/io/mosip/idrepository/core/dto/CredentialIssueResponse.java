package io.mosip.idrepository.core.dto;

import lombok.Data;

/**
 * Minimal success payload after a credential issuance request is accepted.
 *
 * <p>
 * Returned synchronously when a request is queued, before asynchronous
 * credential processing completes. Contains only the correlation
 * {@link #requestId} and the individual {@link #id}.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Nested in {@link CredentialIssueResponseDto} for credential-request create
 * responses. Full status (including datashare URL) is available later via
 * {@link CredentialIssueStatusResponse} or WebSub
 * {@link CredentialStatusUpdateEvent}.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Credential-request REST create/issue handlers</li>
 *   <li>Partner clients correlating subsequent status polls</li>
 * </ul>
 *
 * @see CredentialIssueResponseDto
 * @see CredentialIssueStatusResponse
 */
@Data
public class CredentialIssueResponse {

	/** Correlation identifier assigned to the queued issuance request. */
	private String requestId;

	/** Individual identifier (UIN or VID) for whom the credential was requested. */
	private String id;
}
