package io.mosip.credentialstore.dto;

import lombok.Data;

@Data
public class DataProviderResponse {
	private String  credentialId;
	private String  issuanceDate;
	private String  signature;
	private byte[] formattedData;
}
