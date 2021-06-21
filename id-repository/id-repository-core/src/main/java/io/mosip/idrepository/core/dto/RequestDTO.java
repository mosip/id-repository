package io.mosip.idrepository.core.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The Class RequestDTO - response DTO containing additional fields for request
 * field in {@code IdRequestDTO}.
 *
 * @author Manoj SP
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RequestDTO extends BaseRequestResponseDTO {

	private String registrationId;
	
	@Deprecated(since = "1.1.4.3")
	private String biometricReferenceId;
}
