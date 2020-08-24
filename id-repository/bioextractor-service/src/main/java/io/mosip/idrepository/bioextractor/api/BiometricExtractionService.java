package io.mosip.idrepository.bioextractor.api;

import io.mosip.idrepository.bioextractor.dto.BioExtractRequestDTO;
import io.mosip.idrepository.bioextractor.dto.BioExtractResponseDTO;
import io.mosip.idrepository.bioextractor.exception.BiometricExtractionException;

public interface BiometricExtractionService {

	BioExtractResponseDTO extractBiometrics(BioExtractRequestDTO bioExtractRequestDTO) throws BiometricExtractionException;

}
