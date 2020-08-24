package io.mosip.idrepository.bioextractor.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.idrepository.bioextractor.service.helper.BioExtractionHelper;
import io.mosip.idrepository.core.dto.BioExtractRequestDTO;
import io.mosip.idrepository.core.dto.BioExtractResponseDTO;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.kernel.core.cbeffutil.entity.BDBInfo;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.BIRType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.util.CryptoUtil;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
public class BiometricExtractionServiceImplTest {

	@Mock
	private CbeffUtil cbeffUtil;

	@Mock
	private BioAPIFactory bioApiFactory;

	@InjectMocks
	BioExtractionHelper bioExractionHelper;

	@InjectMocks
	BiometricExtractionServiceImpl biometricExtractionServiceImpl;

	@Before
	public void before() {
		ReflectionTestUtils.setField(biometricExtractionServiceImpl, "bioExractionHelper", bioExractionHelper);
	}

	@Test
	public void testExtractBiometrics() throws Exception {
		BioExtractRequestDTO bioExtractRequestDTO = new BioExtractRequestDTO();
		String biometrics = CryptoUtil.encodeBase64String("test".getBytes());
		bioExtractRequestDTO.setBiometrics(biometrics);
		BioExtractResponseDTO result;

		Mockito.when(cbeffUtil.getBIRDataFromXML(Mockito.any())).thenReturn((List<BIRType>) Mockito.mock(List.class));
		List<BIR> listBir = new ArrayList<>();
		BIR bir = new BIR.BIRBuilder()
				.withBdbInfo(
						new BDBInfo.BDBInfoBuilder().withType(Collections.singletonList(SingleType.FINGER)).build())
				.build();
		listBir.add(bir);
		Mockito.when(cbeffUtil.convertBIRTypeToBIR(Mockito.any())).thenReturn(listBir);

		iBioProviderApi bioProvider = Mockito.mock(iBioProviderApi.class);
		Mockito.when(bioApiFactory.getBioProvider(Mockito.any(), Mockito.any())).thenReturn(bioProvider);
		Mockito.when(bioProvider.extractTemplate(Mockito.any(), Mockito.any())).thenReturn(listBir);
		Mockito.when(cbeffUtil.createXML(Mockito.any())).thenReturn("result".getBytes());

		// default test
		result = biometricExtractionServiceImpl.extractBiometrics(bioExtractRequestDTO);

	}
}