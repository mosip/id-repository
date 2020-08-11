package io.mosip.credential.request.generator.dto;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
public class CredentialServiceResponseDto extends BaseRestResponseDTO{
	private static final long serialVersionUID = 1L;

	private String  status;

	private List<ErrorDTO> errors;
	
}
