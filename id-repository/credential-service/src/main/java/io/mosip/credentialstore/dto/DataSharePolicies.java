package io.mosip.credentialstore.dto;

import lombok.Data;

@Data
public class DataSharePolicies {

	private int validForInMinutes;
	private int transactionsAllowed;
	private String encryptionType;
	private String shareDomain;

}
