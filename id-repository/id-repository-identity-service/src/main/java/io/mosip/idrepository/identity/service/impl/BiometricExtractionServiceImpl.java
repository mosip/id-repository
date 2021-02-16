package io.mosip.idrepository.identity.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.idrepository.core.dto.BioExtractRequestDTO;
import io.mosip.idrepository.core.dto.BioExtractResponseDTO;
import io.mosip.idrepository.core.exception.BiometricExtractionException;
import io.mosip.idrepository.core.helper.BioExtractionHelper;
import io.mosip.idrepository.core.spi.BiometricExtractionService;
import io.mosip.kernel.core.cbeffutil.entity.BIR;

@Service
public class BiometricExtractionServiceImpl implements BiometricExtractionService {

	@Autowired
	private BioExtractionHelper bioExractionHelper;

	@Override
	public BioExtractResponseDTO extractBiometrics(BioExtractRequestDTO bioExtractRequestDTO)
			throws BiometricExtractionException {
		BioExtractResponseDTO bioExtractPromiseResponseDTO = new BioExtractResponseDTO();
		List<BIR> birs = bioExtractRequestDTO.getBiometrics();
		List<BIR> encodedExtractedBiometrics = doBioExtraction(birs, bioExtractRequestDTO.getExtractionFormats());
		bioExtractPromiseResponseDTO.setExtractedBiometrics(encodedExtractedBiometrics);
		return bioExtractPromiseResponseDTO;
	}

	private List<BIR> doBioExtraction(List<BIR> birs, Map<String, String> extractionFormats)
			throws BiometricExtractionException {
		return bioExractionHelper.extractTemplates(birs, extractionFormats);
	}

}
