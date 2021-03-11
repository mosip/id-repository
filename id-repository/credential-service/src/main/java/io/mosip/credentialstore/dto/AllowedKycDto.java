package io.mosip.credentialstore.dto;

import java.util.List;

import lombok.Data;

@Data
public class AllowedKycDto {

	public String attributeName;
	
	public String group;

	public List<Source> source;

	public boolean encrypted;
	
	public String format;
}
