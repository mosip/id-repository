package io.mosip.idrepository.identity.test.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.FACE_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.FINGER_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IRIS_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MOSIP_KERNEL_IDREPO_JSON_PATH;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.DatatypeConverter;

import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.hibernate.exception.JDBCConnectionException;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionException;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.InvalidJsonException;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.RequestDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.repository.CredentialRequestStatusRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.BiometricExtractionService;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.entity.Uin;
import io.mosip.idrepository.identity.entity.UinBiometric;
import io.mosip.idrepository.identity.entity.UinBiometricDraft;
import io.mosip.idrepository.identity.entity.UinDocumentDraft;
import io.mosip.idrepository.identity.entity.UinDraft;
import io.mosip.idrepository.identity.helper.AnonymousProfileHelper;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;
import io.mosip.idrepository.identity.helper.VidDraftHelper;
import io.mosip.idrepository.identity.repository.UinBiometricHistoryRepo;
import io.mosip.idrepository.identity.repository.UinBiometricRepo;
import io.mosip.idrepository.identity.repository.UinDocumentHistoryRepo;
import io.mosip.idrepository.identity.repository.UinDocumentRepo;
import io.mosip.idrepository.identity.repository.UinDraftRepo;
import io.mosip.idrepository.identity.repository.UinHistoryRepo;
import io.mosip.idrepository.identity.repository.UinRepo;
import io.mosip.idrepository.identity.service.impl.DefaultShardResolver;
import io.mosip.idrepository.identity.service.impl.IdRepoDraftServiceImpl;
import io.mosip.idrepository.identity.service.impl.IdRepoProxyServiceImpl;
import io.mosip.idrepository.identity.service.impl.IdRepoServiceImpl;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.kernel.cbeffutil.impl.CbeffImpl;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.CryptoUtil;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ActiveProfiles("test")
@ConfigurationProperties("mosip.idrepo.identity")
public class IdRepoDraftServiceImplTest {
	@InjectMocks
	CbeffImpl cbeffUtil;

	@Mock
	AuditHelper auditHelper;

	@Mock
	ObjectStoreAdapter connection;

	@Mock
	private IdRequestValidator validator;

	@Autowired
	public ObjectMapper mapper;

	@Mock
	private UinBiometricRepo uinBiometricRepo;

	@Mock
	private UinDocumentRepo uinDocumentRepo;
	
	@Mock
	private VidDraftHelper vidDraftHelper;

	@InjectMocks
	IdRepoServiceImpl service;
	
	@Mock
	private IdRepoProxyServiceImpl proxyService;

	@Mock
	IdRepoSecurityManager securityManager;

	@Mock
	private UinBiometricHistoryRepo uinBioHRepo;

	@Mock
	private UinDocumentHistoryRepo uinDocHRepo;

	/** The env. */
	@Autowired
	private EnvUtil envUtil;
	
	@Autowired
	Environment env;

	/** The rest template. */
	@Mock
	private RestHelper restHelper;

	@Mock
	private DefaultShardResolver shardResolver;

	/** The uin repo. */
	@Mock
	private UinRepo uinRepo;

	@Mock
	private UinDraftRepo uinDraftRepo;

	/** The uin history repo. */
	@Mock
	private UinHistoryRepo uinHistoryRepo;

	@Mock
	RestRequestBuilder restBuilder;

	@Mock
	private UinHashSaltRepo uinHashSaltRepo;

	@Mock
	private UinEncryptSaltRepo uinEncryptSaltRepo;

	@Mock
	private CredentialRequestStatusRepo credRequestRepo;

	@Mock
	private ObjectStoreHelper objectStoreHelper;

	@Mock
	private AnonymousProfileHelper anonymousProfileHelper;

	@Mock
	private DummyPartnerCheckUtil dummyPartner;
	
	@Mock
	private BiometricExtractionService biometricExtractionService;
	
	@InjectMocks
	IdRepoDraftServiceImpl idRepoServiceImpl;

	/** The id. */
	private Map<String, String> id;
	
	private static final String uinPath = "identity.UIN";
	
	@Before
	public void setup() throws IdRepoDataValidationException {
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinPath", uinPath);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "restBuilder", restBuilder);
		ReflectionTestUtils.setField(idRepoServiceImpl, "restHelper", restHelper);
		ReflectionTestUtils.setField(restBuilder, "env", env);
		ReflectionTestUtils.setField(idRepoServiceImpl, "proxyService", proxyService);
		ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "validator", validator);
		ReflectionTestUtils.setField(idRepoServiceImpl, "objectStoreHelper", objectStoreHelper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "cbeffUtil", cbeffUtil);
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinEncryptSaltRepo", uinEncryptSaltRepo);
		ReflectionTestUtils.setField(idRepoServiceImpl, "bioAttributes",
				Lists.newArrayList("individualBiometrics", "parentOrGuardianBiometrics"));
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		
	}
	
	@Test
	public void testHasDraft() throws IdRepoAppException {
		boolean flag = idRepoServiceImpl.hasDraft("qsdggtresxcv");
		assertFalse(flag);
	}

	@Ignore//java.lang.NumberFormatException: For input string: "2419762130"
	@Test
	public void testCreateDraft() throws IdRepoAppException, IOException, NoSuchAlgorithmException {
		EnvUtil.setIdrepoSaltKeyLength(12);
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		when(uinHistoryRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		Uin uin = new Uin();
		String identityData = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-data.json"),
				StandardCharsets.UTF_8);
		uin.setUin("2419762130");
//		String uinHash = securityManager.hash();
		String uinHash =  DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		uin.setUinHash("123_"+uinHash);
		uin.setRegId("1234567890");
		uin.setUinData(identityData.getBytes());
		uin.setUinDataHash(DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase());
		Optional<Uin> uinOpt = Optional.of(uin);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(uinOpt);
		IdResponseDTO idresponse = idRepoServiceImpl.createDraft("1234567890","2419762130");
		assertNull(idresponse);
	}
	
	@Test(expected =IdRepoAppException.class)
	public void testCreateDraftWithException() throws IdRepoAppException {
		EnvUtil.setIdrepoSaltKeyLength(12);
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		when(uinHistoryRepo.existsByRegId(Mockito.any())).thenReturn(true);
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(true);
		IdResponseDTO idresponse = idRepoServiceImpl.createDraft("1234567890","274390482564");
		assertNull(idresponse);
	}
	
	@Test
	@Ignore//UndeclaredThrowable
	public void testCreateDraftwithEMptyUin() throws IdRepoAppException {
		EnvUtil.setIdrepoSaltKeyLength(12);
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		when(uinHistoryRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(securityManager.getSaltKeyForId(Mockito.any())).thenReturn(123);
		IdResponseDTO idresponse = idRepoServiceImpl.createDraft("1234567890","");
		assertNull(idresponse);
	}
	

	@Test
	public void testGenerateIdentityObject() {
		Object uin1 = "274390482564";
		Object uin = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "generateIdentityObject",uin1 );
		assertNotNull(uin);
	}
	
//	@Ignore//UndeclaredThrowable
	@Test
	public void testGenerateUin() throws IdRepoDataValidationException, JsonMappingException, RestServiceException, JsonProcessingException {
		ReflectionTestUtils.setField(idRepoServiceImpl, "restBuilder", restBuilder);
		ReflectionTestUtils.setField(idRepoServiceImpl, "restHelper", restHelper);
		ReflectionTestUtils.setField(restBuilder, "env", env);
		ResponseWrapper<ObjectNode> response = new ResponseWrapper<>();
		ObjectNode responseNode = mapper.createObjectNode();
		responseNode.put("data", CryptoUtil.encodeToURLSafeBase64("data".getBytes()));
		response.setResponse(responseNode);
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(Mockito.any()))
				.thenReturn(mapper.readValue(mapper.writeValueAsString(response), ObjectNode.class));
		String uin = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "generateUin");
		assertNotNull(uin);
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testUpdateDraftwithException() throws IdRepoAppException {
		IdRequestDTO request = new IdRequestDTO();
		String registrationId = "1234567890";
		IdResponseDTO response = idRepoServiceImpl.updateDraft(registrationId, request);
		assertNull(response);
	}
	
	@Test
	public void testUpdateDraft() throws IdRepoAppException, IOException {
		IdRequestDTO request = new IdRequestDTO();
		String registrationId = "1234567890";
		RequestDTO req = new RequestDTO();
		UinDraft draft =  new UinDraft();
		draft.setUin("274390482564");
		String identityData = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-data.json"),
				StandardCharsets.UTF_8);
		byte[] Uindata = identityData.getBytes();
		draft.setUinData(Uindata);
		req.setIdentity(mapper.readValue(identityData, Object.class));
		request.setRequest(req);
		Optional<UinDraft> uinOpt =Optional.of(draft);
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinPath", uinPath);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);
		IdResponseDTO response = idRepoServiceImpl.updateDraft(registrationId, request);
		assertNotNull(response);
	}
	
	@Test
	public void testUpdateDraftWithNullUinData() throws IdRepoAppException, IOException {
		IdRequestDTO request = new IdRequestDTO();
		String registrationId = "1234567890";
		RequestDTO req = new RequestDTO();
		UinDraft draft =  new UinDraft();
		draft.setUin("274390482564");
		String identityData = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-data.json"),
				StandardCharsets.UTF_8);
		byte[] Uindata = identityData.getBytes();
		draft.setUinData(null);
		req.setIdentity(mapper.readValue(identityData, Object.class));
		request.setRequest(req);
		Optional<UinDraft> uinOpt =Optional.of(draft);
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinPath", uinPath);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);
		IdResponseDTO response = idRepoServiceImpl.updateDraft(registrationId, request);
		assertNotNull(response);
	}
	
	@Test
	public void testUpdateDemographicData() throws JsonParseException, JsonMappingException, IOException {
		IdRequestDTO request = new IdRequestDTO();
		RequestDTO req = new RequestDTO();
		UinDraft draft =  new UinDraft();
		draft.setUin("274390482564");
		String identityData = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-data.json"),
				StandardCharsets.UTF_8);
		byte[] Uindata = identityData.getBytes();
		draft.setUinData(Uindata);
		req.setIdentity(mapper.readValue(identityData, Object.class));
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinPath", uinPath);
		request.setRequest(req);
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "updateDemographicData",request,draft );
		
	}
	
	@Test
	@Ignore// Error in CryptoUtil.decodeURLSafeBase64()
	public void testUpdateDocuments() throws IOException, NoSuchAlgorithmException {
		RequestDTO req = new RequestDTO();
		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("individualBiometrics");
		String docValue = Base64.getEncoder().encodeToString("text biomterics".getBytes()); 
		doc1.setValue(docValue);
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);
		req.setDocuments(docList);
		UinDraft draft =  new UinDraft();
		draft.setUin("274390482564");
//		byte[] uinData = "274390482564".getBytes();
		byte[] salt = "salt".getBytes();
		String identityData = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-data.json"),
				StandardCharsets.UTF_8);
		byte[] Uindata = identityData.getBytes();
		req.setIdentity(mapper.readValue(identityData, Object.class));
		draft.setUinData(Uindata);
		String uinHashwithSalt = DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest(Uindata)).toUpperCase();
		draft.setUinHash("123_"+uinHashwithSalt);				
		UinBiometricDraft biometric = new UinBiometricDraft();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometric.setBioFileId("1234");
		biometric.setBiometricFileName("name");
		draft.setBiometrics(Collections.singletonList(biometric));
		UinDocumentDraft document = new UinDocumentDraft();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocHash("3A6EB0790F39AC87C94F3856B2DD2C5D110E6811602261A9A923D3BB23ADC8B7");
		document.setDocId("1234");
		document.setDocName("name");
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		draft.setDocuments(Collections.singletonList(document));
		draft.setUinDataHash(DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase());
		ReflectionTestUtils.setField(idRepoServiceImpl, "cbeffUtil", cbeffUtil);
		ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "bioAttributes",
				Lists.newArrayList("individualBiometrics", "parentOrGuardianBiometrics"));
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "updateDocuments",req,draft );
	}
	
	@Test
	public void testConstructIdResponse() throws IOException {
		String identityData = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-data.json"),
				StandardCharsets.UTF_8);
		byte[] Uindata = identityData.getBytes();
		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("individualBiometrics");
		doc1.setValue("text biomterics");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		IdResponseDTO response = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "constructIdResponse",Uindata,"success",docList,"1234567890" );
		assertNotNull(response);
	}
	
	@Test 
	public void testGetModalityForFormat() {
		String response = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "getModalityForFormat","1234567890" );
		assertNotNull(response);
	}
	
	@Test
//	@Ignore//UndeclaredThrowable
	public void testExtractAndGetCombinedCbeff() {
		String uinHash = "5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7";
		String bioFileId = "1234";
		Map<String, String> extractionFormats = new HashMap<>();
		ReflectionTestUtils.setField(idRepoServiceImpl, "proxyService", proxyService);
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		byte[] response = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "extractAndGetCombinedCbeff",uinHash,bioFileId,extractionFormats);
		assertNotNull(response);
	}
	
	@Test
	public void testdeleteExistingExtractedBioData() throws NoSuchAlgorithmException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		String uinHash = DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		UinBiometricDraft bioDraft =  new UinBiometricDraft();
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		bioDraft.setUin(uin);
		bioDraft.setBioFileId("1234");
		bioDraft.setBiometricFileName("Finger");
		bioDraft.setBiometricFileType("Finger");	
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "deleteExistingExtractedBioData",extractionFormats,uinHash,bioDraft);	
	}
	
	
	@Test(expected = IdRepoAppException.class)
//	@Ignore//UndeclaredThrowable
	public void testExtractBiometricsDraft() throws IdRepoAppException, IOException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		UinDraft uin = new UinDraft();
		String identityData = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-data.json"),
				StandardCharsets.UTF_8);
		uin.setUin("274390482564");
		uin.setUinHash("123_"+"5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		List<UinBiometricDraft> biometrics = new ArrayList<UinBiometricDraft>();
		UinBiometricDraft biometric = new UinBiometricDraft();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometric.setBioFileId("1234");
		biometric.setBiometricFileName("name");
		biometrics.add(biometric);
		uin.setBiometrics(biometrics);
		ReflectionTestUtils.setField(idRepoServiceImpl, "objectStoreHelper", objectStoreHelper);
		ReflectionTestUtils.setField(proxyService, "cbeffUtil", cbeffUtil);
		ReflectionTestUtils.setField(idRepoServiceImpl, "proxyService", proxyService);
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "extractBiometricsDraft",extractionFormats,uin);			
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testExtractBiometricswithIdRepoAppException() throws IdRepoAppException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		IdResponseDTO response = idRepoServiceImpl.extractBiometrics("1234567890", extractionFormats);
		assertNotNull(response);
	}
	
	@Test(expected = JDBCConnectionException.class)
	public void testExtractBiometricswithException() throws IdRepoAppException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(JDBCConnectionException.class);
		IdResponseDTO response = idRepoServiceImpl.extractBiometrics("1234567890", extractionFormats);
		assertNotNull(response);
	}
	
	@Test(expected = IdRepoAppException.class)
//	@Test
	public void testExtractBiometrics() throws IdRepoAppException, NoSuchAlgorithmException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		String uinHash = DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		uin.setUinHash("123_"+uinHash);
		uin.setRegId("1234567890");
		uin.setUinData("274390482564".getBytes());
		uin.setUinDataHash(DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase());
		Optional<UinDraft> uinOpt =Optional.of(uin);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);
 		IdResponseDTO response = idRepoServiceImpl.extractBiometrics("1234567890", extractionFormats);
		assertNotNull(response);
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testGetDraftwithIdRepoAppException() throws IdRepoAppException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		IdResponseDTO response = idRepoServiceImpl.getDraft("1234567890", extractionFormats);
		assertNotNull(response);
	}
	
	@Test(expected = JDBCConnectionException.class)
	public void testGetDraftwithException() throws IdRepoAppException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(JDBCConnectionException.class);
		IdResponseDTO response = idRepoServiceImpl.getDraft("1234567890", extractionFormats);
		assertNotNull(response);
	}
	
	@Test
	public void testGetDraft() throws IdRepoAppException, NoSuchAlgorithmException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		String uinHash = DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		uin.setUinHash("123_"+uinHash);
		uin.setRegId("1234567890");
		uin.setUinData("274390482564".getBytes());
		uin.setUinDataHash(DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase());
		Optional<UinDraft> uinOpt =Optional.of(uin);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);
 		IdResponseDTO response = idRepoServiceImpl.getDraft("1234567890", extractionFormats);
		assertNotNull(response);
	}
	
	@Test
	public void testDiscardDraft() throws IdRepoAppException, NoSuchAlgorithmException {
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		String uinHash = DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		uin.setUinHash("123_"+uinHash);
		uin.setRegId("1234567890");
		uin.setUinData("274390482564".getBytes());
		uin.setUinDataHash(DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase());
		Optional<UinDraft> uinOpt =Optional.of(uin);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);	
		IdResponseDTO response = idRepoServiceImpl.discardDraft("1234567890");
		assertNotNull(response);
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testDiscardDraftwithEmptyUin() throws IdRepoAppException {
		Optional<UinDraft> uinOpt =Optional.empty();
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);	
		IdResponseDTO response = idRepoServiceImpl.discardDraft("1234567890");
		assertNotNull(response);
	}
	

	@Test(expected = IdRepoAppException.class)
	public void testDiscardDraftwithException() throws IdRepoAppException {
		when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(IdRepoAppException.class);	
		IdResponseDTO response = idRepoServiceImpl.discardDraft("1234567890");
		assertNotNull(response);
	}
	
	@Test
	public void testDecryptUin() throws IdRepoAppException, NoSuchAlgorithmException {
		String uin = "274390482564";
		String uinHash = "1234_"+DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinEncryptSaltRepo", uinEncryptSaltRepo);
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
//		when(uinEncryptSaltRepo.getOne(Mockito.any()).getSalt()).thenReturn("salt");
		when(securityManager.decryptWithSalt(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn("274390482564".getBytes());
		String res = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "decryptUin", uin,uinHash);
		assertSame(uin,res);
	}
	
	@Test
	public void testBuildRequest() throws IOException {
		UinDraft draft = new UinDraft();
		String identityData = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("identity-data.json"),
				StandardCharsets.UTF_8);
		draft.setUinData(identityData.getBytes());
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		IdRequestDTO response = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "buildRequest","1234567890",draft);			
		assertNotNull(response);
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testPublishDraftwithException() throws IdRepoAppException {
		Optional<UinDraft> uinDraft = Optional.empty();	
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinDraft);
		IdResponseDTO response = idRepoServiceImpl.publishDraft("123567890");
	}
	
	@Test
//	@Ignore//ArrayIndexOutofBounds
	public void testPublishDraft() throws IdRepoAppException, NoSuchAlgorithmException {
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		String uinHash = DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		uin.setUinHash("123_"+uinHash);
		uin.setRegId("1234567890");
		uin.setUinData("274390482564".getBytes());
		uin.setUinDataHash(DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase());
		Optional<UinDraft> uinOpt =Optional.of(uin);
		ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);
		IdResponseDTO response = idRepoServiceImpl.publishDraft("123567890");
		assertNotNull(response);
	}
	
	@Test
	public void testValidateRequest() {
		ReflectionTestUtils.setField(idRepoServiceImpl, "validator", validator);
		RequestDTO req = new RequestDTO();
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "validateRequest",req);			
	}
	
	@Test(expected = JDBCConnectionException.class )
//	@Ignore
	public void testUpdateDraftwithJDBCConnectionException() throws IdRepoAppException {
		IdRequestDTO request = new IdRequestDTO();
		String registrationId = "1234567890";
		when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(JDBCConnectionException.class);
		IdResponseDTO response = idRepoServiceImpl.updateDraft(registrationId, request);
		assertNull(response);
	}
	
	@Test(expected = InvalidJsonException.class )
//	@Ignore
	public void testUpdateDraftwithJSONException() throws IdRepoAppException {
		IdRequestDTO request = new IdRequestDTO();
		String registrationId = "1234567890";
		when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(InvalidJsonException.class);
		IdResponseDTO response = idRepoServiceImpl.updateDraft(registrationId, request);
		assertNull(response);
	}
	
	@Test(expected = JDBCConnectionException.class)
	public void testPublishDraftJDBCConnectionException() throws IdRepoAppException {
		ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(JDBCConnectionException.class);
		IdResponseDTO response = idRepoServiceImpl.publishDraft("123567890");
		assertNotNull(response);
	}
	
	@Test(expected = JDBCConnectionException.class)
	public void testDiscardDraftJDBCConnectionException() throws IdRepoAppException {
		when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(JDBCConnectionException.class);
		IdResponseDTO response = idRepoServiceImpl.discardDraft("123567890");
		assertNotNull(response);
	}
	
	@Test(expected = JDBCConnectionException.class)
	public void testHasDraftJDBCConnectionException() throws IdRepoAppException {
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenThrow(JDBCConnectionException.class);
		boolean response = idRepoServiceImpl.hasDraft("123567890");
		assertNotNull(response);
	}
	
}
