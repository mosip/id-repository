package io.mosip.idrepository.core.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Kernel {@link ResponseWrapper} variant used in legacy credential-request HTTP flows.
 *
 * <p>
 * Note: the generic type parameter is {@link CredentialIssueRequestDto} (request
 * shape) rather than a dedicated response DTO; callers should treat the wrapped
 * object per the specific API contract. Prefer {@link CredentialIssueResponseDto}
 * for new integrations that need a clear response payload.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Retained for backward-compatible credential-request responses that historically
 * echoed request-shaped data inside a kernel response envelope.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Legacy credential-request controller paths</li>
 *   <li>Clients still deserializing the older wrapper shape</li>
 * </ul>
 *
 * @author Loganathan Sekaran
 * @see CredentialIssueRequestDto
 * @see CredentialIssueResponseDto
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CredentialIssueResponseWrapperDto extends ResponseWrapper<CredentialIssueRequestDto> {
}
