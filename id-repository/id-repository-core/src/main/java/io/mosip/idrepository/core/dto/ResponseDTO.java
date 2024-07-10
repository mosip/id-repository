package io.mosip.idrepository.core.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The Class ResponseDTO - response DTO containing additional fields for response
 * field in {@code IdResponseDTO}.
 
 *
 * @author Manoj SP
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResponseDTO<T> extends RequestDTO {
	private T verifiedAttributes;
}
