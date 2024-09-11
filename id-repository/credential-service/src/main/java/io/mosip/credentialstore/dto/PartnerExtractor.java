package io.mosip.credentialstore.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PartnerExtractor implements Serializable {

	private static final long serialVersionUID = 1L;

	private String attributeName;

	private String biometric;

	private Extractor extractor;
}
