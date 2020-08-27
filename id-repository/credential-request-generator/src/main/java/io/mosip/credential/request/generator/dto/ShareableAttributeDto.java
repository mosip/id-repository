package io.mosip.credential.request.generator.dto;

import lombok.Data;

@Data
public class ShareableAttributeDto {

	private String attributeName;
	
	private boolean encrypted;
	
	private String format;
}
