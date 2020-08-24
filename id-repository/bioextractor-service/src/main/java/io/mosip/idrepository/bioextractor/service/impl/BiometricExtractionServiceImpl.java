package io.mosip.idrepository.bioextractor.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.idrepository.bioextractor.api.BiometricExtractionService;
import io.mosip.idrepository.bioextractor.exception.BiometricExtractionException;
import io.mosip.idrepository.bioextractor.service.helper.BioExtractionHelper;
import io.mosip.idrepository.core.dto.BioExtractRequestDTO;
import io.mosip.idrepository.core.dto.BioExtractResponseDTO;
import io.mosip.kernel.core.util.CryptoUtil;

@Service
public class BiometricExtractionServiceImpl implements BiometricExtractionService {

	@Autowired
	private BioExtractionHelper bioExractionHelper;

	@Override
	public BioExtractResponseDTO extractBiometrics(BioExtractRequestDTO bioExtractRequestDTO)
			throws BiometricExtractionException {
		BioExtractResponseDTO bioExtractPromiseResponseDTO = new BioExtractResponseDTO();

		String biometrics = bioExtractRequestDTO.getBiometrics();
		byte[] cbeffFileContent = getCbeffFileContent(biometrics);
		String encodedExtractedBiometrrics = doBioExtraction(cbeffFileContent);
		bioExtractPromiseResponseDTO.setExtractedBiometrics(encodedExtractedBiometrrics);
		return bioExtractPromiseResponseDTO;
	}

	private byte[] getCbeffFileContent(String biometrics) throws BiometricExtractionException {
		byte[] cbeffContent = CryptoUtil.decodeBase64(biometrics);
		return cbeffContent;
	}

	private String doBioExtraction(byte[] cbeffContent)
			throws BiometricExtractionException {
		byte[] extractedTemplatesCbeff = bioExractionHelper.extractTemplates(cbeffContent);
		return CryptoUtil.encodeBase64(extractedTemplatesCbeff);
	}

}
