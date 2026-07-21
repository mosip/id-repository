package io.mosip.idrepository.core.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Response payload body nested inside {@link IdResponseDTO}.
 *
 * <p>
 * Extends {@link BaseRequestResponseDTO} and carries the same identity fragment
 * fields as {@link RequestDTO} (status, identity JSON, documents, verified
 * attributes). Retained as a distinct type for backward-compatible response
 * wrapping; a TODO in source notes possible future consolidation.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Business body of {@code /idrepository/v1/identity} create, update, and
 * retrieve responses inside {@link IdResponseDTO}.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link IdResponseDTO}</li>
 *   <li>Identity service implementations and api-test clients</li>
 *   <li>Credential pipeline decoding identity retrieve results</li>
 * </ul>
 *
 * @author Manoj SP
 * @see IdResponseDTO
 * @see BaseRequestResponseDTO
 * @see RequestDTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResponseDTO extends BaseRequestResponseDTO {
	//TODO remove this dto
}
