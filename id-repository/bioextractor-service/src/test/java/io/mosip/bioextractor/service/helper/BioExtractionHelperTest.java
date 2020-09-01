package io.mosip.bioextractor.service.helper;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.bioextractor.exception.BiometricExtractionException;
import io.mosip.bioextractor.service.helper.BioExtractionHelper;
import io.mosip.kernel.biosdk.provider.factory.BioAPIFactory;
import io.mosip.kernel.biosdk.provider.spi.iBioProviderApi;
import io.mosip.kernel.core.cbeffutil.entity.BDBInfo;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.BIRType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
public class BioExtractionHelperTest {
	
	@Mock
	private CbeffUtil cbeffUtil;
	
	@Mock
	private BioAPIFactory bioApiFactory;
	
	@InjectMocks
	BioExtractionHelper bioExtractionHelper;

	@Test
	public void testExtractTemplates() throws Exception {
		byte[] cbeffContent = new byte[] { ' ' };
		byte[] result;
		
		Mockito.when(cbeffUtil.getBIRDataFromXML(cbeffContent)).thenReturn((List<BIRType>) Mockito.mock(List.class));
		List<BIR> listBir = new ArrayList<>();
		BIR bir= new BIR.BIRBuilder().withBdbInfo(new BDBInfo.BDBInfoBuilder().withType(List.of(SingleType.FINGER)).build()).build();
		listBir.add(bir);
		Mockito.when(cbeffUtil.convertBIRTypeToBIR(Mockito.any())).thenReturn(listBir);
		
		iBioProviderApi bioProvider = Mockito.mock(iBioProviderApi.class);
		Mockito.when(bioApiFactory.getBioProvider(Mockito.any(),
						Mockito.any())).thenReturn(bioProvider);
		Mockito.when(bioProvider.extractTemplate(Mockito.any(), Mockito.any())).thenReturn(listBir);
		Mockito.when(cbeffUtil.createXML(Mockito.any())).thenReturn("result".getBytes());
		
		// default test
		result = bioExtractionHelper.extractTemplates(cbeffContent);
		assertArrayEquals("result".getBytes(), result);
	}
	
	@Test(expected = BiometricExtractionException.class)
	public void testExtractTemplates_Exception() throws Exception {
		byte[] cbeffContent = new byte[] { ' ' };
		byte[] result;
		
		Mockito.when(cbeffUtil.getBIRDataFromXML(cbeffContent)).thenReturn((List<BIRType>) Mockito.mock(List.class));
		List<BIR> listBir = new ArrayList<>();
		BIR bir= new BIR.BIRBuilder().withBdbInfo(new BDBInfo.BDBInfoBuilder().withType(List.of(SingleType.FINGER)).build()).build();
		listBir.add(bir);
		Mockito.when(cbeffUtil.convertBIRTypeToBIR(Mockito.any())).thenReturn(listBir);
		
		iBioProviderApi bioProvider = Mockito.mock(iBioProviderApi.class);
		Mockito.when(bioApiFactory.getBioProvider(Mockito.any(),
						Mockito.any())).thenReturn(bioProvider);
		Mockito.when(bioProvider.extractTemplate(Mockito.any(), Mockito.any())).thenThrow(new RuntimeException("Technical error"));
		Mockito.when(cbeffUtil.createXML(Mockito.any())).thenReturn("result".getBytes());
		
		// default test
		bioExtractionHelper.extractTemplates(cbeffContent);
	}
}