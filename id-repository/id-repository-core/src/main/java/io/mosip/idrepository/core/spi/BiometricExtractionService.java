package io.mosip.idrepository.core.spi;

import io.mosip.idrepository.core.dto.BioExtractRequestDTO;
import io.mosip.idrepository.core.dto.BioExtractResponseDTO;
import io.mosip.idrepository.core.exception.BiometricExtractionException;

public interface BiometricExtractionService {

	BioExtractResponseDTO extractBiometrics(BioExtractRequestDTO bioExtractRequestDTO) throws BiometricExtractionException;

}
