package io.mosip.credentialstore.dto;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class AllowedKycDto implements Serializable {

	private static final long serialVersionUID = 1L;

	public String attributeName;
	
	public String group;

	public List<Source> source;

	public boolean encrypted;
	
	public String format;
}
