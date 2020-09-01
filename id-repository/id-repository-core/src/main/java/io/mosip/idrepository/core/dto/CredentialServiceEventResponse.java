package io.mosip.idrepository.core.dto;

import java.util.Map;

import lombok.Data;

@Data
public class CredentialServiceEventResponse {
	
	private String  dataShareUrl;
	private Map<String,Object> additionalData;
}
