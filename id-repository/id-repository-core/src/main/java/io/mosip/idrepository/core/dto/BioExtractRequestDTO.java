package io.mosip.idrepository.core.dto;

import java.util.List;
import java.util.Map;

import io.mosip.kernel.core.cbeffutil.entity.BIR;
import lombok.Data;

@Data
public class BioExtractRequestDTO {
	private Map<String, String> extractionFormats;
	private List<BIR> biometrics;
}
