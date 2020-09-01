package io.mosip.credentialstore.dto;

import java.util.List;

import lombok.Data;

@Data
public class Type {

	private String id;
	private String name;
	private String description;
	private List<Issuer> issuers;
}
