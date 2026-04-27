package io.mosip.credentialstore.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Extractor implements Serializable {

	private static final long serialVersionUID = 1L;

	private String provider;

	private String version;
}
