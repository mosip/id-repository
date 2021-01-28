package io.mosip.idrepository.core.dto;

import lombok.Data;

@Data
public class CredentialRequestIdsDto {
	private String requestId;
	private String credentialType;
	private String partner;
	private String statusCode;
	private String statusComment;
	private String createDateTime;
	private String updateDateTime;
}
