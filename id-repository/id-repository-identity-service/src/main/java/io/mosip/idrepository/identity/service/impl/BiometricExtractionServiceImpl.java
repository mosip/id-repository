package io.mosip.idrepository.identity.service.impl;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.mosip.idrepository.core.dto.BioExtractRequestDTO;
import io.mosip.idrepository.core.dto.BioExtractResponseDTO;
import io.mosip.idrepository.core.exception.BiometricExtractionException;
import io.mosip.idrepository.core.helper.BioExtractionHelper;
import io.mosip.idrepository.core.spi.BiometricExtractionService;
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
		String encodedExtractedBiometrrics = doBioExtraction(cbeffFileContent, bioExtractRequestDTO.getExtractionFormats());
		bioExtractPromiseResponseDTO.setExtractedBiometrics(encodedExtractedBiometrrics);
		return bioExtractPromiseResponseDTO;
	}

	private byte[] getCbeffFileContent(String biometrics) throws BiometricExtractionException {
		byte[] cbeffContent = CryptoUtil.decodeBase64(biometrics);
		return cbeffContent;
	}

	private String doBioExtraction(byte[] cbeffContent, Map<String, String> extractionFormats)
			throws BiometricExtractionException {
		byte[] extractedTemplatesCbeff = bioExractionHelper.extractTemplates(cbeffContent, extractionFormats);
		return CryptoUtil.encodeBase64(extractedTemplatesCbeff);
	}

}
