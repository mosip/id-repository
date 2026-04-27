package io.mosip.credentialstore.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DataShareDto implements Serializable {

	private static final long serialVersionUID = 1L;

	private String validForInMinutes;
	
	private String transactionsAllowed;
	
	private String encryptionType;
	
	private String shareDomain;
	
	private String typeOfShare;
	
	private String source;
}
