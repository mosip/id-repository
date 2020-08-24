package io.mosip.idrepository.core.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class CredentialServiceRequestDto {
	
	private String id;
	private String credentialType;
	private String requestId;
	private String issuer;
    private String  recepiant;
	private String user;
	private boolean encrypt;
	private String encryptionKey;
    private List<String> sharableAttributes;
    private Map<String,Object> additionalData;
}
