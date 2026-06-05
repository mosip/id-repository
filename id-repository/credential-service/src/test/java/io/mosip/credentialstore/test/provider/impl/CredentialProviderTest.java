package io.mosip.credentialstore.test.provider.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.dto.*;
import io.mosip.credentialstore.exception.*;
import io.mosip.credentialstore.util.VIDUtil;
import io.mosip.idrepository.core.dto.*;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.util.DateUtils2;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.util.EnvUtil;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
@PowerMockRunnerDelegate(SpringRunner.class)
@PrepareForTest(value = MVEL.class)
public class CredentialProviderTest {
	/** The environment. */
	@Mock
	private EnvUtil environment;	
	

	
	/** The utilities. */
	@Mock
    Utilities utilities;
	
	/** The encryption util. */
	@Mock
	EncryptionUtil encryptionUtil;
	
	@Mock
	Environment env;

	@Mock
	VIDUtil vidUtil;

	@Mock
	CbeffUtil cbeffutil;
	
	@InjectMocks
	private CredentialProvider credentialDefaultProvider;

	
	/** The id response. */
	private IdResponseDTO idResponse = new IdResponseDTO();

	/** The response. */
	private ResponseDTO response = new ResponseDTO();

	PartnerCredentialTypePolicyDto policyResponse;
	
	IdentityMapping identityMapping;
	
	private ObjectMapper mapper = new ObjectMapper();

	private boolean shouldRunBeforeMethod = true;

	@Rule
	public TestWatcher testWatcher = new TestWatcher() {

		@Override
		protected void starting(Description description) {
			if (description.getMethodName().equals("testPrepareSharableAttributesWithoutPolicy")) {
				shouldRunBeforeMethod = false;
			} else {
				shouldRunBeforeMethod = true;
			}
		}
	};
	
	@Before
	public void setUp() throws DataEncryptionFailureException, ApiNotAccessibleException, SignatureException,Exception {

		ReflectionTestUtils.setField(credentialDefaultProvider, "mapper", mapper);
		ReflectionTestUtils.setField(credentialDefaultProvider, "env", env);
		ReflectionTestUtils.setField(credentialDefaultProvider, "convertRequestVer", "ISO19794_5_2011");
		Mockito.when(env.getProperty(Mockito.anyString(), Mockito.anyString())).thenReturn("abc");
		PowerMockito.mockStatic(MVEL.class);

		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		identityMapping = mapper.readValue(
				IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-mapping.json"),
						StandardCharsets.UTF_8),
				IdentityMapping.class);
		IdentityIssuanceProfileBuilder.setIdentityMapping(identityMapping);
		IdentityIssuanceProfileBuilder.setDateFormat("uuuu/MM/dd");

		EnvUtil.setDateTimePattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		Mockito.when(encryptionUtil.encryptDataWithPin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn("testdata");
		Mockito.when(MVEL.executeExpression(Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
		.thenReturn("test");
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Mockito.when(utilities.generateId()).thenReturn("test123");

		if (shouldRunBeforeMethod) {
			policyResponse = new PartnerCredentialTypePolicyDto();
			List<AllowedKycDto> shareableAttributes = new ArrayList<>();
			AllowedKycDto kyc1 = new AllowedKycDto();
			kyc1.setAttributeName("fullName");
			kyc1.setEncrypted(true);

			List<Source> sourceList = new ArrayList<>();

			Source source1 = new Source();
			source1.setAttribute("fullName");

			sourceList.add(source1);
			kyc1.setSource(sourceList);
			shareableAttributes.add(kyc1);
			AllowedKycDto kyc2 = new AllowedKycDto();
			kyc2.setAttributeName("dateOfBirth");
			kyc2.setEncrypted(true);

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
			Source source3 = new Source();
			source3.setAttribute("individualBiometrics");
			sourceList3.add(source3);
			kyc3.setSource(sourceList3);
			shareableAttributes.add(kyc3);
			AllowedKycDto kyc4 = new AllowedKycDto();
			List<Source> sourceList4 = new ArrayList<>();
			Source source4 = new Source();
			source4.setAttribute("email");
			sourceList4.add(source4);
			kyc4.setSource(sourceList4);
			kyc4.setAttributeName("email");
			kyc4.setEncrypted(true);
			kyc4.setGroup(CredentialConstants.CBEFF);
			shareableAttributes.add(kyc4);
			PolicyAttributesDto dto = new PolicyAttributesDto();
			dto.setShareableAttributes(shareableAttributes);
			policyResponse.setPolicies(dto);
		}
	}

	@Test
	public void testGetFormattedCredentialDataSuccess() throws CredentialFormatterException {
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
		DataProviderResponse dataProviderResponse = credentialDefaultProvider
				.getFormattedCredentialData(
						credentialServiceRequestDto, sharableAttributes);
		assertNotNull(dataProviderResponse);
	}

	@Test
	public void getFormattedCredentialData_WithValidKyc_ReturnDataProviderResponse() throws CredentialFormatterException {
		CredentialServiceRequestDto credentialServiceRequestDto = new CredentialServiceRequestDto();
		Map<String, Object> additionalData = new HashMap<>();
		credentialServiceRequestDto.setAdditionalData(additionalData);
		Map<AllowedKycDto, Object> sharableAttributes = new HashMap<>();

		AllowedKycDto kyc1 = new AllowedKycDto();
		kyc1.setAttributeName("fullName");
		kyc1.setEncrypted(false);
		List<Source> sourceList = new ArrayList<>();
		Source source1 = new Source();
		source1.setAttribute("fullName");

		sourceList.add(source1);
		kyc1.setSource(sourceList);

		AllowedKycDto kyc2 = new AllowedKycDto();
		kyc2.setAttributeName("biomterics");
		kyc2.setEncrypted(false);
		kyc2.setGroup("CBEFF");
		List<Source> sourceList1 = new ArrayList<>();
		Source source2 = new Source();
		source2.setAttribute("individualBiometrics");

		sourceList1.add(source2);
		kyc2.setSource(sourceList1);
		sharableAttributes.put(kyc1, 2);
		sharableAttributes.put(kyc2, 3);
		DataProviderResponse dataProviderResponse = credentialDefaultProvider
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

		credentialServiceRequestDto.setEncrypt(true);
		credentialServiceRequestDto.setEncryptionKey("te1234");
		Mockito.when(encryptionUtil.encryptDataWithPin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new DataEncryptionFailureException());
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
		credentialDefaultProvider.getFormattedCredentialData(credentialServiceRequestDto,
				sharableAttributes);

	}


	@Test(expected = CredentialFormatterException.class)
	public void testApiNotAccessible()
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
		Mockito.when(encryptionUtil.encryptDataWithPin(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new ApiNotAccessibleException());
		credentialDefaultProvider.getFormattedCredentialData(credentialServiceRequestDto,
				sharableAttributes);

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
		CredentialServiceRequestDto credentialServiceRequestDto = getCredentialServiceRequestDto();

		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse,
				policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}

	@Test
	public void prepareSharableAttributes_WhenIdentityAndDocumentsAreProvided() throws CredentialFormatterException {
		Mockito.when(env.getProperty(Mockito.anyString(), Mockito.anyString())).thenReturn("fullName");
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
		CredentialServiceRequestDto credentialServiceRequestDto = getCredentialServiceRequestDto();

		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse,
						policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}

	@Test
	public void prepareSharableAttributesWithPolicy_ShouldReturnValidAttributes() throws CredentialFormatterException, ApiNotAccessibleException, IdRepoException {
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

		identityMap.put("UIN", 3000);

		Object identity = identityMap;
		response.setIdentity(identity);

		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("individualBiometrics");

		doc1.setValue("text biomterics");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);

		response.setDocuments(docList);
		idResponse.setResponse(response);
		CredentialServiceRequestDto credentialServiceRequestDto = getCredentialServiceRequestDto();

		CredentialServiceRequestDto credReq = new CredentialServiceRequestDto();
		List<String> attributes = new ArrayList<String>();

		attributes.add("abc");

		List<String> maskingAttributesList = new ArrayList<String>();
		maskingAttributesList.add("phone");
		maskingAttributesList.add("email");
		maskingAttributesList.add("uin");
		maskingAttributesList.add("vid");

		Map<String,Object> attributeFormat= new HashMap<String,Object>();

		attributeFormat.put("dateOfBirth", "DD/MMM/YYYY");
		attributeFormat.put("fullAddress","");
		attributeFormat.put("name","");
		attributeFormat.put("fullName","");

		Map<String,Object>additionalData=new HashMap<String,Object>();

		additionalData.put("formatingAttributes",attributeFormat);
		additionalData.put("maskingAttributes",maskingAttributesList);

		credReq.setId("2361485607");
		credReq.setCredentialType("euin");
		credReq.setIssuer("mpartner-default-print");
		credReq.setEncryptionKey("JQ5sLK6Sq11SzUZq");
		credReq.setEncrypt(Boolean.FALSE);
		credReq.setSharableAttributes(attributes);
		credReq.setAdditionalData(additionalData);

		policyResponse = new PartnerCredentialTypePolicyDto();
		List<AllowedKycDto> shareableAttributes = new ArrayList<>();
		AllowedKycDto kyc1 = new AllowedKycDto();
		kyc1.setAttributeName("fullName");
		kyc1.setEncrypted(true);
		kyc1.setFormat(CredentialConstants.RETRIEVE);

		List<Source> sourceList = new ArrayList<>();

		Source source1 = new Source();
		source1.setAttribute(CredentialConstants.VID);
		List<Filter> filterList = new ArrayList<>();
		Filter filter = new Filter();
		filter.setLanguage("en");
		filter.setType("Domain");
		filterList.add(filter);
		source1.setFilter(filterList);

		sourceList.add(source1);
		kyc1.setSource(sourceList);
		shareableAttributes.add(kyc1);
		AllowedKycDto kyc2 = new AllowedKycDto();
		kyc2.setAttributeName("dateOfBirth");
		kyc2.setEncrypted(true);

		List<Source> sourceList2 = new ArrayList<>();
		Source source2 = new Source();
		source2.setAttribute("Test1");
		sourceList2.add(source2);
		kyc2.setSource(sourceList2);
		shareableAttributes.add(kyc2);
		AllowedKycDto kyc3 = new AllowedKycDto();
		kyc3.setAttributeName("biometrics");
		kyc3.setGroup("CBEFF");
		kyc3.setEncrypted(true);
		List<Source> sourceList3 = new ArrayList<>();
		Source source3 = new Source();
		source3.setAttribute("Test2");
		sourceList3.add(source3);
		kyc3.setSource(sourceList3);
		shareableAttributes.add(kyc3);
		AllowedKycDto kyc4 = new AllowedKycDto();
		List<Source> sourceList4 = new ArrayList<>();
		Source source4 = new Source();
		source4.setAttribute("test4");
		sourceList4.add(source4);
		kyc4.setSource(sourceList4);
		kyc4.setAttributeName("email");
		kyc4.setEncrypted(true);
		kyc4.setGroup(CredentialConstants.CBEFF);
		shareableAttributes.add(kyc4);
		PolicyAttributesDto dto = new PolicyAttributesDto();
		dto.setShareableAttributes(shareableAttributes);
		policyResponse.setPolicies(dto);

		VidResponseDTO vidResponseDTO = mock(VidResponseDTO.class);
		Mockito.when(vidUtil.generateVID(Mockito.any(), Mockito.any()))
				.thenReturn(vidResponseDTO);

		VidInfoDTO vidInfoDTO = new VidInfoDTO();
		vidInfoDTO.setExpiryTimestamp(DateUtils2.getUTCCurrentDateTime());
		Mockito.when(vidUtil.getVIDData(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(vidInfoDTO);

		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse,
						policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}

	@Test
	public void prepareSharableAttributes_WithPolicyAttributes_ReturnValidMap() throws Exception {
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

		identityMap.put("UIN", 3000);

		Object identity = identityMap;
		response.setIdentity(identity);

		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("APEX");

		doc1.setValue("textbiomterics");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);

		response.setDocuments(docList);
		idResponse.setResponse(response);
		CredentialServiceRequestDto credentialServiceRequestDto = getCredentialServiceRequestDto();

		CredentialServiceRequestDto credReq = new CredentialServiceRequestDto();
		List<String> attributes = new ArrayList<String>();

		attributes.add("abc");

		List<String> maskingAttributesList = new ArrayList<String>();
		maskingAttributesList.add("phone");
		maskingAttributesList.add("email");
		maskingAttributesList.add("uin");
		maskingAttributesList.add("vid");

		Map<String,Object> attributeFormat= new HashMap<String,Object>();

		attributeFormat.put("dateOfBirth", "DD/MMM/YYYY");
		attributeFormat.put("fullAddress","");
		attributeFormat.put("name","");
		attributeFormat.put("fullName","");

		Map<String,Object>additionalData=new HashMap<String,Object>();

		additionalData.put("formatingAttributes",attributeFormat);
		additionalData.put("maskingAttributes",maskingAttributesList);

		credReq.setId("2361485607");
		credReq.setCredentialType("euin");
		credReq.setIssuer("mpartner-default-print");
		credReq.setEncryptionKey("JQ5sLK6Sq11SzUZq");
		credReq.setEncrypt(Boolean.FALSE);
		credReq.setSharableAttributes(attributes);
		credReq.setAdditionalData(additionalData);

		policyResponse = new PartnerCredentialTypePolicyDto();
		List<AllowedKycDto> shareableAttributes = new ArrayList<>();
		AllowedKycDto kyc1 = new AllowedKycDto();
		kyc1.setAttributeName("fullName");
		kyc1.setEncrypted(true);
		kyc1.setFormat(CredentialConstants.NAME);

		List<Source> sourceList = new ArrayList<>();

		Source source1 = new Source();
		source1.setAttribute(CredentialConstants.VID);
		List<Filter> filterList = new ArrayList<>();
		Filter filter = new Filter();
		filter.setLanguage("en");
		filter.setType("Domain");
		filterList.add(filter);
		source1.setFilter(filterList);

		sourceList.add(source1);
		kyc1.setSource(sourceList);
		shareableAttributes.add(kyc1);
		AllowedKycDto kyc2 = new AllowedKycDto();
		kyc2.setAttributeName("dateOfBirth");
		kyc2.setEncrypted(true);

		List<Source> sourceList2 = new ArrayList<>();
		Source source2 = new Source();
		source2.setAttribute("Test1");
		sourceList2.add(source2);
		kyc2.setSource(sourceList2);
		shareableAttributes.add(kyc2);
		AllowedKycDto kyc3 = new AllowedKycDto();
		kyc3.setAttributeName("biometrics");
		kyc3.setGroup("CBEFF");
		kyc3.setEncrypted(true);
		List<Source> sourceList3 = new ArrayList<>();
		Source source3 = new Source();
		source3.setAttribute("Test2");
		sourceList3.add(source3);
		kyc3.setSource(sourceList3);
		shareableAttributes.add(kyc3);
		AllowedKycDto kyc4 = new AllowedKycDto();
		List<Source> sourceList4 = new ArrayList<>();
		Source source4 = new Source();
		source4.setAttribute("APEX");
		List<Filter> filterList1 = new ArrayList<>();
		Filter filter1 = new Filter();
		filter1.setLanguage("en");
		filter1.setType("Face");
		filterList1.add(filter1);
		source4.setFilter(filterList1);
		sourceList4.add(source4);
		kyc4.setSource(sourceList4);
		kyc4.setAttributeName("email");
		kyc4.setEncrypted(true);
		kyc4.setGroup(CredentialConstants.CBEFF);
		kyc4.setFormat(CredentialConstants.BESTTWOFINGERS);
		shareableAttributes.add(kyc4);
		PolicyAttributesDto dto = new PolicyAttributesDto();
		dto.setShareableAttributes(shareableAttributes);
		policyResponse.setPolicies(dto);

		VidResponseDTO vidResponseDTO = mock(VidResponseDTO.class);
		Mockito.when(vidUtil.generateVID(Mockito.any(), Mockito.any()))
				.thenReturn(vidResponseDTO);

		VidInfoDTO vidInfoDTO = new VidInfoDTO();
		vidInfoDTO.setExpiryTimestamp(DateUtils2.getUTCCurrentDateTime());
		Mockito.when(vidUtil.getVIDData(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(vidInfoDTO);

		List<BIR> birList = new ArrayList<>();
		BIR bir = new BIR();
		BDBInfo bdbInfoFace = new BDBInfo();
		List<BiometricType> singleFaceList = new ArrayList<>();
		singleFaceList.add(BiometricType.FACE);
		bdbInfoFace.setType(singleFaceList);
		bdbInfoFace.setSubtype(List.of("ref"));
		QualityType qualityType = new QualityType();
		qualityType.setScore(4L);
		bdbInfoFace.setQuality(qualityType);
		bir.setBdbInfo(bdbInfoFace);
		birList.add(bir);
		BIR birFinger = new BIR();
		BDBInfo bdbInfoRightThumb = new BDBInfo();
		QualityType bdbInfoFingerQuality = new QualityType();
		bdbInfoFingerQuality.setScore(60L);
		List<BiometricType> singleFingerList = new ArrayList<>();
		singleFingerList.add(BiometricType.FINGER);
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
		Mockito.when(cbeffutil.getBIRDataFromXML(Mockito.any())).thenReturn(birList);

		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse, policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}

	@Test
	public void prepareSharableAttributes_WithPolicyAndBiometrics_ReturnSharableAttributesMap() throws Exception {
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

		identityMap.put("UIN", 3000);

		Object identity = identityMap;
		response.setIdentity(identity);

		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("APEX");

		doc1.setValue("textbiomterics");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);

		response.setDocuments(docList);
		idResponse.setResponse(response);
		CredentialServiceRequestDto credentialServiceRequestDto = getCredentialServiceRequestDto();

		CredentialServiceRequestDto credReq = new CredentialServiceRequestDto();
		List<String> attributes = new ArrayList<String>();

		attributes.add("abc");

		List<String> maskingAttributesList = new ArrayList<String>();
		maskingAttributesList.add("phone");
		maskingAttributesList.add("email");
		maskingAttributesList.add("uin");
		maskingAttributesList.add("vid");

		Map<String,Object> attributeFormat= new HashMap<String,Object>();

		attributeFormat.put("dateOfBirth", "DD/MMM/YYYY");
		attributeFormat.put("fullAddress","");
		attributeFormat.put("name","");
		attributeFormat.put("fullName","");

		Map<String,Object>additionalData=new HashMap<String,Object>();

		additionalData.put("formatingAttributes",attributeFormat);
		additionalData.put("maskingAttributes",maskingAttributesList);

		credReq.setId("2361485607");
		credReq.setCredentialType("euin");
		credReq.setIssuer("mpartner-default-print");
		credReq.setEncryptionKey("JQ5sLK6Sq11SzUZq");
		credReq.setEncrypt(Boolean.FALSE);
		credReq.setSharableAttributes(attributes);
		credReq.setAdditionalData(additionalData);

		policyResponse = new PartnerCredentialTypePolicyDto();
		List<AllowedKycDto> shareableAttributes = new ArrayList<>();
		AllowedKycDto kyc1 = new AllowedKycDto();
		kyc1.setAttributeName("fullName");
		kyc1.setEncrypted(true);
		kyc1.setFormat(CredentialConstants.NAME);

		List<Source> sourceList = new ArrayList<>();

		Source source1 = new Source();
		source1.setAttribute(CredentialConstants.VID);
		List<Filter> filterList = new ArrayList<>();
		Filter filter = new Filter();
		filter.setLanguage("en");
		filter.setType("Domain");
		filterList.add(filter);
		source1.setFilter(filterList);

		sourceList.add(source1);
		kyc1.setSource(sourceList);
		shareableAttributes.add(kyc1);
		AllowedKycDto kyc2 = new AllowedKycDto();
		kyc2.setAttributeName("dateOfBirth");
		kyc2.setEncrypted(true);

		List<Source> sourceList2 = new ArrayList<>();
		Source source2 = new Source();
		source2.setAttribute("Test1");
		sourceList2.add(source2);
		kyc2.setSource(sourceList2);
		shareableAttributes.add(kyc2);
		AllowedKycDto kyc3 = new AllowedKycDto();
		kyc3.setAttributeName("biometrics");
		kyc3.setGroup("CBEFF");
		kyc3.setEncrypted(true);
		List<Source> sourceList3 = new ArrayList<>();
		Source source3 = new Source();
		source3.setAttribute("Test2");
		sourceList3.add(source3);
		kyc3.setSource(sourceList3);
		shareableAttributes.add(kyc3);
		AllowedKycDto kyc4 = new AllowedKycDto();
		List<Source> sourceList4 = new ArrayList<>();
		Source source4 = new Source();
		source4.setAttribute("APEX");
		List<Filter> filterList1 = new ArrayList<>();
		Filter filter1 = new Filter();
		filter1.setLanguage("en");
		filter1.setType("Face");
		filter1.setSubType(List.of("ref"));
		filterList1.add(filter1);
		source4.setFilter(filterList1);
		sourceList4.add(source4);
		kyc4.setSource(sourceList4);
		kyc4.setAttributeName("email");
		kyc4.setEncrypted(true);
		kyc4.setGroup(CredentialConstants.CBEFF);
		kyc4.setFormat(CredentialConstants.BESTTWOFINGERS);
		shareableAttributes.add(kyc4);
		PolicyAttributesDto dto = new PolicyAttributesDto();
		dto.setShareableAttributes(shareableAttributes);
		policyResponse.setPolicies(dto);

		VidResponseDTO vidResponseDTO = mock(VidResponseDTO.class);
		Mockito.when(vidUtil.generateVID(Mockito.any(), Mockito.any()))
				.thenReturn(vidResponseDTO);

		VidInfoDTO vidInfoDTO = new VidInfoDTO();
		vidInfoDTO.setExpiryTimestamp(DateUtils2.getUTCCurrentDateTime());
		Mockito.when(vidUtil.getVIDData(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(vidInfoDTO);

		List<BIR> birList = new ArrayList<>();
		BIR bir = new BIR();
		BDBInfo bdbInfoFace = new BDBInfo();
		List<BiometricType> singleFaceList = new ArrayList<>();
		singleFaceList.add(BiometricType.FACE);
		bdbInfoFace.setType(singleFaceList);
		bdbInfoFace.setSubtype(List.of("ref"));
		QualityType qualityType = new QualityType();
		qualityType.setScore(4L);
		bdbInfoFace.setQuality(qualityType);
		bir.setBdbInfo(bdbInfoFace);
		birList.add(bir);
		BIR birFinger = new BIR();
		BDBInfo bdbInfoRightThumb = new BDBInfo();
		QualityType bdbInfoFingerQuality = new QualityType();
		bdbInfoFingerQuality.setScore(60L);
		List<BiometricType> singleFingerList = new ArrayList<>();
		singleFingerList.add(BiometricType.FINGER);
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
		Mockito.when(cbeffutil.getBIRDataFromXML(Mockito.any())).thenReturn(birList);

		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse, policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}

	@Test
	public void prepareSharableAttributes_WithSubTypeAndScore_ReturnSharableAttributesMap() throws Exception {
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

		identityMap.put("UIN", 3000);

		Object identity = identityMap;
		response.setIdentity(identity);

		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("APEX");

		doc1.setValue("textbiomterics");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);

		response.setDocuments(docList);
		idResponse.setResponse(response);
		CredentialServiceRequestDto credentialServiceRequestDto = getCredentialServiceRequestDto();

		CredentialServiceRequestDto credReq = new CredentialServiceRequestDto();
		List<String> attributes = new ArrayList<String>();

		attributes.add("abc");

		List<String> maskingAttributesList = new ArrayList<String>();
		maskingAttributesList.add("phone");
		maskingAttributesList.add("email");
		maskingAttributesList.add("uin");
		maskingAttributesList.add("vid");

		Map<String,Object> attributeFormat= new HashMap<String,Object>();

		attributeFormat.put("dateOfBirth", "DD/MMM/YYYY");
		attributeFormat.put("fullAddress","");
		attributeFormat.put("name","");
		attributeFormat.put("fullName","");

		Map<String,Object>additionalData=new HashMap<String,Object>();

		additionalData.put("formatingAttributes",attributeFormat);
		additionalData.put("maskingAttributes",maskingAttributesList);

		credReq.setId("2361485607");
		credReq.setCredentialType("euin");
		credReq.setIssuer("mpartner-default-print");
		credReq.setEncryptionKey("JQ5sLK6Sq11SzUZq");
		credReq.setEncrypt(Boolean.FALSE);
		credReq.setSharableAttributes(attributes);
		credReq.setAdditionalData(additionalData);

		policyResponse = new PartnerCredentialTypePolicyDto();
		List<AllowedKycDto> shareableAttributes = new ArrayList<>();
		AllowedKycDto kyc1 = new AllowedKycDto();
		kyc1.setAttributeName("fullName");
		kyc1.setEncrypted(true);
		kyc1.setFormat(CredentialConstants.NAME);

		List<Source> sourceList = new ArrayList<>();

		Source source1 = new Source();
		source1.setAttribute(CredentialConstants.VID);
		List<Filter> filterList = new ArrayList<>();
		Filter filter = new Filter();
		filter.setLanguage("en");
		filter.setType("Domain");
		filterList.add(filter);
		source1.setFilter(filterList);

		sourceList.add(source1);
		kyc1.setSource(sourceList);
		shareableAttributes.add(kyc1);
		AllowedKycDto kyc2 = new AllowedKycDto();
		kyc2.setAttributeName("dateOfBirth");
		kyc2.setEncrypted(true);

		List<Source> sourceList2 = new ArrayList<>();
		Source source2 = new Source();
		source2.setAttribute("Test1");
		sourceList2.add(source2);
		kyc2.setSource(sourceList2);
		shareableAttributes.add(kyc2);
		AllowedKycDto kyc3 = new AllowedKycDto();
		kyc3.setAttributeName("biometrics");
		kyc3.setGroup("CBEFF");
		kyc3.setEncrypted(true);
		List<Source> sourceList3 = new ArrayList<>();
		Source source3 = new Source();
		source3.setAttribute("Test2");
		sourceList3.add(source3);
		kyc3.setSource(sourceList3);
		shareableAttributes.add(kyc3);
		AllowedKycDto kyc4 = new AllowedKycDto();
		List<Source> sourceList4 = new ArrayList<>();
		Source source4 = new Source();
		source4.setAttribute("APEX");
		List<Filter> filterList1 = new ArrayList<>();
		Filter filter1 = new Filter();
		filter1.setLanguage("en");
		filter1.setType("Face");
		filter1.setSubType(List.of("ref", "demo"));
		filterList1.add(filter1);
		Filter filter2 = new Filter();
		filter2.setLanguage("en");
		filter2.setType("Finger");
		filter2.setSubType(List.of("Right Thumb"));
		filterList1.add(filter2);
		source4.setFilter(filterList1);
		sourceList4.add(source4);
		kyc4.setSource(sourceList4);
		kyc4.setAttributeName("email");
		kyc4.setEncrypted(true);
		kyc4.setGroup(CredentialConstants.CBEFF);
		kyc4.setFormat(CredentialConstants.BESTTWOFINGERS);
		shareableAttributes.add(kyc4);
		PolicyAttributesDto dto = new PolicyAttributesDto();
		dto.setShareableAttributes(shareableAttributes);
		policyResponse.setPolicies(dto);

		VidResponseDTO vidResponseDTO = mock(VidResponseDTO.class);
		Mockito.when(vidUtil.generateVID(Mockito.any(), Mockito.any()))
				.thenReturn(vidResponseDTO);

		VidInfoDTO vidInfoDTO = new VidInfoDTO();
		vidInfoDTO.setExpiryTimestamp(DateUtils2.getUTCCurrentDateTime());
		Mockito.when(vidUtil.getVIDData(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(vidInfoDTO);

		List<BIR> birList = new ArrayList<>();
		BIR bir = new BIR();
		BDBInfo bdbInfoFace = new BDBInfo();
		List<BiometricType> singleFaceList = new ArrayList<>();
		singleFaceList.add(BiometricType.FACE);
		bdbInfoFace.setType(singleFaceList);
		bdbInfoFace.setSubtype(List.of("ref"));
		QualityType qualityType = new QualityType();
		qualityType.setScore(4L);
		bdbInfoFace.setQuality(qualityType);
		bir.setBdbInfo(bdbInfoFace);
		birList.add(bir);

		BIR birFinger = new BIR();
		BDBInfo bdbInfoRightThumb = new BDBInfo();
		QualityType bdbInfoFingerQuality = new QualityType();
		bdbInfoFingerQuality.setScore(60L);
		List<BiometricType> singleFingerList = new ArrayList<>();
		singleFingerList.add(BiometricType.FINGER);
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
		Mockito.when(cbeffutil.getBIRDataFromXML(Mockito.any())).thenReturn(birList);

		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse, policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 2);
	}

	@Test
	public void prepareSharableAttributes_WithBiometricFilter_ReturnSharableAttributesMap() throws Exception {
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

		doc1.setValue("textbiomterics");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);

		response.setDocuments(docList);
		idResponse.setResponse(response);
		CredentialServiceRequestDto credentialServiceRequestDto = getCredentialServiceRequestDto();
		List<String> sharableAttributesList = Collections.EMPTY_LIST;
		credentialServiceRequestDto.setSharableAttributes(sharableAttributesList);

		policyResponse = new PartnerCredentialTypePolicyDto();
		List<AllowedKycDto> shareableAttributes = new ArrayList<>();
		AllowedKycDto kyc1 = new AllowedKycDto();
		kyc1.setAttributeName("fullName");
		kyc1.setEncrypted(true);

		List<Source> sourceList = new ArrayList<>();

		Source source1 = new Source();
		source1.setAttribute("fullName");

		sourceList.add(source1);
		kyc1.setSource(sourceList);
		shareableAttributes.add(kyc1);
		AllowedKycDto kyc2 = new AllowedKycDto();
		kyc2.setAttributeName("dateOfBirth");
		kyc2.setEncrypted(true);

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
		Source source3 = new Source();
		source3.setAttribute("individualBiometrics");
		Filter filter = new Filter();
		filter.setType("Face");
		filter.setSubType(List.of("ref refData"));
		source3.setFilter(List.of(filter));
		sourceList3.add(source3);
		kyc3.setSource(sourceList3);
		shareableAttributes.add(kyc3);
		AllowedKycDto kyc4 = new AllowedKycDto();
		List<Source> sourceList4 = new ArrayList<>();
		Source source4 = new Source();
		source4.setAttribute("email");
		sourceList4.add(source4);
		kyc4.setSource(sourceList4);
		kyc4.setAttributeName("email");
		kyc4.setEncrypted(true);
		kyc4.setGroup(CredentialConstants.CBEFF);
		shareableAttributes.add(kyc4);
		PolicyAttributesDto dto = new PolicyAttributesDto();
		dto.setShareableAttributes(shareableAttributes);
		policyResponse.setPolicies(dto);

		List<BIR> birList = new ArrayList<>();
		BIR bir = new BIR();
		BDBInfo bdbInfoFace = new BDBInfo();
		List<BiometricType> singleFaceList = new ArrayList<>();
		singleFaceList.add(BiometricType.FACE);
		bdbInfoFace.setType(singleFaceList);
		bdbInfoFace.setSubtype(List.of("ref", "refData"));
		QualityType qualityType = new QualityType();
		qualityType.setScore(4L);
		bdbInfoFace.setQuality(qualityType);
		bir.setBdbInfo(bdbInfoFace);
		byte[] bioBytes = "textbiomterics".getBytes();
		bir.setBdb(bioBytes);
		birList.add(bir);
		BIR birFinger = new BIR();
		BDBInfo bdbInfoRightThumb = new BDBInfo();
		QualityType bdbInfoFingerQuality = new QualityType();
		bdbInfoFingerQuality.setScore(60L);
		List<BiometricType> singleFingerList = new ArrayList<>();
		singleFingerList.add(BiometricType.FINGER);
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
		Mockito.when(cbeffutil.getBIRDataFromXML(Mockito.any())).thenReturn(birList);

		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse, policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}

	@Test
	public void prepareSharableAttributes_HandleIdentityAttribute_ReturnSharableAttributesMap() throws Exception {
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
		identityMap.put("fullName", "John");

		identityMap.put("dateOfBirth", "1980/11/14");

		Object identity = identityMap;
		response.setIdentity(identity);

		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("individualBiometrics");

		doc1.setValue("textbiomterics");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);

		response.setDocuments(docList);
		idResponse.setResponse(response);
		CredentialServiceRequestDto credentialServiceRequestDto = getCredentialServiceRequestDto();
		List<String> sharableAttributesList = Collections.EMPTY_LIST;
		credentialServiceRequestDto.setSharableAttributes(sharableAttributesList);

		policyResponse = new PartnerCredentialTypePolicyDto();
		List<AllowedKycDto> shareableAttributes = new ArrayList<>();
		AllowedKycDto kyc1 = new AllowedKycDto();
		kyc1.setAttributeName("fullName");
		kyc1.setEncrypted(true);

		List<Source> sourceList = new ArrayList<>();

		Source source1 = new Source();
		source1.setAttribute("abc");

		sourceList.add(source1);
		kyc1.setSource(sourceList);
		shareableAttributes.add(kyc1);
		AllowedKycDto kyc2 = new AllowedKycDto();
		kyc2.setAttributeName("dateOfBirth");
		kyc2.setEncrypted(true);

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
		Source source3 = new Source();
		source3.setAttribute("individualBiometrics");
		Filter filter = new Filter();
		filter.setType("Face");
		filter.setSubType(List.of("ref refData"));
		source3.setFilter(List.of(filter));
		sourceList3.add(source3);
		kyc3.setSource(sourceList3);
		shareableAttributes.add(kyc3);
		AllowedKycDto kyc4 = new AllowedKycDto();
		List<Source> sourceList4 = new ArrayList<>();
		Source source4 = new Source();
		source4.setAttribute("email");
		sourceList4.add(source4);
		kyc4.setSource(sourceList4);
		kyc4.setAttributeName("email");
		kyc4.setEncrypted(true);
		kyc4.setGroup(CredentialConstants.CBEFF);
		shareableAttributes.add(kyc4);
		PolicyAttributesDto dto = new PolicyAttributesDto();
		dto.setShareableAttributes(shareableAttributes);
		policyResponse.setPolicies(dto);

		List<BIR> birList = new ArrayList<>();
		BIR bir = new BIR();
		BDBInfo bdbInfoFace = new BDBInfo();
		List<BiometricType> singleFaceList = new ArrayList<>();
		singleFaceList.add(BiometricType.FACE);
		bdbInfoFace.setType(singleFaceList);
		bdbInfoFace.setSubtype(List.of("ref", "refData"));
		QualityType qualityType = new QualityType();
		qualityType.setScore(4L);
		bdbInfoFace.setQuality(qualityType);
		bir.setBdbInfo(bdbInfoFace);
		byte[] bioBytes = "textbiomterics".getBytes();
		bir.setBdb(bioBytes);
		birList.add(bir);
		BIR birFinger = new BIR();
		BDBInfo bdbInfoRightThumb = new BDBInfo();
		QualityType bdbInfoFingerQuality = new QualityType();
		bdbInfoFingerQuality.setScore(60L);
		List<BiometricType> singleFingerList = new ArrayList<>();
		singleFingerList.add(BiometricType.FINGER);
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
		Mockito.when(cbeffutil.getBIRDataFromXML(Mockito.any())).thenReturn(birList);

		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse, policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}

	@Test
	public void prepareSharableAttributes_ReturnValidMap_WithMaskApplied() throws Exception {

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
		identityMap.put("abc", "John");

		identityMap.put("dateOfBirth", "1980/11/14");

		Object identity = identityMap;
		response.setIdentity(identity);

		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("individualBiometrics");

		doc1.setValue("textbiomterics");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);

		response.setDocuments(docList);
		idResponse.setResponse(response);
		CredentialServiceRequestDto credReq = new CredentialServiceRequestDto();
		List<String> attributes = new ArrayList<String>();
		attributes.add("sample");

		List<String> maskingAttributesList = new ArrayList<String>();
		maskingAttributesList.add("abc");

		Map<String,Object> attributeFormat= new HashMap<String,Object>();
		attributeFormat.put("fullAddress","");
		Map<String,Object>additionalData=new HashMap<String,Object>();

		additionalData.put("formatingAttributes",attributeFormat);
		additionalData.put("maskingAttributes",maskingAttributesList);

		credReq.setId("2361485607");
		credReq.setCredentialType("euin");
		credReq.setIssuer("mpartner-default-print");
		credReq.setEncryptionKey("JQ5sLK6Sq11SzUZq");
		credReq.setEncrypt(Boolean.FALSE);
		credReq.setSharableAttributes(attributes);
		credReq.setAdditionalData(additionalData);

		List<String> sharableAttributesList = Collections.EMPTY_LIST;
		credReq.setSharableAttributes(sharableAttributesList);

		policyResponse = new PartnerCredentialTypePolicyDto();
		List<AllowedKycDto> shareableAttributes = new ArrayList<>();
		AllowedKycDto kyc1 = new AllowedKycDto();
		kyc1.setAttributeName("fullName");
		kyc1.setEncrypted(true);

		List<Source> sourceList = new ArrayList<>();

		Source source1 = new Source();
		source1.setAttribute("abc");

		sourceList.add(source1);
		kyc1.setSource(sourceList);
		shareableAttributes.add(kyc1);
		AllowedKycDto kyc2 = new AllowedKycDto();
		kyc2.setAttributeName("dateOfBirth");
		kyc2.setEncrypted(true);

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
		Source source3 = new Source();
		source3.setAttribute("individualBiometrics");
		Filter filter = new Filter();
		filter.setType("Face");
		filter.setSubType(List.of("ref refData"));
		source3.setFilter(List.of(filter));
		sourceList3.add(source3);
		kyc3.setSource(sourceList3);
		shareableAttributes.add(kyc3);
		AllowedKycDto kyc4 = new AllowedKycDto();
		List<Source> sourceList4 = new ArrayList<>();
		Source source4 = new Source();
		source4.setAttribute("email");
		sourceList4.add(source4);
		kyc4.setSource(sourceList4);
		kyc4.setAttributeName("email");
		kyc4.setEncrypted(true);
		kyc4.setGroup(CredentialConstants.CBEFF);
		shareableAttributes.add(kyc4);
		PolicyAttributesDto dto = new PolicyAttributesDto();
		dto.setShareableAttributes(shareableAttributes);
		policyResponse.setPolicies(dto);

		List<BIR> birList = new ArrayList<>();
		BIR bir = new BIR();
		BDBInfo bdbInfoFace = new BDBInfo();
		List<BiometricType> singleFaceList = new ArrayList<>();
		singleFaceList.add(BiometricType.FACE);
		bdbInfoFace.setType(singleFaceList);
		bdbInfoFace.setSubtype(List.of("ref", "refData"));
		QualityType qualityType = new QualityType();
		qualityType.setScore(4L);
		bdbInfoFace.setQuality(qualityType);
		bir.setBdbInfo(bdbInfoFace);
		byte[] bioBytes = "textbiomterics".getBytes();
		bir.setBdb(bioBytes);
		birList.add(bir);
		BIR birFinger = new BIR();
		BDBInfo bdbInfoRightThumb = new BDBInfo();
		QualityType bdbInfoFingerQuality = new QualityType();
		bdbInfoFingerQuality.setScore(60L);
		List<BiometricType> singleFingerList = new ArrayList<>();
		singleFingerList.add(BiometricType.FINGER);
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
		Mockito.when(cbeffutil.getBIRDataFromXML(Mockito.any())).thenReturn(birList);

		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse, policyResponse, credReq);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}
	
	@Test
	public void testPrepareSharableAttributesEmptyListCheck() throws CredentialFormatterException {
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
		CredentialServiceRequestDto credentialServiceRequestDto = getCredentialServiceRequestDto();
		List<String> sharableAttributesList = Collections.EMPTY_LIST;
		credentialServiceRequestDto.setSharableAttributes(sharableAttributesList);
		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse, policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}
	
	@Test
	public void testPrepareMaskingAndFormattingEmptyListCheck() throws CredentialFormatterException {
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
		CredentialServiceRequestDto credentialServiceRequestDto = getCredentialServiceRequestDto();
		List<String> sharableAttributesList = Collections.EMPTY_LIST;
		List<String> maskingAttributes = Collections.EMPTY_LIST;
		Map<String,String> formatingAttributes = Collections.EMPTY_MAP;
		Map<String,Object> additionalAttributes = new HashMap<String,Object>();
		additionalAttributes.put("maskingAttributes", maskingAttributes);
		additionalAttributes.put("formatingAttributes", formatingAttributes);
		
		credentialServiceRequestDto.setSharableAttributes(sharableAttributesList);
		credentialServiceRequestDto.setAdditionalData(additionalAttributes);
		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse, policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}
	
	@Test
	public void testPrepareMaskingAndFormattingNullCheck() throws CredentialFormatterException {
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
		CredentialServiceRequestDto credentialServiceRequestDto = getCredentialServiceRequestDto();
		List<String> sharableAttributesList = Collections.EMPTY_LIST;
		List<String> maskingAttributes = null;
		List<String> formatingAttributes = null;
		
		Map<String,Object> additionalAttributes = new HashMap<String,Object>();
		additionalAttributes.put("maskingAttributes", maskingAttributes);
		additionalAttributes.put("formatingAttributes", formatingAttributes);
		
		credentialServiceRequestDto.setSharableAttributes(sharableAttributesList);
		credentialServiceRequestDto.setAdditionalData(additionalAttributes);
		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse, policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}
	
	@Test
	public void testPrepareSharableAttributesNULLCheck() throws CredentialFormatterException {
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
		CredentialServiceRequestDto credentialServiceRequestDto = getCredentialServiceRequestDto();
		List<String> sharableAttributesList = null;
		credentialServiceRequestDto.setSharableAttributes(sharableAttributesList);
		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse, policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}

	
	@Test
	public void testPrepareSharableAttributesSuccessWithUserRequestedAttributes() throws CredentialFormatterException {
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
		CredentialServiceRequestDto credentialServiceRequestDto = getCredentialServiceRequestDto();
		List<String> sharableAttributesList = new ArrayList<>();
		sharableAttributesList.add("fullName");
		credentialServiceRequestDto.setSharableAttributes(sharableAttributesList);
		Map<AllowedKycDto, Object> sharabaleAttrubutesMap = credentialDefaultProvider
				.prepareSharableAttributes(idResponse, policyResponse, credentialServiceRequestDto);
		assertTrue("preparedsharableattribute smap", sharabaleAttrubutesMap.size() >= 1);
	}
	
private CredentialServiceRequestDto getCredentialServiceRequestDto() {
		
		CredentialServiceRequestDto credReq = new CredentialServiceRequestDto();
		List<String> attributes = new ArrayList<String>();
		
		attributes.add("fullName");
		attributes.add("middleName");
		attributes.add("lastName");
		attributes.add("phone");
		attributes.add("email");
		attributes.add("UIN");
		attributes.add("fullAddress");
		attributes.add("dob");
		attributes.add("vid");

		List<String> maskingAttributesList = new ArrayList<String>();
		maskingAttributesList.add("phone");
		maskingAttributesList.add("email");
		maskingAttributesList.add("uin");
		maskingAttributesList.add("vid");

		Map<String,Object> attributeFormat= new HashMap<String,Object>();
		
		attributeFormat.put("dateOfBirth", "DD/MMM/YYYY");
		attributeFormat.put("fullAddress","");
		attributeFormat.put("name","");
		attributeFormat.put("fullName","");
		
		Map<String,Object>additionalData=new HashMap<String,Object>();		
		
		additionalData.put("formatingAttributes",attributeFormat);
		additionalData.put("maskingAttributes",maskingAttributesList);
		
		credReq.setId("2361485607");
		credReq.setCredentialType("euin");
		credReq.setIssuer("mpartner-default-print");
		credReq.setEncryptionKey("JQ5sLK6Sq11SzUZq");
		credReq.setEncrypt(Boolean.FALSE);
		credReq.setSharableAttributes(attributes);
		credReq.setAdditionalData(additionalData);
		
		return credReq;
	}

}