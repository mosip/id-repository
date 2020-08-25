package io.mosip.bioextractor.api;

import io.mosip.bioextractor.dto.BioExtractRequestDTO;
import io.mosip.bioextractor.dto.BioExtractResponseDTO;
import io.mosip.bioextractor.exception.BiometricExtractionException;

public interface BiometricExtractionService {

	BioExtractResponseDTO extractBiometrics(BioExtractRequestDTO bioExtractRequestDTO) throws BiometricExtractionException;

}
