package io.mosip.credentialstore.test.provider.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.dto.AllowedKycDto;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.PartnerCredentialTypePolicyDto;
import io.mosip.credentialstore.dto.PolicyAttributesDto;
import io.mosip.credentialstore.dto.Source;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataEncryptionFailureException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.IdentityMapping;
import io.mosip.idrepository.core.dto.ResponseDTO;
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
	
	@InjectMocks
	private CredentialProvider credentialDefaultProvider;
	
	
	/** The id response. */
	private IdResponseDTO idResponse = new IdResponseDTO();

	/** The response. */
	private ResponseDTO response = new ResponseDTO();

	PartnerCredentialTypePolicyDto policyResponse;
	
	IdentityMapping identityMapping;
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Before
	public void setUp() throws DataEncryptionFailureException, ApiNotAccessibleException, SignatureException,Exception {
		
		ReflectionTestUtils.setField(credentialDefaultProvider, "mapper", mapper);
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
		shareableAttributes.add(kyc4);		
		PolicyAttributesDto dto = new PolicyAttributesDto();
		dto.setShareableAttributes(shareableAttributes);
		policyResponse.setPolicies(dto);
		
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