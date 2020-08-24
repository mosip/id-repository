package io.mosip.idrepository.bioextractor.api;

import io.mosip.idrepository.bioextractor.exception.BiometricExtractionException;
import io.mosip.idrepository.core.dto.BioExtractRequestDTO;
import io.mosip.idrepository.core.dto.BioExtractResponseDTO;

public interface BiometricExtractionService {

	BioExtractResponseDTO extractBiometrics(BioExtractRequestDTO bioExtractRequestDTO) throws BiometricExtractionException;

}
