package io.mosip.idrepository.core.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request payload body nested inside {@link IdRequestDTO}.
 *
 * <p>
 * Extends {@link BaseRequestResponseDTO} with identifiers supplied during
 * identity add, update, and draft operations ({@link #registrationId},
 * {@link #uin}). The deprecated {@link #biometricReferenceId} is retained for
 * older clients but unused in current ID schema flows.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Business body of {@code /idrepository/v1/identity} create/update requests.
 * Identity JSON lives in inherited {@link BaseRequestResponseDTO#getIdentity()}.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link IdRequestDTO}</li>
 *   <li>{@code IdRequestValidator} and {@code IdRepoService}</li>
 * </ul>
 *
 * @author Manoj SP
 * @see IdRequestDTO
 * @see BaseRequestResponseDTO
 * @see ResponseDTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RequestDTO extends BaseRequestResponseDTO {

	/** Registration identifier (RID) from the registration module. */
	private String registrationId;

	/** Unique Identification Number for the individual. */
	private String uin;

	/**
	 * Legacy biometric reference identifier.
	 *
	 * @deprecated since 1.1.4; no longer used in current ID schema flows.
	 */
	@Deprecated(since = "1.1.4")
	private String biometricReferenceId;
}
