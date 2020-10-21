package io.mosip.credentialstore.dto;

import lombok.Data;

@Data
public class PartnerExtractorResponse {

	private String attributeName;
	
	private String biometric;
	
	private Extractor extractor;
}
