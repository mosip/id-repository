package io.mosip.credential.request.generator.dto;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CredentialIssueResponseDto extends BaseRestResponseDTO {
	private static final long serialVersionUID = 1L;

	private CredentialIssueResponse response;

	private List<ErrorDTO> errors;
}
