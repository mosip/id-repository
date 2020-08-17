package io.mosip.credentialstore.dto;

import java.util.List;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PolicyResponse extends BaseRestResponseDto {
	private static final long serialVersionUID = 1L;

	private PolicyDetailResponseDto response;

	private List<ErrorDTO> errors;
}
