package io.mosip.idrepository.core.dto;

import io.mosip.kernel.core.http.RequestWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MOSIP standard request wrapper for the credential-request generator API.
 *
 * <p>
 * Extends kernel {@link RequestWrapper} with {@link CredentialIssueRequestDto}
 * as the typed {@code request} body. Envelope fields ({@code id},
 * {@code version}, {@code requesttime}) are validated by
 * {@code RequestValidator} before the business payload is processed.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Inbound body for {@code /v1/credentialrequest/} create/issue endpoints.
 * Lombok {@code @Data} and {@code @EqualsAndHashCode(callSuper = true)} include
 * envelope fields in equality checks.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>Credential-request controllers and validators</li>
 *   <li>{@code CredentialRequestService} implementations</li>
 * </ul>
 *
 * @author Loganathan Sekaran
 * @see CredentialIssueRequestDto
 * @see io.mosip.idrepository.credential.request.validator.RequestValidator
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CredentialIssueRequestWrapperDto extends RequestWrapper<CredentialIssueRequestDto> {
}
