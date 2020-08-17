package io.mosip.kernel.bioextractor.api;

import io.mosip.kernel.bioextractor.dto.BioExtractResponseDTO;
import io.mosip.kernel.bioextractor.dto.BioExtractRequestDTO;
import io.mosip.kernel.bioextractor.exception.BiometricExtractionException;

public interface BiometricExtractionService {

	BioExtractResponseDTO extractBiometrics(BioExtractRequestDTO bioExtractRequestDTO) throws BiometricExtractionException;

}
