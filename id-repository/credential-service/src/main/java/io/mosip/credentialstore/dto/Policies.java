package io.mosip.credentialstore.dto;

import java.util.List;

import lombok.Data;

@Data
public class Policies {
	
	private List<AllowedAuthType> allowedAuthTypes;

	private DataSharePolicies dataSharePolicies;

	private List<ShareableAttribute> shareableAttributes;
	
	private String authTokenType;
}
