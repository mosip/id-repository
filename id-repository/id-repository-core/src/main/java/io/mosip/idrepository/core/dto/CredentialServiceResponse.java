package io.mosip.idrepository.core.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CredentialServiceResponse {
	private String  status;
	private String  credentialId;
	private LocalDateTime  issuanceDate;
	private String  signature;
	private String  dataShareUrl;
}
