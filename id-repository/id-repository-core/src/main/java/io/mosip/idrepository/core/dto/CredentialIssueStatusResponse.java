package io.mosip.idrepository.core.dto;

import lombok.Data;

@Data
public class CredentialIssueStatusResponse {

	
	private String requestId;
	
	private String id;
	
	private String statusCode;

	private String url;
}
