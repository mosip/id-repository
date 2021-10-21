package io.mosip.credentialstore.test.provider.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mvel2.MVEL;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.dto.AllowedKycDto;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.Filter;
import io.mosip.credentialstore.dto.PartnerCredentialTypePolicyDto;
import io.mosip.credentialstore.dto.PolicyAttributesDto;
import io.mosip.credentialstore.dto.Source;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.provider.impl.QrCodeProvider;
import io.mosip.credentialstore.util.DigitalSignatureUtil;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.kernel.core.cbeffutil.entity.BDBInfo;
import io.mosip.kernel.core.cbeffutil.entity.BIR;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.QualityType;
import io.mosip.kernel.core.cbeffutil.jaxbclasses.SingleType;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PowerMockRunnerDelegate(SpringRunner.class)
@PrepareForTest(value = MVEL.class)
public class QrCodeProviderTest {
	/** The environment. */
	@Mock
	private Environment environment;	
	
	/** The digital signature util. */
	@Mock
	DigitalSignatureUtil digitalSignatureUtil;
	
	/** The utilities. */
	@Mock
    Utilities utilities;
	
	/** The encryption util. */
	@Mock
	EncryptionUtil encryptionUtil;
	
	@InjectMocks
	private QrCodeProvider qrCodeProvider;
	
	
	/** The id response. */
	private IdResponseDTO idResponse = new IdResponseDTO();

	/** The response. */
	private ResponseDTO response = new ResponseDTO();
	
	PartnerCredentialTypePolicyDto policyResponse;

	@Mock
	private CbeffUtil cbeffutil;


	@Before
	public void setUp() throws DataEncryptionFailureException, ApiNotAccessibleException, SignatureException,
			IOException {
		PowerMockito.mockStatic(MVEL.class);
		Mockito.when(MVEL.executeExpression(Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
				.thenReturn("test");
		
		Mockito.when(environment.getProperty("mosip.credential.service.datetime.pattern"))
		.thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Mockito.when(encryptionUtil.encryptDataWithPin(Mockito.any(),Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn("testdata");
		
		Mockito.when(digitalSignatureUtil.sign(Mockito.any(), Mockito.any())).thenReturn("testdata");
		Mockito.when(utilities.generateId()).thenReturn("test123");
		Map<String, String> map1 = new HashMap<>();
		map1.put("UIN", "4238135072");
		JSONObject jsonObject = new JSONObject(map1);


		policyResponse = new PartnerCredentialTypePolicyDto();
		List<AllowedKycDto> shareableAttributes = new ArrayList<>();
		AllowedKycDto kyc1 = new AllowedKycDto();
		kyc1.setAttributeName("fullName");
		kyc1.setEncrypted(true);
		kyc1.setFormat("mask");
		List<Source> sourceList = new ArrayList<>();
		List<Filter> filterList1 = new ArrayList<>();
		Filter filter = new Filter();
		filter.setLanguage("fra");
		filterList1.add(filter);
		Source source1 = new Source();
		source1.setAttribute("fullName");
		source1.setFilter(filterList1);
		sourceList.add(source1);
		kyc1.setSource(sourceList);
		shareableAttributes.add(kyc1);
		AllowedKycDto kyc2 = new AllowedKycDto();
		kyc2.setAttributeName("dateOfBirth");
		kyc2.setEncrypted(true);
		kyc2.setFormat("YYYY");
		List<Source> sourceList2 = new ArrayList<>();
		Source source2 = new Source();
		source2.setAttribute("dateOfBirth");
		sourceList2.add(source2);
		kyc2.setSource(sourceList2);
		shareableAttributes.add(kyc2);
		AllowedKycDto kyc3 = new AllowedKycDto();
		kyc3.setAttributeName("biometrics");
		kyc3.setGroup("CBEFF");
		kyc3.setEncrypted(true);
		List<Source> sourceList3 = new ArrayList<>();
		List<Filter> filterList2 = new ArrayList<>();
		Filter filter1 = new Filter();
		filter1.setType("Face");
		filterList2.add(filter1);
		Filter filter2 = new Filter();
		filter2.setType("Finger");

		List<String> subtypeList = new ArrayList<>();
		subtypeList.add("Right Thumb");
		subtypeList.add("Left Thumb");
		filter2.setSubType(subtypeList);
		filterList2.add(filter2);
		Filter filter3 = new Filter();
		filter3.setType("Iris");

		List<String> subtypeList1 = new ArrayList<>();
		subtypeList1.add("Left");
		filter3.setSubType(subtypeList1);
		filterList2.add(filter3);
		Source source3 = new Source();
		source3.setAttribute("individualBiometrics");
		source3.setFilter(filterList2);
		sourceList3.add(source3);
		kyc3.setSource(sourceList3);
		shareableAttributes.add(kyc3);
		AllowedKycDto kyc4 = new AllowedKycDto();
		kyc4.setAttributeName("bestTwoFingers");
		kyc4.setGroup("CBEFF");
		kyc4.setEncrypted(true);
		kyc4.setFormat(CredentialConstants.BESTTWOFINGERS);
		List<Source> sourceList4 = new ArrayList<>();
		List<Filter> filterList3 = new ArrayList<>();

		Filter filter4 = new Filter();
		filter4.setType("Finger");
		filterList3.add(filter4);

		Source source4 = new Source();
		source4.setAttribute("individualBiometrics");
		source4.setFilter(filterList2);
		sourceList4.add(source4);
		kyc4.setSource(sourceList4);
		shareableAttributes.add(kyc4);

		PolicyAttributesDto dto = new PolicyAttributesDto();
		dto.setShareableAttributes(shareableAttributes);
		policyResponse.setPolicies(dto);


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
		Mockito.when(cbeffutil.convertBIRTypeToBIR(Mockito.any())).thenReturn(birList);
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

		sharableAttributes.put(kyc1, "testname");

		credentialServiceRequestDto.setEncrypt(true);
		credentialServiceRequestDto.setEncryptionKey("te1234");
		DataProviderResponse dataProviderResponse = qrCodeProvider
				.getFormattedCredentialData(
						credentialServiceRequestDto, sharableAttributes);
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

		sharableAttributes.put(kyc1, "testname");
		credentialServiceRequestDto.setEncrypt(true);
		credentialServiceRequestDto.setEncryptionKey("te1234");
		Mockito.when(encryptionUtil.encryptDataWithPin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new DataEncryptionFailureException());
		qrCodeProvider.getFormattedCredentialData(credentialServiceRequestDto, sharableAttributes);

	}


	@Test(expected = CredentialFormatterException.class)
	public void testApiNotAccessible()
			throws CredentialFormatterException, DataEncryptionFailureException, ApiNotAccessibleException {
		CredentialServiceRequestDto credentialServiceRequestDto = new CredentialServiceRequestDto();
		Map<String, Object> additionalData = new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		Map<String, Boolean> encryptMap = new HashMap<>();
		encryptMap.put("name", true);
		Map<AllowedKycDto, Object> sharableAttributes = new HashMap<>();
		AllowedKycDto kyc1 = new AllowedKycDto();
		kyc1.setAttributeName("fullName");
		kyc1.setEncrypted(true);
		List<Source> sourceList = new ArrayList<>();
		Source source1 = new Source();
		source1.setAttribute("fullName");

		sourceList.add(source1);
		kyc1.setSource(sourceList);

		sharableAttributes.put(kyc1, "testname");
		credentialServiceRequestDto.setEncrypt(true);
		credentialServiceRequestDto.setEncryptionKey("te1234");
		Mockito.when(encryptionUtil.encryptDataWithPin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new ApiNotAccessibleException());
		qrCodeProvider.getFormattedCredentialData(credentialServiceRequestDto, sharableAttributes);

	}
	
	@Test
	public void testPrepareSharableAttributesSuccess() throws CredentialFormatterException {
		LinkedHashMap<String, Object> identityMap = new LinkedHashMap<>();
		Map<String, String> map = new HashMap<>();
		map.put("language", "eng");
		map.put("value", "raghav");
		JSONObject j1 = new JSONObject(map);

		Map<String, String> map2 = new HashMap<>();
		map2.put("language", "ara");
		map2.put("value", "Alok");
		JSONObject j2 = new JSONObject(map2);
		JSONArray array = new JSONArray();
		array.add(j1);
		array.add(j2);
		identityMap.put("fullName", array);

		identityMap.put("dateOfBirth", "1980/11/14");

		Object identity = identityMap;
		response.setIdentity(identity);

		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("individualBiometrics");

		doc1.setValue("text biomterics");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);

		response.setDocuments(docList);
		idResponse.setResponse(response);
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();

		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = qrCodeProvider.prepareSharableAttributes(idResponse,
				policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}

	@Test
	public void testPrepareSharableAttributesWithFullNameSuccess() throws CredentialFormatterException {
		LinkedHashMap<String, Object> identityMap = new LinkedHashMap<>();
		Map<String, String> map = new HashMap<>();
		map.put("language", "eng");
		map.put("value", "raghav");
		JSONObject j1 = new JSONObject(map);

		Map<String, String> map2 = new HashMap<>();
		map2.put("language", "ara");
		map2.put("value", "Alok");
		JSONObject j2 = new JSONObject(map2);
		JSONArray array = new JSONArray();
		array.add(j1);
		array.add(j2);
		identityMap.put("firstName", array);
		identityMap.put("middleName", array);
		identityMap.put("lastName", array);
		identityMap.put("dateOfBirth", "1980/11/14");

		Object identity = identityMap;
		response.setIdentity(identity);

		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("individualBiometrics");

		doc1.setValue("text biomterics");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);

		response.setDocuments(docList);
		idResponse.setResponse(response);
		CredentialServiceRequestDto credentialServiceRequestDto = new CredentialServiceRequestDto();
		List<String> sharableAttributesList = new ArrayList<>();
		sharableAttributesList.add("fullName");
		credentialServiceRequestDto.setSharableAttributes(sharableAttributesList);
		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = qrCodeProvider.prepareSharableAttributes(idResponse,
				policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}
}
