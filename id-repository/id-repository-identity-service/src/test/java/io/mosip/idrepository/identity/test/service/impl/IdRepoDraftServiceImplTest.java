package io.mosip.idrepository.identity.test.service.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.InvalidJsonException;
import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.RequestDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.entity.UinEncryptSalt;
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
import io.mosip.idrepository.identity.entity.UinDocument;
import io.mosip.idrepository.identity.entity.UinDocumentDraft;
import io.mosip.idrepository.identity.entity.UinDraft;
import io.mosip.idrepository.identity.helper.AnonymousProfileHelper;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;
import io.mosip.idrepository.identity.helper.VidDraftHelper;
import io.mosip.idrepository.identity.repository.IdentityUpdateTrackerRepo;
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
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.CryptoUtil;
import org.apache.commons.io.IOUtils;
import org.hibernate.exception.JDBCConnectionException;
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
import org.springframework.web.context.WebApplicationContext;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.mosip.idrepository.core.constant.IdRepoConstants.FACE_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.FINGER_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IRIS_EXTRACTION_FORMAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest
@Import(EnvUtil.class)
@ActiveProfiles("test")
@ConfigurationProperties("mosip.idrepo.identity")
public class IdRepoDraftServiceImplTest {
	@Mock
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
	private UinEncryptSalt uinEncryptSalt;

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

	@Mock
	private CryptoUtil cryptoUtil;
	
	@Mock
	private IdentityUpdateTrackerRepo identityUpdateTracker;

	@Mock
	private Environment environment;

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
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinBiometricRepo", uinBiometricRepo);
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinDocumentRepo", uinDocumentRepo);
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

	@Test
	public void testCreateDraft() throws IdRepoAppException, IOException, NoSuchAlgorithmException {
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		when(uinHistoryRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(securityManager.getSaltKeyForId(Mockito.anyString())).thenReturn(1234);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("12345");
		when(securityManager.hashwithSalt(Mockito.any(), Mockito.any()))
				.thenReturn("5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("1234567");
		Uin uin = new Uin();
		String identityData = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		uin.setUin("2419762130");
		List<UinBiometric> biometrics = new ArrayList<UinBiometric>();
		UinBiometric biometric = new UinBiometric();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometric.setBioFileId("1234");
		biometric.setBiometricFileName("name");
		biometrics.add(biometric);
		uin.setBiometrics(biometrics);
		UinDocument document = new UinDocument();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocHash("3A6EB0790F39AC87C94F3856B2DD2C5D110E6811602261A9A923D3BB23ADC8B7");
		document.setDocId("1236");
		document.setDocName("name");
		List<UinDocument> listdocs = new ArrayList<UinDocument>();
		listdocs.add(document);
		uin.setDocuments(listdocs);
		String uinHash = DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		uin.setUinHash("123_" + uinHash);
		uin.setRegId("1234567890");
		uin.setUinData(identityData.getBytes());
		uin.setUinDataHash(DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest(identityData.getBytes())).toUpperCase());
		Optional<Uin> uinOpt = Optional.of(uin);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(uinOpt);
		IdResponseDTO idresponse = idRepoServiceImpl.createDraft("1234567890", "2419762130");
		assertNotNull(idresponse);
	}

	@Test
	public void testCreateDraftwithEMptyUin() throws IdRepoAppException, NoSuchAlgorithmException, IOException {
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		when(uinHistoryRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("1234567");
		when(securityManager.getSaltKeyForId(Mockito.anyString())).thenReturn(1234);
		ResponseWrapper<Map<String, String>> response = new ResponseWrapper<Map<String, String>>();
		Map<String, String> res = new HashMap<String, String>();
		res.put("uin", "274390482564");
		response.setResponse(res);
		String identityData = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(response);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("12345");
		when(securityManager.hashwithSalt(Mockito.any(), Mockito.any()))
				.thenReturn("5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		when(securityManager.hash(Mockito.any())).thenReturn(DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest(identityData.getBytes())).toUpperCase());
		IdResponseDTO idresponse = idRepoServiceImpl.createDraft("1234567890", null);
		assertNotNull(idresponse);
	}

	@Test(expected = IdRepoAppException.class)
	public void testCreateDraftwithIdRepoAppException() throws IdRepoAppException {
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		when(uinHistoryRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(securityManager.getSaltKeyForId(Mockito.anyString())).thenReturn(1234);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("12345");
		when(securityManager.hashwithSalt(Mockito.any(), Mockito.any()))
				.thenReturn("1234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		Optional<Uin> uinOpt = Optional.empty();
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(uinOpt);
		IdResponseDTO idresponse = idRepoServiceImpl.createDraft("1234567890", "2419762130");
		assertNull(idresponse);
	}

	@Test(expected = IdRepoAppException.class)
	public void testCreateDraftWithException() throws IdRepoAppException {
		EnvUtil.setIdrepoSaltKeyLength(12);
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		when(uinHistoryRepo.existsByRegId(Mockito.any())).thenReturn(true);
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(true);
		IdResponseDTO idresponse = idRepoServiceImpl.createDraft("1234567890", "274390482564");
		assertNull(idresponse);
	}

	@Test
	public void testGenerateIdentityObject() {
		Object uin1 = "274390482564";
		Object uin = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "generateIdentityObject", uin1);
		assertNotNull(uin);
	}

	@Test
	public void testGenerateUin()
			throws IdRepoDataValidationException, JsonMappingException, RestServiceException, JsonProcessingException {
		ReflectionTestUtils.setField(idRepoServiceImpl, "restBuilder", restBuilder);
		ReflectionTestUtils.setField(idRepoServiceImpl, "restHelper", restHelper);
		ResponseWrapper<Map<String, String>> response = new ResponseWrapper<Map<String, String>>();
		ReflectionTestUtils.setField(restBuilder, "env", env);
		Map<String, String> res = new HashMap<String, String>();
		res.put("uin", "274390482564");
		response.setResponse(res);
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(response);
		String uin = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "generateUin");
		assertSame(uin, "274390482564");
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
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		RequestWrapper<IdRequestDTO<Object>> request = new RequestWrapper<>();
		String registrationId = "1234567890";
		IdRequestDTO<Object> req = new IdRequestDTO<>();
		UinDraft draft = new UinDraft();
		draft.setUinHash("hash");
		draft.setUin("274390482564");
		String identityData = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		byte[] Uindata = identityData.getBytes();
		draft.setUinData(Uindata);
		req.setIdentity(mapper.readValue(identityData, Object.class));
		request.setRequest(req);
		Optional<UinDraft> uinOpt = Optional.of(draft);
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinPath", uinPath);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);
		IdResponseDTO response = idRepoServiceImpl.updateDraft(registrationId, request.getRequest());
		assertNotNull(response);
	}

	@Test
	public void testUpdateDraftWithNullUinData() throws IdRepoAppException, IOException {
		RequestWrapper<IdRequestDTO<Object>> request = new RequestWrapper<>();
		String registrationId = "1234567890";
		IdRequestDTO<Object> req = new IdRequestDTO<>();
		UinDraft draft = new UinDraft();
		draft.setUin("274390482564");
		String identityData = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		byte[] Uindata = identityData.getBytes();
		draft.setUinData(null);
		req.setIdentity(mapper.readValue(identityData, Object.class));
		request.setRequest(req);
		Optional<UinDraft> uinOpt = Optional.of(draft);
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinPath", uinPath);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);
		IdResponseDTO response = idRepoServiceImpl.updateDraft(registrationId, request.getRequest());
		assertNotNull(response);
	}

	@Test
	public void testUpdateDemographicData() throws JsonParseException, JsonMappingException, IOException {
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		IdRequestDTO<Object> req = new IdRequestDTO<>();
		UinDraft draft = new UinDraft();
		draft.setUinHash("hash");
		draft.setUin("274390482564");
		String identityData = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		byte[] Uindata = identityData.getBytes();
		draft.setUinData(Uindata);
		req.setIdentity(mapper.readValue(identityData, Object.class));
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinPath", uinPath);
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "updateDemographicData", req, draft);
	}

	@Test
	public void testUpdateDocuments() throws Exception {
		RequestDTO req = new RequestDTO();
		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("individualBiometrics");
		String docValue = Base64.getEncoder().encodeToString("text biomterics".getBytes());
		doc1.setValue(docValue);
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);
		req.setDocuments(docList);
		UinDraft draft = new UinDraft();
		draft.setUin("274390482564");
		byte[] salt = "salt".getBytes();
		String identityData = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		byte[] Uindata = identityData.getBytes();
		req.setIdentity(mapper.readValue(identityData, Object.class));
		draft.setUinData(Uindata);
		String uinHashwithSalt = DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest(Uindata))
				.toUpperCase();
		draft.setUinHash("123_" + uinHashwithSalt);
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
		draft.setUinDataHash(DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase());
		ReflectionTestUtils.setField(idRepoServiceImpl, "cbeffUtil", cbeffUtil);
		ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "bioAttributes",
				Lists.newArrayList("individualBiometrics", "parentOrGuardianBiometrics"));
		when(cbeffUtil.validateXML(Mockito.any())).thenReturn(true);
		when(securityManager.hash(Mockito.any()))
				.thenReturn("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "updateDocuments", req, draft);
	}

	@Test
	public void testConstructIdResponse() throws IOException {
		String identityData = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		byte[] Uindata = identityData.getBytes();
		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("individualBiometrics");
		doc1.setValue("text biomterics");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		IdResponseDTO response = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "constructIdResponse", Uindata,
				"success", docList, "1234567890");
		assertNotNull(response);
	}

	@Test
	public void testGetModalityForFormat() {
		String response = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "getModalityForFormat", "1234567890");
		assertNotNull(response);
	}

	@Test
	@Ignore
	public void testExtractAndGetCombinedCbeff() {
		String uinHash = "5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7";
		String bioFileId = "1234";
		Map<String, String> extractionFormats = new HashMap<>();
		ReflectionTestUtils.setField(idRepoServiceImpl, "proxyService", proxyService);
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		byte[] response = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "extractAndGetCombinedCbeff", uinHash,
				bioFileId, extractionFormats);
		assertNotNull(response);
	}

	@Test
	public void testdeleteExistingExtractedBioData() throws NoSuchAlgorithmException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		String uinHash = DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		UinBiometricDraft bioDraft = new UinBiometricDraft();
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		bioDraft.setUin(uin);
		bioDraft.setBioFileId("1234");
		bioDraft.setBiometricFileName("Finger");
		bioDraft.setBiometricFileType("Finger");
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "deleteExistingExtractedBioData", extractionFormats,
				uinHash, bioDraft);
	}

	@Test
	public void testExtractBiometricsDraft() throws IdRepoAppException, IOException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		UinDraft uin = new UinDraft();
		String identityData = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		uin.setUin("274390482564");
		uin.setUinHash("123_" + "5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		List<UinBiometricDraft> biometrics = new ArrayList<UinBiometricDraft>();
		UinBiometricDraft biometric = new UinBiometricDraft();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometric.setBioFileId("1234");
		biometric.setBiometricFileName("name");
		biometrics.add(biometric);
		uin.setBiometrics(biometrics);
		uin.setUinData(identityData.getBytes());
		ReflectionTestUtils.setField(idRepoServiceImpl, "objectStoreHelper", objectStoreHelper);
		ReflectionTestUtils.setField(proxyService, "cbeffUtil", cbeffUtil);
		ReflectionTestUtils.setField(idRepoServiceImpl, "proxyService", proxyService);
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "extractBiometricsDraft", extractionFormats, uin);
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

	@Test
	public void testExtractBiometricswithException() throws IdRepoAppException {
		try {
			Map<String, String> extractionFormats = new HashMap<>();
			extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
			extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
			extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
			when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(JDBCConnectionException.class);
			IdResponseDTO response = idRepoServiceImpl.extractBiometrics("1234567890", extractionFormats);
			assertNotNull(response);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
		}
	}

	@Test(expected = IdRepoAppException.class)
	public void testExtractBiometrics() throws IdRepoAppException, NoSuchAlgorithmException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		String uinHash = DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		uin.setUinHash("123_" + uinHash);
		uin.setRegId("1234567890");
		uin.setUinData("274390482564".getBytes());
		uin.setUinDataHash(DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase());
		Optional<UinDraft> uinOpt = Optional.of(uin);
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

	@Test
	public void testGetDraftwithException() throws IdRepoAppException {
		try {
			Map<String, String> extractionFormats = new HashMap<>();
			extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
			extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
			extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
			when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(JDBCConnectionException.class);
			IdResponseDTO response = idRepoServiceImpl.getDraft("1234567890", extractionFormats);
			assertNotNull(response);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
		}
	}

	@Test
	public void testGetDraft() throws IdRepoAppException, NoSuchAlgorithmException, IOException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		String identityData = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		String uinHash = DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		uin.setUinHash("123_" + uinHash);
		uin.setRegId("1234567890");
		uin.setUinData(identityData.getBytes());
		List<UinBiometricDraft> biometrics = new ArrayList<UinBiometricDraft>();
		UinBiometricDraft biometric = new UinBiometricDraft();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometric.setBioFileId("1234");
		biometric.setBiometricFileName("name");
		biometrics.add(biometric);
		uin.setBiometrics(biometrics);
		UinDocumentDraft document = new UinDocumentDraft();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocHash("3A6EB0790F39AC87C94F3856B2DD2C5D110E6811602261A9A923D3BB23ADC8B7");
		document.setDocId("1234");
		document.setDocName("name");
		List<UinDocumentDraft> listdocs = new ArrayList<UinDocumentDraft>();
		listdocs.add(document);
		uin.setDocuments(listdocs);
		uin.setUinDataHash(DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase());
		Optional<UinDraft> uinOpt = Optional.of(uin);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);
		IdResponseDTO response = idRepoServiceImpl.getDraft("1234567890", extractionFormats);
		assertNotNull(response);
	}

	@Test
	public void testDiscardDraft() throws IdRepoAppException, NoSuchAlgorithmException {
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		String uinHash = DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		uin.setUinHash("123_" + uinHash);
		uin.setRegId("1234567890");
		uin.setUinData("274390482564".getBytes());
		uin.setUinDataHash(DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase());
		Optional<UinDraft> uinOpt = Optional.of(uin);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);
		IdResponseDTO response = idRepoServiceImpl.discardDraft("1234567890");
		assertNotNull(response);
	}

	@Test(expected = IdRepoAppException.class)
	public void testDiscardDraftwithEmptyUin() throws IdRepoAppException {
		Optional<UinDraft> uinOpt = Optional.empty();
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);
		IdResponseDTO response = idRepoServiceImpl.discardDraft("1234567890");
		assertNotNull(response);
	}

	@Test(expected = IdRepoAppException.class)
	@Ignore
	public void testDiscardDraftwithException() throws IdRepoAppException {
		when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(IdRepoAppException.class);
		IdResponseDTO response = idRepoServiceImpl.discardDraft("1234567890");
		assertNotNull(response);
	}

	@Test
	@Ignore
	public void testDecryptUin() throws IdRepoAppException, NoSuchAlgorithmException {
		String uin = "274390482564";
		String uinEncrypt = "1234_AKH3N4PlvZlXYkS/zP0cGtghWORy+Mk5SJXnEeVFfeo=";
		String uinHash = DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		String uinHashInput = "1234_" + uinHash;
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinEncryptSaltRepo", uinEncryptSaltRepo);
		ReflectionTestUtils.setField(idRepoServiceImpl, "securityManager", securityManager);
		when(uinEncryptSaltRepo.getOne(Mockito.anyInt())).thenReturn(uinEncryptSalt);
		when(CryptoUtil.decodeURLSafeBase64(Mockito.anyString())).thenReturn("2419762130".getBytes());
		when(CryptoUtil.decodePlainBase64(Mockito.anyString())).thenReturn("2419762130".getBytes());
		ReflectionTestUtils.setField(idRepoServiceImpl, "cryptoUtil", cryptoUtil);
		when(uinEncryptSalt.getSalt()).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(securityManager.getSaltKeyForId(Mockito.anyString())).thenReturn(1234);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("");
		when(securityManager.hashwithSalt(Mockito.any(), Mockito.any())).thenReturn(uinHash);
		when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn("274390482564".getBytes());
		String res = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "decryptUin", uin, uinHashInput);
		assertSame(uin, res);
	}

	@Test
	public void testBuildRequest() throws IOException {
		UinDraft draft = new UinDraft();
		String identityData = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		draft.setUinData(identityData.getBytes());
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		IdRequestDTO response = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "buildRequest", "1234567890",
				draft);
		assertNotNull(response);
	}

	@Test(expected = IdRepoAppException.class)
	public void testPublishDraftwithException() throws IdRepoAppException {
		Optional<UinDraft> uinDraft = Optional.empty();
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinDraft);
		IdResponseDTO response = idRepoServiceImpl.publishDraft("123567890");
	}

	@Test
	@Ignore
	public void testPublishDraft() throws IdRepoAppException, NoSuchAlgorithmException {
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		String uinHash = DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase();
		uin.setUinHash("123_" + uinHash);
		uin.setRegId("1234567890");
		uin.setUinData("274390482564".getBytes());
		uin.setUinDataHash(DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest("2419762130".getBytes())).toUpperCase());
		Optional<UinDraft> uinOpt = Optional.of(uin);
		ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);
		IdResponseDTO response = idRepoServiceImpl.publishDraft("123567890");
		assertNotNull(response);
	}

	@Test
	public void testValidateRequest() {
		ReflectionTestUtils.setField(idRepoServiceImpl, "validator", validator);
		RequestDTO req = new RequestDTO();
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "validateRequest", req);
	}

	@Test
	public void testUpdateDraftwithJDBCConnectionException() throws IdRepoAppException {
		try {
			IdRequestDTO request = new IdRequestDTO();
			String registrationId = "1234567890";
			when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(JDBCConnectionException.class);
			IdResponseDTO response = idRepoServiceImpl.updateDraft(registrationId, request);
			assertNull(response);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
		}
	}

	@Test
	public void testUpdateDraftwithJSONException() throws IdRepoAppException {
		try {
			IdRequestDTO request = new IdRequestDTO();
			String registrationId = "1234567890";
			when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(InvalidJsonException.class);
			IdResponseDTO response = idRepoServiceImpl.updateDraft(registrationId, request);
			assertNull(response);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
		}
	}

	@Test
	public void testPublishDraftJDBCConnectionException() throws IdRepoAppException {
		try {
			ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
			when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(JDBCConnectionException.class);
			IdResponseDTO response = idRepoServiceImpl.publishDraft("123567890");
			assertNotNull(response);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
		}
	}

	@Test
	public void testDiscardDraftJDBCConnectionException() throws IdRepoAppException {
		try {
			when(uinDraftRepo.findByRegId(Mockito.any())).thenThrow(JDBCConnectionException.class);
			IdResponseDTO response = idRepoServiceImpl.discardDraft("123567890");
			assertNotNull(response);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
		}
	}

	@Test
	public void testHasDraftJDBCConnectionException() throws IdRepoAppException {
		try {
			when(uinDraftRepo.existsByRegId(Mockito.any())).thenThrow(JDBCConnectionException.class);
			boolean response = idRepoServiceImpl.hasDraft("123567890");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
		}
	}

	@Test
	public void testPublishDocuments() {
		final Uin uinObject = new Uin();
		uinObject.setUinRefId("1234567890");
		UinDraft uin = new UinDraft();
		UinDocumentDraft document = new UinDocumentDraft();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocHash("3A6EB0790F39AC87C94F3856B2DD2C5D110E6811602261A9A923D3BB23ADC8B7");
		document.setDocId("1234");
		document.setDocName("name");
		List<UinDocumentDraft> listdocs = new ArrayList<UinDocumentDraft>();
		listdocs.add(document);
		uin.setDocuments(listdocs);
		List<UinBiometricDraft> biometrics = new ArrayList<UinBiometricDraft>();
		UinBiometricDraft biometric = new UinBiometricDraft();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometric.setBioFileId("1234");
		biometric.setBiometricFileName("name");
		biometrics.add(biometric);
		uin.setBiometrics(biometrics);
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "publishDocuments", uin, uinObject);
	}

	@Ignore
	@Test
	public void testGenerateUinwithRestServiceException()
			throws IdRepoDataValidationException, JsonMappingException, RestServiceException, JsonProcessingException {
		try {
			ReflectionTestUtils.setField(idRepoServiceImpl, "restBuilder", restBuilder);
			ReflectionTestUtils.setField(idRepoServiceImpl, "restHelper", restHelper);
			ResponseWrapper<Map<String, String>> response = new ResponseWrapper<Map<String, String>>();
			ReflectionTestUtils.setField(restBuilder, "env", env);
			Map<String, String> res = new HashMap<String, String>();
			res.put("uin", "274390482564");
			response.setResponse(res);
			when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
					.thenThrow(IdRepoDataValidationException.class);
			when(restHelper.requestSync(Mockito.any())).thenReturn(response);
			String uin = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "generateUin");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
		}
	}

	@Test
	@Ignore
	public void testGenerateUinwithIdRepoDataValidationException()
			throws IdRepoDataValidationException, JsonMappingException, RestServiceException, JsonProcessingException {
		try {
			ReflectionTestUtils.setField(idRepoServiceImpl, "restBuilder", restBuilder);
			ReflectionTestUtils.setField(idRepoServiceImpl, "restHelper", restHelper);
			ResponseWrapper<Map<String, String>> response = new ResponseWrapper<Map<String, String>>();
			ReflectionTestUtils.setField(restBuilder, "env", env);
			Map<String, String> res = new HashMap<String, String>();
			res.put("uin", "274390482564");
			response.setResponse(res);
			when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
					.thenReturn(new RestRequestDTO());
			when(restHelper.requestSync(Mockito.any())).thenThrow(RestServiceException.class);
			String uin = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "generateUin");
			assertSame(uin, "274390482564");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UIN_GENERATION_FAILED.getErrorCode(), e.getErrorCode());
		}
	}

	@Test
	public void testCreateDraftwithJDBCConnectionException() {
		try {
			when(uinHistoryRepo.existsByRegId(Mockito.anyString())).thenReturn(false);
			when(uinDraftRepo.existsByRegId(Mockito.anyString())).thenThrow(JDBCConnectionException.class);
			idRepoServiceImpl.createDraft("123457890", "45678901234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
		}
	}

	@Test
	public void testconstructIdResponsewithNUll() {
		IdResponseDTO response = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "constructIdResponse", null,
				"DRAFTED", null, null);
		assertNotNull(response);
	}

	@Test
	public void testUpdateBiometricAndDocumentDrafts() {
		Uin uin = new Uin();
		List<UinBiometric> biometrics = new ArrayList<UinBiometric>();
		UinBiometric biometric = new UinBiometric();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometric.setBioFileId("1234");
		biometric.setBiometricFileName("name");
		biometrics.add(biometric);
		uin.setBiometrics(biometrics);
		UinDraft draft = new UinDraft();
		List<UinBiometricDraft> draftbiometrics = new ArrayList<UinBiometricDraft>();
		UinBiometricDraft draftbiometric = new UinBiometricDraft();
		draftbiometric.setBiometricFileType("individualBiometrics");
		draftbiometric.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		draftbiometric.setBioFileId("1235");
		draftbiometric.setBiometricFileName("name");
		draftbiometrics.add(draftbiometric);
		draft.setBiometrics(draftbiometrics);
		UinDocumentDraft draftdocument = new UinDocumentDraft();
		draftdocument.setDoccatCode("ProofOfIdentity");
		draftdocument.setDocHash("3A6EB0790F39AC87C94F3856B2DD2C5D110E6811602261A9A923D3BB23ADC8B7");
		draftdocument.setDocId("1234");
		draftdocument.setDocName("name");
		List<UinDocumentDraft> draftlistdocs = new ArrayList<UinDocumentDraft>();
		draftlistdocs.add(draftdocument);
		draft.setDocuments(draftlistdocs);
		UinDocument document = new UinDocument();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocHash("3A6EB0790F39AC87C94F3856B2DD2C5D110E6811602261A9A923D3BB23ADC8B7");
		document.setDocId("1236");
		document.setDocName("name");
		List<UinDocument> listdocs = new ArrayList<UinDocument>();
		listdocs.add(document);
		uin.setDocuments(listdocs);
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "updateBiometricAndDocumentDrafts", "123456890", draft,
				uin);
	}

	@Test
	public void testGetDraftUinNullUin() throws IdRepoAppException {
		when(securityManager.getSaltKeyForId(Mockito.anyString())).thenReturn(1234);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("12345");
		when(securityManager.hashwithSalt(Mockito.any(), Mockito.any()))
				.thenReturn("1234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		assertNotNull(idRepoServiceImpl.getDraftUin("6856306938"));
	}

	@Test
	public void testGetDraftUinSuccess() throws IdRepoAppException, IOException {
		String uin = "6856306938";
		String regId = "123";
		String identityData = IOUtils.toString(
				Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("identity-data.json")), StandardCharsets.UTF_8);
		when(securityManager.getSaltKeyForId(Mockito.anyString())).thenReturn(1234);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("12345");
		when(securityManager.hashwithSalt(Mockito.any(), Mockito.any()))
				.thenReturn("1234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		when(environment.getProperty(Mockito.anyString(), Mockito.anyString())).thenReturn("UIN");
		UinDraft uinDraft = new UinDraft();
		uinDraft.setUin(uin);
		uinDraft.setRegId(regId);
		uinDraft.setCreatedDateTime(LocalDateTime.now());
		uinDraft.setUinData(identityData.getBytes());
		when(uinDraftRepo.findByUinHash(Mockito.anyString())).thenReturn(uinDraft);
		assertEquals(regId, idRepoServiceImpl.getDraftUin(uin).getDrafts().get(0).getRid());
	}

	@Test
	public void testGetDraftUinFailure() {
		try {
			String uin = "6856306938";
			String regId = "123";
			when(securityManager.getSaltKeyForId(Mockito.anyString())).thenReturn(1234);
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("12345");
			when(securityManager.hashwithSalt(Mockito.any(), Mockito.any()))
					.thenReturn("1234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
			when(environment.getProperty(Mockito.anyString(), Mockito.anyString())).thenReturn("UIN");
			UinDraft uinDraft = new UinDraft();
			uinDraft.setUin(uin);
			uinDraft.setRegId(regId);
			uinDraft.setCreatedDateTime(LocalDateTime.now());
			uinDraft.setUinData("123{".getBytes());
			when(uinDraftRepo.findByUinHash(Mockito.anyString())).thenReturn(uinDraft);
			idRepoServiceImpl.getDraftUin(uin);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
		}
	}
}
