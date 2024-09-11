package io.mosip.credentialstore.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AuthPolicyDto implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String authType;
	
	private String authSubType;
	

	private boolean mandatory;
}
