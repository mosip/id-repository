package io.mosip.idrepository.core.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class CredentialIssueRequestDto {


	
	private String id;
	private String credentialType;
	//partner id
	private String issuer;
    private String  recepiant;
	private String user;
	private boolean encrypt;
	private String encryptionKey;
    private List<String> sharableAttributes;
    private Map<String,Object> additionalData;
	
}
