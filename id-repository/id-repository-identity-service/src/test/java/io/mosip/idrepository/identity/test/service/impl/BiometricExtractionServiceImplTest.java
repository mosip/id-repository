package io.mosip.idrepository.identity.test.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.commons.khazana.exception.ObjectStoreAdapterException;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.BiometricExtractionException;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.helper.BioExtractionHelper;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;
import io.mosip.idrepository.identity.service.impl.BiometricExtractionServiceImpl;
import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.util.CryptoUtil;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ActiveProfiles("test")
public class BiometricExtractionServiceImplTest {

	@InjectMocks
	private BiometricExtractionServiceImpl extractionServiceImpl;

	@Mock
	private ObjectStoreHelper objectStoreHelper;

	@Mock
	private BioExtractionHelper bioExractionHelper;

	@Mock
	private CbeffUtil cbeffUtil;

	@Test
	public void testExtractTemplateExtractionExists() throws Exception {
		when(objectStoreHelper.biometricObjectExists(any(), any())).thenReturn(true);
		String cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		List<BIR> birDataFromXMLType = CbeffValidator.getBIRDataFromXMLType(CryptoUtil.decodeURLSafeBase64(cbeff),
				"Finger");
		when(objectStoreHelper.getBiometricObject(any(), any())).thenReturn(CryptoUtil.decodeURLSafeBase64(cbeff));
		when(cbeffUtil.getBIRDataFromXML(any())).thenReturn(birDataFromXMLType);
		CompletableFuture<List<BIR>> extractTemplate = extractionServiceImpl.extractTemplate("", "", "", "", birDataFromXMLType);
		assertEquals(birDataFromXMLType.size(), extractTemplate.join().size());
	}

	@Test
	public void testExtractTemplateExtractionNotExists() throws Exception {
		when(objectStoreHelper.biometricObjectExists(any(), any())).thenReturn(false);
		String cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		List<BIR> birDataFromXMLType = CbeffValidator.getBIRDataFromXMLType(CryptoUtil.decodeURLSafeBase64(cbeff),
				"Finger");
		when(objectStoreHelper.getBiometricObject(any(), any())).thenReturn(CryptoUtil.decodeURLSafeBase64(cbeff));
		when(cbeffUtil.getBIRDataFromXML(any())).thenReturn(birDataFromXMLType);
		when(bioExractionHelper.extractTemplates(any(), any())).thenReturn(birDataFromXMLType);
		CompletableFuture<List<BIR>> extractTemplate = extractionServiceImpl.extractTemplate("", "", "a", "ExtractionFormat", birDataFromXMLType);
		assertEquals(birDataFromXMLType.size(), extractTemplate.join().size());
	}

	@Test
	public void testExtractTemplateExtractedBioIsEmpty() throws Exception {
		when(objectStoreHelper.biometricObjectExists(any(), any())).thenReturn(false);
		String cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		List<BIR> birDataFromXMLType = CbeffValidator.getBIRDataFromXMLType(CryptoUtil.decodeURLSafeBase64(cbeff),
				"Finger");
		when(objectStoreHelper.getBiometricObject(any(), any())).thenReturn(CryptoUtil.decodeURLSafeBase64(cbeff));
		when(cbeffUtil.getBIRDataFromXML(any())).thenReturn(birDataFromXMLType);
		when(bioExractionHelper.extractTemplates(any(), any())).thenReturn(List.of());
		CompletableFuture<List<BIR>> extractTemplate = extractionServiceImpl.extractTemplate("", "", "a", "ExtractionFormat", birDataFromXMLType);
		assertEquals(0, extractTemplate.join().size());
		verify(objectStoreHelper, never()).putBiometricObject(any(), any(), any());
	}

	@Test
	public void testExtractTemplateObjectStoreFailure() throws Exception {
		when(objectStoreHelper.biometricObjectExists(any(), any())).thenReturn(true);
		String cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		List<BIR> birDataFromXMLType = CbeffValidator.getBIRDataFromXMLType(CryptoUtil.decodeURLSafeBase64(cbeff),
				"Finger");
		when(objectStoreHelper.getBiometricObject(any(), any())).thenThrow(new ObjectStoreAdapterException("", ""));
		when(cbeffUtil.getBIRDataFromXML(any())).thenReturn(birDataFromXMLType);
		when(bioExractionHelper.extractTemplates(any(), any())).thenReturn(birDataFromXMLType);
		CompletableFuture<List<BIR>> extractTemplate = extractionServiceImpl.extractTemplate("", "", "a", "ExtractionFormat", birDataFromXMLType);
		assertEquals(birDataFromXMLType.size(), extractTemplate.join().size());
	}

	@Test
	public void testExtractTemplateBioExtractionFailure() throws Exception {
		when(objectStoreHelper.biometricObjectExists(any(), any())).thenReturn(false);
		String cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		List<BIR> birDataFromXMLType = CbeffValidator.getBIRDataFromXMLType(CryptoUtil.decodeURLSafeBase64(cbeff),
				"Finger");
		when(objectStoreHelper.getBiometricObject(any(), any())).thenReturn(CryptoUtil.decodeURLSafeBase64(cbeff));
		when(cbeffUtil.getBIRDataFromXML(any())).thenReturn(birDataFromXMLType);
		when(bioExractionHelper.extractTemplates(any(), any())).thenThrow(new BiometricExtractionException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
		extractionServiceImpl.extractTemplate("", "", "a", "ExtractionFormat", birDataFromXMLType);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.BIO_EXTRACTION_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.BIO_EXTRACTION_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testExtractTemplateUnknownError() throws Exception {
		when(objectStoreHelper.biometricObjectExists(any(), any())).thenReturn(false);
		String cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		List<BIR> birDataFromXMLType = CbeffValidator.getBIRDataFromXMLType(CryptoUtil.decodeURLSafeBase64(cbeff),
				"Finger");
		when(objectStoreHelper.getBiometricObject(any(), any())).thenReturn(CryptoUtil.decodeURLSafeBase64(cbeff));
		when(cbeffUtil.getBIRDataFromXML(any())).thenReturn(birDataFromXMLType);
		when(bioExractionHelper.extractTemplates(any(), any())).thenThrow(new NullPointerException());
		try {
		extractionServiceImpl.extractTemplate("", "", "a", "ExtractionFormat", birDataFromXMLType);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

}
