package io.mosip.credentialstore.dto;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class Filter implements Serializable {

	private static final long serialVersionUID = 1L;

	public String language;
	public String type;
	public List<String> subType;
}
