package io.mosip.idrepository.identity.helper;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.TECHNICAL_ERROR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.exception.BiometricExtractionException;
import io.mosip.kernel.biometrics.constant.BiometricFunction;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;

/**
 * The Class BioExtractionHelper.
 * 
 *  @author Loganathan Sekar
 */
@Component
public class BioExtractionHelper {
	
	/** The bio api factory. */
	@Autowired
	private BioAPIFactory bioApiFactory;
	
	/**
	 * Extract templates.
	 *
	 * @param birs the birs
	 * @param extractionFormats the extraction formats
	 * @return the list
	 * @throws BiometricExtractionException the biometric extraction exception
	 */
	public List<BIR> extractTemplates(List<BIR> birs, Map<String, String> extractionFormats) throws BiometricExtractionException {
		try {
			Map<BiometricType, List<BIR>> birsByType = birs.stream().collect(Collectors.groupingBy(bir -> bir.getBdbInfo().getType().get(0)));
			
			List<BIR> allExtractedTemplates =  new ArrayList<>();
			
			for (Entry<BiometricType,List<BIR>> entry : birsByType.entrySet()) {
				BiometricType modality = entry.getKey();
				iBioProviderApi bioProvider = bioApiFactory.getBioProvider(BiometricType.fromValue(modality.value()),
						BiometricFunction.EXTRACT);
				List<BIR> extractedTemplates = bioProvider.extractTemplate(entry.getValue(), extractionFormats);
				allExtractedTemplates.addAll(extractedTemplates);
			}
			
			return allExtractedTemplates;
			
		} catch (Exception e) {
			throw new BiometricExtractionException(TECHNICAL_ERROR, e);
		}
	}

}
