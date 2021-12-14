package io.mosip.idrepository.identity.test.helper;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.BiometricExtractionException;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.idrepository.identity.helper.BioExtractionHelper;
import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.kernel.core.bioapi.exception.BiometricException;

public class BioExtractionHelperTest {

	BioExtractionHelper bioExtractionHelper = new BioExtractionHelper();

	iBioProviderApi mockProvider;

	@Before
	public void init() throws BiometricException {
		BioAPIFactory mockBioAPIFactory = Mockito.mock(BioAPIFactory.class);
		ReflectionTestUtils.setField(bioExtractionHelper, "bioApiFactory", mockBioAPIFactory);
		mockProvider = Mockito.mock(iBioProviderApi.class);
		when(mockBioAPIFactory.getBioProvider(any(), any())).thenReturn(mockProvider);
	}

	@Test
	public void testExtractTemplates() throws Exception {
		String cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		List<BIR> birDataFromXMLType = CbeffValidator.getBIRDataFromXMLType(CryptoUtil.decodeURLSafeBase64(cbeff),
				"Finger");
		when(mockProvider.extractTemplate(any(), any())).thenReturn(birDataFromXMLType);
		List<BIR> extractedTemplates = bioExtractionHelper.extractTemplates(birDataFromXMLType, Map.of());
		assertEquals(birDataFromXMLType.size(), extractedTemplates.size());
	}


	@Test
	public void testExtractTemplatesWException() throws Exception {
		String cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		List<BIR> birDataFromXMLType = CbeffValidator.getBIRDataFromXMLType(CryptoUtil.decodeURLSafeBase64(cbeff),
				"Finger");
		when(mockProvider.extractTemplate(any(), any())).thenThrow(new NullPointerException());
		try {
			bioExtractionHelper.extractTemplates(birDataFromXMLType, Map.of());
		} catch (BiometricExtractionException e) {
			assertEquals(IdRepoErrorConstants.TECHNICAL_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.TECHNICAL_ERROR.getErrorMessage(), e.getErrorText());
		}
	}
}
