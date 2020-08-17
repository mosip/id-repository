package io.mosip.credentialstore.dto;

import java.util.List;


import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
public class CredentialServiceResponseDto extends BaseRestResponseDto {

	private static final long serialVersionUID = 1L;

	private String  status;

	private List<ErrorDTO> errors;

}
