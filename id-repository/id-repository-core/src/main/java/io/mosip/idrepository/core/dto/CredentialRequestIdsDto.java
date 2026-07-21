package io.mosip.idrepository.core.dto;

import lombok.Data;

/**
 * Summary row for a credential request in paginated list APIs.
 *
 * <p>
 * Represents one credential-request queue entry as returned by administrative
 * or partner status queries. Fields are string-typed for JSON transport and
 * mirror columns from the credential-request persistence model (request id,
 * partner, type, status, and audit timestamps).
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Returned inside {@link PageDto} from credential-request list endpoints under
 * {@code /v1/credentialrequest/} (for example, get request ids by status).
 * Lombok {@code @Data} generates accessors used for Jackson serialization.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Credential-request REST controllers and
 *       {@code CredentialRequestServiceImpl#getRequestIds}</li>
 *   <li>Partner / admin clients polling issuance queue status</li>
 *   <li><strong>IDA (ID Authentication)</strong> — this type is part of the
 *       published {@code id-repository-core} API surface</li>
 * </ul>
 *
 * <h2>IDA stability</h2>
 * <p>
 * IDA references this DTO from the core JAR. Do <strong>not</strong> rename the
 * class or change JSON field names ({@code requestId}, {@code credentialType},
 * {@code partner}, {@code statusCode}, {@code statusComment},
 * {@code createDateTime}, {@code updateDateTime}) without a coordinated IDA
 * release. Error codes such as {@code IDR-CRG-009} that IDA matches are
 * orthogonal but part of the same contract surface.
 * </p>
 *
 * @see PageDto
 * @see CredentialIssueStatusResponse
 * @see io.mosip.idrepository.credential.request.service.impl.CredentialRequestServiceImpl#getRequestIds
 */
@Data
public class CredentialRequestIdsDto {

	/** Unique identifier of the credential issuance request. */
	private String requestId;

	/** Partner-defined credential type. */
	private String credentialType;

	/** Partner identifier that requested the credential. */
	private String partner;

	/** Current processing status code. */
	private String statusCode;

	/** Human-readable or system status comment. */
	private String statusComment;

	/** Timestamp when the request was first queued. */
	private String createDateTime;

	/** Timestamp of the last status update. */
	private String updateDateTime;
}
