package io.mosip.idrepository.core.dto;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * REST response wrapper for credential store {@code /issue} and related endpoints.
 *
 * <p>
 * Combines {@link BaseRestResponseDTO} envelope fields with
 * {@link CredentialServiceResponse} on success or {@link ErrorDTO} entries on
 * failure.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Returned by {@code /v1/credentialservice/} issue (and related) operations,
 * whether invoked over HTTP or in-process from credential-request.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Credential-store controllers</li>
 *   <li>{@code InProcessCredentialClient} / credential-request callers</li>
 *   <li>api-test suites validating issuance contracts</li>
 * </ul>
 *
 * @see CredentialServiceRequestDto
 * @see CredentialServiceResponse
 * @see BaseRestResponseDTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CredentialServiceResponseDto extends BaseRestResponseDTO {
	private static final long serialVersionUID = -4819273640158293741L;

	/** Issuance result; {@code null} when {@link #errors} is populated. */
	private CredentialServiceResponse response;

	/** Populated when issuance fails validation or downstream processing. */
	private List<ErrorDTO> errors;
}
