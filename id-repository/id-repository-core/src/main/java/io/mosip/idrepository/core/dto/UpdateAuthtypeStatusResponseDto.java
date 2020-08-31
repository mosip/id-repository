package io.mosip.idrepository.core.dto;

import java.util.List;

import io.mosip.kernel.core.exception.ServiceError;
import lombok.Data;

@Data
public class UpdateAuthtypeStatusResponseDto {
	
	private String id;

	/** Variable To hold version */
	private String version;

	/** The error List */
	private List<ServiceError> errors;

	/** The resTime value */
	private String responseTime;

}
