package io.mosip.credentialstore.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DataProviderResponse {
	private String  credentialId;
	private LocalDateTime  issuanceDate;
	private byte[] formattedData;
}
