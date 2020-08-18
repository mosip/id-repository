package io.mosip.idrepository.core.dto;

import lombok.Data;

@Data
public class CredentialServiceResponse {
	private String  status;
	private String  credentialId;
	private String  issuanceDate;
	private String  signature;
	private String  dataShareUrl;
}
