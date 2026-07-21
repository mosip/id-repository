package io.mosip.idrepository.core.dto;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * REST response for credential-request generator endpoints.
 *
 * <p>
 * Combines {@link BaseRestResponseDTO} envelope fields with a
 * {@link CredentialIssueResponse} business payload or a list of
 * {@link ErrorDTO} entries on failure.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Returned by {@code /v1/credentialrequest/} create and related operations.
 * Lombok {@code @EqualsAndHashCode(callSuper = true)} includes envelope fields
 * in equality.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Credential-request controllers</li>
 *   <li>Partner HTTP clients and api-test suites</li>
 * </ul>
 *
 * @see CredentialIssueResponse
 * @see BaseRestResponseDTO
 * @see ErrorDTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CredentialIssueResponseDto extends BaseRestResponseDTO {
	private static final long serialVersionUID = 7193846502174938625L;

	/** Successful issuance acknowledgement; {@code null} when {@link #errors} is populated. */
	private CredentialIssueResponse response;

	/** Populated when the request fails validation or processing. */
	private List<ErrorDTO> errors;
}
