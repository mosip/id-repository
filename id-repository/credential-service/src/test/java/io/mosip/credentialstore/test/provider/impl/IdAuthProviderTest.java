package io.mosip.credentialstore.test.provider.impl;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import io.mosip.credentialstore.dto.AllowedKycDto;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.EncryptZkResponseDto;
import io.mosip.credentialstore.dto.Source;
import io.mosip.credentialstore.dto.ZkDataAttribute;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.credentialstore.provider.impl.IdAuthProvider;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.kernel.core.cbeffutil.entity.BDBInfo;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.QualityType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;

@RunWith(SpringRunner.class)
@WebMvcTest
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class})
public class IdAuthProviderTest {
	@Mock
    Utilities utilities;	
	
	/** The env. */
	@Mock
	Environment env;
	
	
	@Mock
	EncryptionUtil encryptionUtil;
	
	private EncryptZkResponseDto encryptZkResponseDto;
	
	@InjectMocks
	private IdAuthProvider idAuthProvider;
	
	@Mock
	private CbeffUtil cbeffutil;
	
	@Before
	public void setUp() throws Exception {
		Mockito.when(env.getProperty("mosip.credential.service.datetime.pattern"))
		.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		encryptZkResponseDto=new EncryptZkResponseDto();
		List<ZkDataAttribute>  zkDataAttributeList=new ArrayList<>();
			ZkDataAttribute zkDataAttribute=new ZkDataAttribute();
			zkDataAttribute.setIdentifier("name");
			zkDataAttribute.setValue("test");
			zkDataAttributeList.add(zkDataAttribute);
			encryptZkResponseDto.setZkDataAttributes(zkDataAttributeList);
		Mockito.when(encryptionUtil.encryptDataWithZK(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(encryptZkResponseDto);
		
		List<BIR> birList = new ArrayList<>();
		BIR bir = new BIR();
		BDBInfo bdbInfoFace = new BDBInfo();
		List<SingleType> singleFaceList = new ArrayList<>();
		singleFaceList.add(SingleType.FACE);
		bdbInfoFace.setType(singleFaceList);
		bir.setBdbInfo(bdbInfoFace);
		birList.add(bir);
		BIR birFinger = new BIR();
		BDBInfo bdbInfoRightThumb = new BDBInfo();
		QualityType bdbInfoFingerQuality = new QualityType();
		bdbInfoFingerQuality.setScore(60L);
		List<SingleType> singleFingerList = new ArrayList<>();
		singleFingerList.add(SingleType.FINGER);
		bdbInfoRightThumb.setType(singleFingerList);
		List<String> subTypeList = new ArrayList<>();
		subTypeList.add("Right");
		subTypeList.add("Thumb");
		bdbInfoRightThumb.setSubtype(subTypeList);
		bdbInfoRightThumb.setQuality(bdbInfoFingerQuality);
		birFinger.setBdbInfo(bdbInfoRightThumb);
		birList.add(birFinger);

		BIR birLeftThumb = new BIR();
		BDBInfo bdbInfoLeftThumb = new BDBInfo();
		QualityType bdbInfoLeftThumbQuality = new QualityType();
		bdbInfoLeftThumbQuality.setScore(58L);

		bdbInfoLeftThumb.setType(singleFingerList);
		List<String> subTypeListLeftThumb = new ArrayList<>();
		subTypeListLeftThumb.add("Left");
		subTypeListLeftThumb.add("Thumb");
		bdbInfoLeftThumb.setSubtype(subTypeListLeftThumb);
		bdbInfoLeftThumb.setQuality(bdbInfoLeftThumbQuality);
		birLeftThumb.setBdbInfo(bdbInfoLeftThumb);
		birList.add(birLeftThumb);
		Mockito.when(cbeffutil.getBIRDataFromXML(Mockito.any())).thenReturn(new ArrayList<>());
		Mockito.when(cbeffutil.createXML(Mockito.any())).thenReturn("".getBytes());

	}
	@Test
	public void testGetFormattedCredentialDataSuccess() throws CredentialFormatterException {
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
        Map<String,Object> additionalData=new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		Map<AllowedKycDto, Object> sharableAttributes = new HashMap<>();

		AllowedKycDto kyc1 = new AllowedKycDto();
		kyc1.setAttributeName("fullName");
		kyc1.setEncrypted(true);
		List<Source> sourceList = new ArrayList<>();
		Source source1 = new Source();
		source1.setAttribute("fullName");

		sourceList.add(source1);
		kyc1.setSource(sourceList);

		AllowedKycDto kyc2 = new AllowedKycDto();
		kyc2.setAttributeName("biomterics");
		kyc2.setEncrypted(true);
		kyc2.setGroup("CBEFF");
		List<Source> sourceList1 = new ArrayList<>();
		Source source2 = new Source();
		source2.setAttribute("individualBiometrics");

		sourceList1.add(source2);
		kyc2.setSource(sourceList1);
		sharableAttributes.put(kyc1, "testname");
		sharableAttributes.put(kyc2, "biomtericencodedcbeffstring");
		DataProviderResponse dataProviderResponse = idAuthProvider
				.getFormattedCredentialData(credentialServiceRequestDto, sharableAttributes);
		assertNotNull(dataProviderResponse);
	}

	@Test(expected = CredentialFormatterException.class)
	public void testEncryptionFailure()
			throws CredentialFormatterException, DataEncryptionFailureException, ApiNotAccessibleException {
		CredentialServiceRequestDto credentialServiceRequestDto = new CredentialServiceRequestDto();
		Map<String, Object> additionalData = new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);

		Map<AllowedKycDto, Object> sharableAttributes = new HashMap<>();
		AllowedKycDto kyc1 = new AllowedKycDto();
		kyc1.setAttributeName("fullName");
		kyc1.setEncrypted(true);
		List<Source> sourceList = new ArrayList<>();
		Source source1 = new Source();
		source1.setAttribute("fullName");

		sourceList.add(source1);
		kyc1.setSource(sourceList);

		AllowedKycDto kyc2 = new AllowedKycDto();
		kyc2.setAttributeName("biomterics");
		kyc2.setEncrypted(true);
		kyc2.setGroup("CBEFF");
		List<Source> sourceList1 = new ArrayList<>();
		Source source2 = new Source();
		source2.setAttribute("individualBiometrics");

		sourceList1.add(source2);
		kyc2.setSource(sourceList1);
		sharableAttributes.put(kyc1, "testname");
		sharableAttributes.put(kyc2, "biomtericencodedcbeffstring");
		Mockito.when(encryptionUtil.encryptDataWithZK(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new DataEncryptionFailureException());
		idAuthProvider.getFormattedCredentialData(credentialServiceRequestDto, sharableAttributes);

	}

	@Test(expected = CredentialFormatterException.class)
	public void testApiNotAccessibleFailure()
			throws CredentialFormatterException, DataEncryptionFailureException, ApiNotAccessibleException {
		CredentialServiceRequestDto credentialServiceRequestDto = new CredentialServiceRequestDto();
		Map<String, Object> additionalData = new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		Map<AllowedKycDto, Object> sharableAttributes = new HashMap<>();
		AllowedKycDto kyc1 = new AllowedKycDto();
		kyc1.setAttributeName("fullName");
		kyc1.setEncrypted(true);
		List<Source> sourceList = new ArrayList<>();
		Source source1 = new Source();
		source1.setAttribute("fullName");

		sourceList.add(source1);
		kyc1.setSource(sourceList);

		AllowedKycDto kyc2 = new AllowedKycDto();
		kyc2.setAttributeName("biomterics");
		kyc2.setEncrypted(true);
		kyc2.setGroup("CBEFF");
		List<Source> sourceList1 = new ArrayList<>();
		Source source2 = new Source();
		source2.setAttribute("individualBiometrics");

		sourceList1.add(source2);
		kyc2.setSource(sourceList1);
		sharableAttributes.put(kyc1, "testname");
		sharableAttributes.put(kyc2, "biomtericencodedcbeffstring");
		Mockito.when(encryptionUtil.encryptDataWithZK(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new ApiNotAccessibleException());
		idAuthProvider.getFormattedCredentialData(credentialServiceRequestDto, sharableAttributes);

	}
}
