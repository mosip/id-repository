package io.mosip.idrepository.core.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CredentialIssueStatusResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	private String requestId;
	
	private String id;
	
	private String statusCode;

	private String url;
}
