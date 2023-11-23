package io.mosip.idrepository.core.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CredentialRequestV2DTO  extends CredentialIssueRequestDto  {
	
	private String requestId;
}
