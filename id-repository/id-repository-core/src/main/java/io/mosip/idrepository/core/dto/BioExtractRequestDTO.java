package io.mosip.idrepository.core.dto;

import java.util.Map;

import lombok.Data;

@Data
public class BioExtractRequestDTO {
	private Map<String, String> extractionFormats;
	private String biometrics;
}
