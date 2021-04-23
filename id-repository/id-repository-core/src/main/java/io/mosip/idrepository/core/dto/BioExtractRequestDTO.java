package io.mosip.idrepository.core.dto;

import java.util.List;
import java.util.Map;

import io.mosip.kernel.biometrics.entities.BIR;
import lombok.Data;

@Data
public class BioExtractRequestDTO {
	private Map<String, String> extractionFormats;
	private List<BIR> biometrics;
}
