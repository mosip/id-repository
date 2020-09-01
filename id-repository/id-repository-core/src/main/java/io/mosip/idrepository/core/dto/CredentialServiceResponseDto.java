package io.mosip.idrepository.core.dto;

import java.util.List;


import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
public class CredentialServiceResponseDto extends BaseRestResponseDTO{
	private static final long serialVersionUID = 1L;

	private CredentialServiceResponse  response;


	private List<ErrorDTO> errors;
	
}
