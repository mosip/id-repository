package io.mosip.idrepository.core.dto;

import java.util.List;

import io.mosip.kernel.core.cbeffutil.entity.BIR;
import lombok.Data;

@Data
public class BioExtractResponseDTO {
	private List<BIR> extractedBiometrics;
}
