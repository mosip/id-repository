package io.mosip.idrepository.identity.test.service.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import io.mosip.idrepository.identity.helper.IdRepoServiceHelper;
import io.mosip.idrepository.identity.helper.VidDraftHelper;
import io.mosip.idrepository.identity.repository.IdentityUpdateTrackerRepo;
import io.mosip.idrepository.identity.repository.UinBiometricDraftRepo;
import io.mosip.idrepository.identity.repository.UinBiometricHistoryRepo;
import io.mosip.idrepository.identity.repository.UinBiometricRepo;
import io.mosip.idrepository.identity.repository.UinDocumentDraftRepo;
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
import org.apache.commons.io.IOUtils;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

	@Mock
	private IdRepoServiceHelper idRepoServiceHelper;

	@InjectMocks
	IdRepoServiceImpl service;

	@Mock
	private TestableProxyService proxyService;

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

	@Mock
	private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

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

	@Mock
	private UinBiometricDraftRepo uinBiometricDraftRepo;

	@Mock
	private UinDocumentDraftRepo uinDocumentDraftRepo;

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
		ReflectionTestUtils.setField(idRepoServiceImpl, "idRepoServiceHelper", idRepoServiceHelper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "proxyService", proxyService);
		ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "validator", validator);
		ReflectionTestUtils.setField(idRepoServiceImpl, "objectStoreHelper", objectStoreHelper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "transactionTemplate", transactionTemplate);
		when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
			TransactionCallback<?> callback = invocation.getArgument(0);
			return callback.doInTransaction(null);
		});
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
		when(idRepoServiceHelper.generateUin()).thenReturn("274390482564");
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
			throws IdRepoDataValidationException, JsonMappingException, RestServiceException, JsonProcessingException, IdRepoAppException {
		IdRepoServiceHelper helper = new IdRepoServiceHelper();
		ReflectionTestUtils.setField(helper, "restBuilder", restBuilder);
		ReflectionTestUtils.setField(helper, "restHelper", restHelper);
		ResponseWrapper<Map<String, String>> response = new ResponseWrapper<Map<String, String>>();
		ReflectionTestUtils.setField(restBuilder, "env", env);
		Map<String, String> res = new HashMap<String, String>();
		res.put("uin", "274390482564");
		response.setResponse(res);
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(response);
		String uin = helper.generateUin();
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
		IdRequestDTO request = new IdRequestDTO();
		String registrationId = "1234567890";
		RequestDTO req = new RequestDTO();
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
		IdResponseDTO response = idRepoServiceImpl.updateDraft(registrationId, request);
		assertNotNull(response);
	}

	@Test
	public void testUpdateDraftWithNullUinData() throws IdRepoAppException, IOException {
		IdRequestDTO request = new IdRequestDTO();
		String registrationId = "1234567890";
		RequestDTO req = new RequestDTO();
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
		IdResponseDTO response = idRepoServiceImpl.updateDraft(registrationId, request);
		assertNotNull(response);
	}

	@Test
	public void testUpdateDemographicData() throws JsonParseException, JsonMappingException, IOException {
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		IdRequestDTO request = new IdRequestDTO();
		RequestDTO req = new RequestDTO();
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
		request.setRequest(req);
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "updateDemographicData", request, draft);
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
		verify(objectStoreHelper).putBiometricObject(eq(uinHashwithSalt), any(), any());
		verify(objectStoreHelper).getBiometricObject(eq(uinHashwithSalt), eq("1234"));
		verify(objectStoreHelper, never()).putDraftBiometricObject(any(), any(), any());
		verify(objectStoreHelper, never()).getDraftBiometricObject(any(), any());
		verify(objectStoreHelper, never()).getRidHash(anyString());
	}

	@Test
	public void testUpdateDocumentsV2_usesDraftRidHashPath() throws Exception {
		RequestDTO req = new RequestDTO();
		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("individualBiometrics");
		String docValue = Base64.getEncoder().encodeToString("text biomterics".getBytes());
		doc1.setValue(docValue);
		req.setDocuments(Collections.singletonList(doc1));
		UinDraft draft = new UinDraft();
		draft.setUin("274390482564");
		draft.setRegId("1234567890");
		String identityData = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		req.setIdentity(mapper.readValue(identityData, Object.class));
		draft.setUinData(identityData.getBytes());
		String uinHashwithSalt = DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest(identityData.getBytes()))
				.toUpperCase();
		draft.setUinHash("123_" + uinHashwithSalt);
		UinBiometricDraft biometric = new UinBiometricDraft();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometric.setBioFileId("1234");
		biometric.setBiometricFileName("name");
		draft.setBiometrics(new ArrayList<>(List.of(biometric)));
		draft.setDocuments(new ArrayList<>());
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "cbeffUtil", cbeffUtil);
		ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "bioAttributes",
				Lists.newArrayList("individualBiometrics", "parentOrGuardianBiometrics"));
		when(cbeffUtil.validateXML(Mockito.any())).thenReturn(true);
		when(securityManager.hash(Mockito.any()))
				.thenReturn("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("AAFCC2383A50FAFD9131EF9F731CCCF276BBCD6D62076ADF6C887B791BB75D85");
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "updateDocumentsV2", req, draft);
		verify(objectStoreHelper).putDraftBiometricObject(eq("AAFCC2383A50FAFD9131EF9F731CCCF276BBCD6D62076ADF6C887B791BB75D85"), any(), any());
		verify(objectStoreHelper).getDraftBiometricObject(eq("AAFCC2383A50FAFD9131EF9F731CCCF276BBCD6D62076ADF6C887B791BB75D85"), eq("1234"));
		verify(objectStoreHelper, never()).putBiometricObject(any(), any(), any());
		verify(objectStoreHelper, never()).getBiometricObject(any(), any());
	}

	@Test
	public void should_storeAndReadBiometricsOnLiveUinHash_when_updateDraftV1() throws Exception {
		stubDocumentUpload();
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(anyString())).thenReturn(Optional.of(draft));

		IdRequestDTO request = new IdRequestDTO();
		request.setRequest(documentUpdateRequest(draft.getUinData()));

		IdResponseDTO response = idRepoServiceImpl.updateDraft(draft.getRegId(), request);

		assertNotNull(response);
		String liveUinHash = draft.getUinHash().split("_")[1];
		verify(objectStoreHelper).putBiometricObject(eq(liveUinHash), any(), any());
		verify(objectStoreHelper).putDemographicObject(eq(liveUinHash), any(), any());
		verify(objectStoreHelper).getBiometricObject(eq(liveUinHash), eq("1234"));
		verify(objectStoreHelper, never()).putDraftBiometricObject(any(), any(), any());
		verify(objectStoreHelper, never()).putDraftDemographicObject(any(), any(), any());
		verify(objectStoreHelper, never()).getDraftBiometricObject(any(), any());
		verify(objectStoreHelper, never()).getRidHash(anyString());
	}

	@Test
	public void should_storeAndReadBiometricsOnDraftRidHash_when_updateDraftV2() throws Exception {
		stubDocumentUpload();
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(anyString())).thenReturn(Optional.of(draft));
		when(objectStoreHelper.getRidHash(draft.getRegId())).thenReturn("AAFCC2383A50FAFD9131EF9F731CCCF276BBCD6D62076ADF6C887B791BB75D85");

		IdRequestDTO request = new IdRequestDTO();
		request.setRequest(documentUpdateRequest(draft.getUinData()));

		IdResponseDTO response = idRepoServiceImpl.updateDraftV2(draft.getRegId(), request);

		assertEquals("DRAFTED", response.getResponse().getStatus());
		verify(objectStoreHelper).putDraftBiometricObject(eq("AAFCC2383A50FAFD9131EF9F731CCCF276BBCD6D62076ADF6C887B791BB75D85"), any(), any());
		verify(objectStoreHelper).putDraftDemographicObject(eq("AAFCC2383A50FAFD9131EF9F731CCCF276BBCD6D62076ADF6C887B791BB75D85"), any(), any());
		verify(objectStoreHelper).getDraftBiometricObject(eq("AAFCC2383A50FAFD9131EF9F731CCCF276BBCD6D62076ADF6C887B791BB75D85"), eq("1234"));
		verify(objectStoreHelper, never()).putBiometricObject(any(), any(), any());
		verify(objectStoreHelper, never()).putDemographicObject(any(), any(), any());
		verify(objectStoreHelper, never()).getBiometricObject(any(), any());
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

	@Test
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
		uin.setBiometrics(new ArrayList<>());
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
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(true);
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
			when(uinDraftRepo.existsByRegId(Mockito.any())).thenThrow(JDBCConnectionException.class);
			IdResponseDTO response = idRepoServiceImpl.discardDraft("123567890");
			assertNotNull(response);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
		}
	}

	// ── discardDraftV2 ────────────────────────────────────────────────────── //

	@Test
	public void should_discardDraftV2_deleteObjectStoreFilesAndDbRecords_when_draftExists()
			throws IdRepoAppException {
		when(uinDraftRepo.existsByRegId(anyString())).thenReturn(true);
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");
		doAnswer(invocation -> {
			java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action = invocation.getArgument(0);
			action.accept(null);
			return null;
		}).when(transactionTemplate).executeWithoutResult(any());

		IdResponseDTO response = idRepoServiceImpl.discardDraftV2("1234567890");

		assertEquals("DISCARDED", response.getResponse().getStatus());
		assertNull(response.getResponse().getIdentity());
		assertNull(response.getResponse().getDocuments());
		verify(uinDraftRepo, never()).findByRegId(any());
		InOrder inOrder = inOrder(objectStoreHelper, uinBiometricDraftRepo, uinDocumentDraftRepo, uinDraftRepo);
		inOrder.verify(objectStoreHelper).deleteAllDraftBiometrics("RID_HASH_TEST");
		inOrder.verify(objectStoreHelper).deleteAllDraftDemographics("RID_HASH_TEST");
		inOrder.verify(uinBiometricDraftRepo).deleteByRegId("1234567890");
		inOrder.verify(uinDocumentDraftRepo).deleteByRegId("1234567890");
		inOrder.verify(uinDraftRepo).deleteByRegId("1234567890");
	}

	@Test
	public void should_discardDraftV2_keepDbRecords_when_objectDeleteFails()
			throws IdRepoAppException {
		when(uinDraftRepo.existsByRegId(anyString())).thenReturn(true);
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");
		doThrow(new IdRepoAppException(IdRepoErrorConstants.DRAFT_OBJECT_DELETE_FAILED.getErrorCode(),
				IdRepoErrorConstants.DRAFT_OBJECT_DELETE_FAILED.getErrorMessage()))
				.when(objectStoreHelper).deleteAllDraftBiometrics(anyString());

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.discardDraftV2("1234567890"));
		assertEquals(IdRepoErrorConstants.DRAFT_OBJECT_DELETE_FAILED.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper, never()).deleteAllDraftDemographics(anyString());
		verify(uinDraftRepo, never()).deleteByRegId(anyString());
		verify(uinBiometricDraftRepo, never()).deleteByRegId(anyString());
		verify(uinDocumentDraftRepo, never()).deleteByRegId(anyString());
	}

	@Test
	public void should_discardDraftV2_keepDbRecords_when_demographicDeleteFails_afterBioDelete()
			throws IdRepoAppException {
		when(uinDraftRepo.existsByRegId(anyString())).thenReturn(true);
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");
		doThrow(new IdRepoAppException(IdRepoErrorConstants.DRAFT_OBJECT_DELETE_FAILED.getErrorCode(),
				IdRepoErrorConstants.DRAFT_OBJECT_DELETE_FAILED.getErrorMessage()))
				.when(objectStoreHelper).deleteAllDraftDemographics(anyString());

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.discardDraftV2("1234567890"));

		assertEquals(IdRepoErrorConstants.DRAFT_OBJECT_DELETE_FAILED.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper).deleteAllDraftBiometrics("RID_HASH_TEST");
		verify(uinDraftRepo, never()).deleteByRegId(anyString());
		verify(uinBiometricDraftRepo, never()).deleteByRegId(anyString());
		verify(uinDocumentDraftRepo, never()).deleteByRegId(anyString());
	}

	@Test
	public void should_throwNoRecordFound_when_discardDraftV2_ridDoesNotExist()
			throws IdRepoAppException {
		when(uinDraftRepo.existsByRegId(anyString())).thenReturn(false);

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.discardDraftV2("1234567890"));

		assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper, never()).getRidHash(anyString());
		verify(objectStoreHelper, never()).deleteAllDraftBiometrics(anyString());
		verify(objectStoreHelper, never()).deleteAllDraftDemographics(anyString());
		verify(uinDraftRepo, never()).deleteByRegId(anyString());
	}

	@Test
	public void testDiscardDraftV2JDBCConnectionException() throws IdRepoAppException {
		when(uinDraftRepo.existsByRegId(any())).thenThrow(JDBCConnectionException.class);

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.discardDraftV2("123567890"));

		assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper, never()).deleteAllDraftBiometrics(anyString());
	}

	@Test
	public void should_throwDatabaseAccessError_when_discardDraftV2_dbDeleteFails_afterS3()
			throws IdRepoAppException {
		when(uinDraftRepo.existsByRegId(anyString())).thenReturn(true);
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");
		doAnswer(invocation -> {
			java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action = invocation.getArgument(0);
			action.accept(null);
			return null;
		}).when(transactionTemplate).executeWithoutResult(any());
		doThrow(JDBCConnectionException.class).when(uinDraftRepo).deleteByRegId(anyString());

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.discardDraftV2("1234567890"));

		assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper).deleteAllDraftBiometrics("RID_HASH_TEST");
		verify(objectStoreHelper).deleteAllDraftDemographics("RID_HASH_TEST");
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
		verify(uinBiometricRepo).saveAll(any());
		verify(uinDocumentRepo).saveAll(any());
	}

	@Test
	public void testPublishDocuments_ignoresDuplicateBioRows_andStillSavesDocuments() {
		final Uin uinObject = new Uin();
		uinObject.setUinRefId("1234567890");
		UinDraft uin = new UinDraft();
		UinDocumentDraft document = new UinDocumentDraft();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocHash("3A6EB0790F39AC87C94F3856B2DD2C5D110E6811602261A9A923D3BB23ADC8B7");
		document.setDocId("1234");
		document.setDocName("name");
		uin.setDocuments(new ArrayList<>(List.of(document)));
		UinBiometricDraft biometric = new UinBiometricDraft();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometric.setBioFileId("1234");
		biometric.setBiometricFileName("name");
		uin.setBiometrics(new ArrayList<>(List.of(biometric)));
		when(uinBiometricRepo.saveAll(any())).thenThrow(new DataIntegrityViolationException("uk_uinb"));

		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "publishDocuments", uin, uinObject);

		verify(uinBiometricRepo).saveAll(any());
		verify(uinDocumentRepo).saveAll(any());
	}

	@Test
	public void testPublishDocuments_ignoresDuplicateDocRows() {
		final Uin uinObject = new Uin();
		uinObject.setUinRefId("1234567890");
		UinDraft uin = new UinDraft();
		UinDocumentDraft document = new UinDocumentDraft();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocHash("3A6EB0790F39AC87C94F3856B2DD2C5D110E6811602261A9A923D3BB23ADC8B7");
		document.setDocId("1234");
		document.setDocName("name");
		uin.setDocuments(new ArrayList<>(List.of(document)));
		UinBiometricDraft biometric = new UinBiometricDraft();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometric.setBioFileId("1234");
		biometric.setBiometricFileName("name");
		uin.setBiometrics(new ArrayList<>(List.of(biometric)));
		when(uinDocumentRepo.saveAll(any())).thenThrow(new DataIntegrityViolationException("uk_uind"));

		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "publishDocuments", uin, uinObject);

		verify(uinBiometricRepo).saveAll(any());
		verify(uinDocumentRepo).saveAll(any());
	}

	@Ignore
	@Test
	public void testGenerateUinwithRestServiceException()
			throws IdRepoDataValidationException, JsonMappingException, RestServiceException, JsonProcessingException {
		try {
			IdRepoServiceHelper helper = new IdRepoServiceHelper();
			ReflectionTestUtils.setField(helper, "restBuilder", restBuilder);
			ReflectionTestUtils.setField(helper, "restHelper", restHelper);
			ResponseWrapper<Map<String, String>> response = new ResponseWrapper<Map<String, String>>();
			ReflectionTestUtils.setField(restBuilder, "env", env);
			Map<String, String> res = new HashMap<String, String>();
			res.put("uin", "274390482564");
			response.setResponse(res);
			when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
					.thenThrow(IdRepoDataValidationException.class);
			when(restHelper.requestSync(Mockito.any())).thenReturn(response);
			helper.generateUin();
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
		}
	}

	@Test
	@Ignore
	public void testGenerateUinwithIdRepoDataValidationException()
			throws IdRepoDataValidationException, JsonMappingException, RestServiceException, JsonProcessingException {
		try {
			IdRepoServiceHelper helper = new IdRepoServiceHelper();
			ReflectionTestUtils.setField(helper, "restBuilder", restBuilder);
			ReflectionTestUtils.setField(helper, "restHelper", restHelper);
			ResponseWrapper<Map<String, String>> response = new ResponseWrapper<Map<String, String>>();
			ReflectionTestUtils.setField(restBuilder, "env", env);
			Map<String, String> res = new HashMap<String, String>();
			res.put("uin", "274390482564");
			response.setResponse(res);
			when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
					.thenReturn(new RestRequestDTO());
			when(restHelper.requestSync(Mockito.any())).thenThrow(RestServiceException.class);
			helper.generateUin();
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
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
		}
	}

	@Test
	public void testCreateDraftWhenGenerateUinThrowsException() throws Exception {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		when(idRepoServiceHelper.generateUin())
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR));
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraft("REG123", null));
		assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void testCreateDraftWhenRestServiceExceptionOccurs() throws Exception {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		when(idRepoServiceHelper.generateUin())
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UIN_GENERATION_FAILED));
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraft("REG123", null));
		assertEquals(IdRepoErrorConstants.UIN_GENERATION_FAILED.getErrorCode(), thrown.getErrorCode());
	}

	// ── createDraftV2 (generateUin=false — bare LOST draft) ─────────────────

	@Test
	public void should_createDraftSuccessfully_when_ridDoesNotExist() throws IdRepoAppException {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		IdResponseDTO response = idRepoServiceImpl.createDraftV2("REG123", null, false);
		assertDraftedResponse(response);
		UinDraft saved = captureSavedDraft();
		assertEquals("REG123", saved.getRegId());
		assertEquals("DRAFT", saved.getStatusCode());
		assertNull(saved.getUin());
		assertNull(saved.getUinHash());
		verify(idRepoServiceHelper, never()).generateUin();
		verify(objectStoreHelper, never()).copyBiometricLiveToDraft(any(), any(), any());
		verify(objectStoreHelper, never()).copyDemographicLiveToDraft(any(), any(), any());
		verify(uinDraftRepo, times(1)).save(any(UinDraft.class));
	}

	@Test
	public void should_throwRecordExists_when_ridAlreadyExists() {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(true);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraftV2("REG123", null, false));
		assertEquals(IdRepoErrorConstants.RECORD_EXISTS.getErrorCode(), thrown.getErrorCode());
		verify(uinDraftRepo, never()).save(any());
	}

	@Test
	public void should_throwDatabaseAccessError_when_jdbcConnectionException() {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenThrow(JDBCConnectionException.class);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraftV2("REG123", null, false));
		assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_throwRidOlderThanLatestProcessed_when_lostDraftRidExistsInHistoryButNotUin() {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(true);
		when(uinRepo.existsByRegId(any())).thenReturn(false);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraftV2("REG123", null, false));
		assertEquals(IdRepoErrorConstants.RID_OLDER_THAN_LATEST_PROCESSED.getErrorCode(), thrown.getErrorCode());
		verify(uinDraftRepo, never()).save(any());
	}

	@Test
	public void should_createLostDraft_when_ridExistsInHistoryAndIsLatest() throws IdRepoAppException {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(true);
		when(uinRepo.existsByRegId(any())).thenReturn(true);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		IdResponseDTO response = idRepoServiceImpl.createDraftV2("REG123", null, false);
		assertDraftedResponse(response);
		UinDraft saved = captureSavedDraft();
		assertEquals("REG123", saved.getRegId());
		assertEquals("DRAFT", saved.getStatusCode());
		assertNull(saved.getUin());
		assertNull(saved.getUinHash());
		verify(idRepoServiceHelper, never()).generateUin();
		verify(uinDraftRepo, times(1)).save(any(UinDraft.class));
	}

	// ── updateDraftUinData ───────────────────────────────────────────────────

	@Test
	public void should_throwNoRecordFound_when_ridDoesNotExist() throws IdRepoAppException {
		when(uinDraftRepo.findByRegId(anyString())).thenReturn(Optional.empty());
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.updateDraftUinData("REG123", "274390482564"));
		assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_throwDatabaseAccessError_when_updateRidThrowsJdbcException() throws IdRepoAppException {
		when(uinDraftRepo.findByRegId(anyString())).thenThrow(JDBCConnectionException.class);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.updateDraftUinData("REG123", "274390482564"));
		assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_throwUinNotFound_when_uinDoesNotExistInUinTable() throws IOException {
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(anyString())).thenReturn(Optional.of(draft));
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(1234);
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hashSalt");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("some-hash");
		when(uinRepo.existsByUinHash(any())).thenReturn(false);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.updateDraftUinData("REG123", "274390482564"));
		assertEquals(IdRepoErrorConstants.UIN_NOT_FOUND.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_throwDraftUinHashMismatch_when_restampingWithDifferentUin() throws IOException {
		UinDraft draft = buildMinimalDraft();
		draft.setUinHash("different-existing-hash");
		when(uinDraftRepo.findByRegId(anyString())).thenReturn(Optional.of(draft));
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(1234);
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hashSalt");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("some-hash");
		when(uinRepo.existsByUinHash(any())).thenReturn(true);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.updateDraftUinData("REG123", "274390482564"));
		assertEquals(IdRepoErrorConstants.DRAFT_UIN_HASH_MISMATCH.getErrorCode(), thrown.getErrorCode());
	}


	// ── getDraftV2 with type parameter ───────────────────────────────────────

	@Test
	public void should_returnDemographicsOnly_when_typeIsDemographics() throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		stubDraftObjectStoreReads();

		IdResponseDTO response = idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), "demographics");

		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertEquals(List.of("x", "y"), response.getResponse().getVerifiedAttributes());
		Map<String, Object> identity = mapper.convertValue(response.getResponse().getIdentity(),
				new TypeReference<Map<String, Object>>() {});
		assertEquals("2419762130", identity.get("UIN"));
		assertNull(identity.get("verifiedAttributes"));
		assertTrue(response.getResponse().getDocuments() == null
				|| response.getResponse().getDocuments().isEmpty());
		verify(objectStoreHelper).getRidHash("1234567890");
		verify(objectStoreHelper, never()).getDraftBiometricObject(anyString(), anyString());
		verify(objectStoreHelper, never()).getDraftDemographicObject(anyString(), anyString());
		verify(objectStoreHelper, never()).getBiometricObject(anyString(), anyString());
		verify(objectStoreHelper, never()).getDemographicObject(anyString(), anyString());
	}

	@Test
	public void should_returnBiometricsOnly_fromDraftPath_when_typeIsBiometrics()
			throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		stubDraftObjectStoreReads();

		IdResponseDTO response = idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), "biometrics");

		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertNull(response.getResponse().getIdentity());
		assertEquals(1, response.getResponse().getDocuments().size());
		assertEquals("individualBiometrics", response.getResponse().getDocuments().get(0).getCategory());
		assertEquals(CryptoUtil.encodeToURLSafeBase64("extracted-cbeff".getBytes()),
				response.getResponse().getDocuments().get(0).getValue());
		verify(objectStoreHelper).getDraftBiometricObject("RID_HASH_TEST", "1234");
		verify(proxyService).getBiometricsForRequestedFormatsDraft(eq("RID_HASH_TEST"), eq("1234"), any(), any());
		verify(objectStoreHelper, never()).getDraftDemographicObject(anyString(), anyString());
		verify(objectStoreHelper, never()).getBiometricObject(anyString(), anyString());
	}

	@Test
	public void should_returnSupportingDocumentsOnly_fromDraftPath_when_typeIsSupportingDocuments()
			throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		stubDraftObjectStoreReads();

		IdResponseDTO response = idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), "supportingDocuments");

		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertNull(response.getResponse().getIdentity());
		assertEquals(1, response.getResponse().getDocuments().size());
		assertEquals("ProofOfIdentity", response.getResponse().getDocuments().get(0).getCategory());
		assertEquals(CryptoUtil.encodeToURLSafeBase64("doc-bytes".getBytes()),
				response.getResponse().getDocuments().get(0).getValue());
		verify(objectStoreHelper).getDraftDemographicObject("RID_HASH_TEST", "1236");
		verify(objectStoreHelper, never()).getDraftBiometricObject(anyString(), anyString());
		verify(objectStoreHelper, never()).getDemographicObject(anyString(), anyString());
	}

	@Test
	public void should_returnIdentityAndDraftFiles_when_typeIsAll() throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		stubDraftObjectStoreReads();

		IdResponseDTO response = idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), "all");

		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertEquals(List.of("x", "y"), response.getResponse().getVerifiedAttributes());
		assertNotNull(response.getResponse().getIdentity());
		assertEquals(2, response.getResponse().getDocuments().size());
		assertEquals("individualBiometrics", response.getResponse().getDocuments().get(0).getCategory());
		assertEquals("ProofOfIdentity", response.getResponse().getDocuments().get(1).getCategory());
		verify(objectStoreHelper).getDraftBiometricObject("RID_HASH_TEST", "1234");
		verify(objectStoreHelper).getDraftDemographicObject("RID_HASH_TEST", "1236");
		verify(objectStoreHelper, never()).getBiometricObject(anyString(), anyString());
		verify(objectStoreHelper, never()).getDemographicObject(anyString(), anyString());
	}

	@Test
	public void should_returnFullDraft_when_typeIsNullOrBlank() throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		stubDraftObjectStoreReads();

		IdResponseDTO nullType = idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), null);
		IdResponseDTO blankType = idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), "  ");

		assertEquals("DRAFTED", nullType.getResponse().getStatus());
		assertEquals(List.of("x", "y"), nullType.getResponse().getVerifiedAttributes());
		assertEquals(2, nullType.getResponse().getDocuments().size());
		assertEquals("DRAFTED", blankType.getResponse().getStatus());
		assertEquals(List.of("x", "y"), blankType.getResponse().getVerifiedAttributes());
		assertEquals(2, blankType.getResponse().getDocuments().size());
	}

	@Test
	public void should_treatTypeAsDemographics_when_getDraftV2_typeIsMixedCaseWithPadding()
			throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		stubDraftObjectStoreReads();

		IdResponseDTO response = idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), " DEMOGRAPHICS ");

		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertNotNull(response.getResponse().getIdentity());
		assertTrue(response.getResponse().getDocuments() == null
				|| response.getResponse().getDocuments().isEmpty());
		verify(objectStoreHelper, never()).getDraftBiometricObject(anyString(), anyString());
		verify(objectStoreHelper, never()).getDraftDemographicObject(anyString(), anyString());
	}

	@Test
	public void should_skipFileReads_when_getDraftV2_collectionsAreNull() throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		draft.setBiometrics(null);
		draft.setDocuments(null);
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		stubDraftObjectStoreReads();

		IdResponseDTO response = idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), "all");

		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertNotNull(response.getResponse().getIdentity());
		assertTrue(response.getResponse().getDocuments() == null
				|| response.getResponse().getDocuments().isEmpty());
		verify(objectStoreHelper, never()).getDraftBiometricObject(anyString(), anyString());
		verify(objectStoreHelper, never()).getDraftDemographicObject(anyString(), anyString());
	}

	@Test
	public void should_skipSupportingDocumentReads_when_getDraftV2_documentsListIsEmpty()
			throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		draft.setDocuments(new ArrayList<>());
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		stubDraftObjectStoreReads();

		IdResponseDTO response = idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), "supportingdocuments");

		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertNull(response.getResponse().getIdentity());
		assertTrue(response.getResponse().getDocuments() == null
				|| response.getResponse().getDocuments().isEmpty());
		verify(objectStoreHelper, never()).getDraftDemographicObject(anyString(), anyString());
		verify(objectStoreHelper, never()).getDraftBiometricObject(anyString(), anyString());
	}

	@Test
	public void should_throwInvalidInputParameter_when_typeIsInvalid() throws IdRepoAppException {
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), "invalid"));
		assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), thrown.getErrorCode());
		assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "type"),
				thrown.getErrorText());
		verify(uinDraftRepo, never()).findByRegId(any());
		verify(objectStoreHelper, never()).getRidHash(anyString());
	}

	@Test
	public void should_throwNoRecordFound_when_getDraftV2_ridDoesNotExist() throws IdRepoAppException {
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.empty());
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), "demographics"));
		assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper, never()).getRidHash(anyString());
	}

	@Test
	public void should_throwDatabaseAccessError_when_getDraftV2_jdbcException() throws IdRepoAppException {
		when(uinDraftRepo.findByRegId(any())).thenThrow(JDBCConnectionException.class);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), "all"));
		assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper, never()).getRidHash(anyString());
	}

	@Test
	public void should_throwFileNotFound_when_getDraftV2_supportingDocumentMissing()
			throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		stubDraftObjectStoreReads();
		when(objectStoreHelper.getDraftDemographicObject(anyString(), anyString()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.FILE_NOT_FOUND));

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), "supportingdocuments"));
		assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_throwFileNotFound_when_getDraftV2_biometricMissing()
			throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		stubDraftObjectStoreReads();
		when(objectStoreHelper.getDraftBiometricObject(anyString(), anyString()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.FILE_NOT_FOUND));

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.getDraftV2("1234567890", new HashMap<>(), "biometrics"));
		assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper, never()).getDraftDemographicObject(anyString(), anyString());
	}

	// ── createDraftV2 (generateUin=true) — createDraftWithUin ──────────────

	@Test
	public void should_createDraftWithUin_when_uinProvided_and_ridDoesNotExist() throws IdRepoAppException, IOException, NoSuchAlgorithmException {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		Uin uinEntity = buildUinEntity();
		when(uinRepo.findWithBiometricsByUinHash(any())).thenReturn(Optional.of(uinEntity));
		when(uinDraftRepo.findByUinHash(any())).thenReturn(null);
		stubCreateDraftCrypto();
		IdResponseDTO response = idRepoServiceImpl.createDraftV2("REG123", "274390482564", true);
		assertDraftedResponse(response);
		UinDraft saved = captureSavedDraft();
		assertEquals("REG123", saved.getRegId());
		assertEquals("DRAFT", saved.getStatusCode());
		assertEquals(uinEntity.getUinHash(), saved.getUinHash());
		assertEquals("1234_274390482564_YWJj", saved.getUin());
		verify(idRepoServiceHelper, never()).generateUin();
		verify(objectStoreHelper, never()).getRidHash(any());
		verify(objectStoreHelper, never()).copyBiometricLiveToDraft(any(), any(), any());
		verify(objectStoreHelper, never()).copyDemographicLiveToDraft(any(), any(), any());
		verify(proxyService, never()).retrieveIdentityByRid(any(), any(), any());
		verify(uinRepo).findWithBiometricsByUinHash(any());
		verify(uinDraftRepo, times(1)).save(any(UinDraft.class));
	}

	@Test
	public void should_createDraftFromLiveUin_when_generateUinFalse_and_uinProvided() throws Exception {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		Uin uinEntity = buildUinEntity();
		when(uinRepo.findWithBiometricsByUinHash(any())).thenReturn(Optional.of(uinEntity));
		stubCreateDraftCrypto();
		IdResponseDTO response = idRepoServiceImpl.createDraftV2("REG123", "274390482564", false);
		assertDraftedResponse(response);
		UinDraft saved = captureSavedDraft();
		assertEquals("REG123", saved.getRegId());
		assertEquals("DRAFT", saved.getStatusCode());
		assertEquals(uinEntity.getUinHash(), saved.getUinHash());
		verify(idRepoServiceHelper, never()).generateUin();
		verify(uinRepo).findWithBiometricsByUinHash(any());
		verify(uinDraftRepo, times(1)).save(any(UinDraft.class));
	}

	@Test
	public void should_createDraftWithGeneratedUin_when_uinIsNull_and_ridDoesNotExist() throws IdRepoAppException {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		when(idRepoServiceHelper.generateUin()).thenReturn("274390482564");
		stubCreateDraftCrypto();
		when(securityManager.hash(any())).thenReturn("data-hash");
		IdResponseDTO response = idRepoServiceImpl.createDraftV2("REG123", null, true);
		assertDraftedResponse(response);
		UinDraft saved = captureSavedDraft();
		assertEquals("REG123", saved.getRegId());
		assertEquals("DRAFT", saved.getStatusCode());
		assertEquals("1234_274390482564_YWJj", saved.getUin());
		assertEquals("1234_some-hash", saved.getUinHash());
		assertEquals("data-hash", saved.getUinDataHash());
		assertNotNull(saved.getUinData());
		verify(idRepoServiceHelper, times(1)).generateUin();
		verify(objectStoreHelper, never()).getRidHash(any());
		verify(objectStoreHelper, never()).copyBiometricLiveToDraft(any(), any(), any());
		verify(objectStoreHelper, never()).copyDemographicLiveToDraft(any(), any(), any());
		verify(uinRepo, never()).findWithBiometricsByUinHash(any());
		verify(uinRepo, never()).findWithBiometricsByRegId(any());
		verify(uinDraftRepo, times(1)).save(any(UinDraft.class));
	}

	@Test
	public void should_throwRecordExists_when_createDraftWithUin_ridAlreadyExists() throws IdRepoAppException {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(true);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraftV2("REG123", "274390482564", true));
		assertEquals(IdRepoErrorConstants.RECORD_EXISTS.getErrorCode(), thrown.getErrorCode());
		verify(uinDraftRepo, never()).save(any());
		verify(idRepoServiceHelper, never()).generateUin();
	}

	@Test
	public void should_throwNoRecordFound_when_createDraftWithUin_uinNotInDb() throws IdRepoAppException {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(1234);
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hashSalt");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("some-hash");
		when(uinRepo.findWithBiometricsByUinHash(any())).thenReturn(Optional.empty());
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraftV2("REG123", "274390482564", true));
		assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), thrown.getErrorCode());
		verify(uinDraftRepo, never()).save(any());
		verify(idRepoServiceHelper, never()).generateUin();
	}

	@Test
	public void should_throwDatabaseAccessError_when_createDraftWithUin_jdbcException() {
		when(uinDraftRepo.existsByRegId(any())).thenThrow(JDBCConnectionException.class);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraftV2("REG123", "274390482564", true));
		assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
		verify(uinDraftRepo, never()).save(any());
	}

	@Test
	public void should_reuseExistingUin_when_uinIsNull_and_ridAlreadyCommitted() throws Exception {
		// Reprocess of a previously committed NEW packet: uin=null but uin_h has an entry.
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(true);
		// This RID is still the latest/current one on the live Uin table (not superseded).
		when(uinRepo.existsByRegId(any())).thenReturn(true);

		Uin uinEntity = buildUinEntity();
		uinEntity.setUin("1234_YWJj");
		uinEntity.setUinHash("1234_some-hash");
		when(uinRepo.findWithBiometricsByRegId("REG123")).thenReturn(Optional.of(uinEntity));
		when(uinEncryptSaltRepo.getOne(1234)).thenReturn(uinEncryptSalt);
		when(uinEncryptSalt.getSalt()).thenReturn("YWJj");
		when(securityManager.decryptWithSalt(any(), any(), any())).thenReturn("274390482564".getBytes());
		when(uinDraftRepo.findByUinHash(any())).thenReturn(null);
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(1234);
		when(uinEncryptSaltRepo.retrieveSaltById(anyInt())).thenReturn("YWJj");
		when(securityManager.encryptWithSalt(any(), any(), any())).thenReturn("encrypted".getBytes());
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hashSalt");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("some-hash");

		IdResponseDTO response = idRepoServiceImpl.createDraftV2("REG123", null, true);

		assertDraftedResponse(response);
		UinDraft saved = captureSavedDraft();
		assertEquals("REG123", saved.getRegId());
		assertEquals("DRAFT", saved.getStatusCode());
		assertEquals(uinEntity.getUinHash(), saved.getUinHash());
		assertEquals("1234_274390482564_YWJj", saved.getUin());
		verify(idRepoServiceHelper, never()).generateUin();
		verify(proxyService, never()).retrieveIdentityByRid(any(), any(), any());
		verify(uinRepo).findWithBiometricsByRegId("REG123");
		verify(uinRepo, never()).findByUinHash(any());
		verify(uinRepo, never()).findWithBiometricsByUinHash(any());
		verify(objectStoreHelper, never()).getRidHash(any());
		verify(uinDraftRepo, times(1)).save(any(UinDraft.class));
	}

	@Test
	public void should_copyLiveFilesToDraft_when_createDraftV2_hasBiometricsAndDocuments()
			throws Exception {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		Uin uinEntity = buildUinEntity();
		UinBiometric biometric = new UinBiometric();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBioFileId("bio-1");
		biometric.setBiometricFileName("name");
		biometric.setBiometricFileHash("hash");
		uinEntity.setBiometrics(new ArrayList<>(List.of(biometric)));
		UinDocument document = new UinDocument();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocId("doc-1");
		document.setDocName("name");
		document.setDocHash("hash");
		uinEntity.setDocuments(new ArrayList<>(List.of(document)));
		when(uinRepo.findWithBiometricsByUinHash(any())).thenReturn(Optional.of(uinEntity));
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(1234);
		when(uinEncryptSaltRepo.retrieveSaltById(anyInt())).thenReturn("YWJj");
		when(securityManager.encryptWithSalt(any(), any(), any())).thenReturn("encrypted".getBytes());
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hashSalt");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("some-hash");
		when(objectStoreHelper.getRidHash("REG123")).thenReturn("rid-hash");

		IdResponseDTO response = idRepoServiceImpl.createDraftV2("REG123", "274390482564", true);

		assertDraftedResponse(response);
		String livePrefix = uinEntity.getUinHash().split("_")[1];
		InOrder inOrder = inOrder(objectStoreHelper, uinDraftRepo);
		inOrder.verify(objectStoreHelper).copyBiometricLiveToDraft(livePrefix, "rid-hash", "bio-1");
		inOrder.verify(objectStoreHelper).copyDemographicLiveToDraft(livePrefix, "rid-hash", "doc-1");
		inOrder.verify(uinDraftRepo).save(any(UinDraft.class));
		UinDraft saved = captureSavedDraft();
		assertEquals("REG123", saved.getRegId());
		assertEquals("DRAFT", saved.getStatusCode());
		assertEquals(uinEntity.getUinHash(), saved.getUinHash());
		assertEquals("bio-1", saved.getBiometrics().get(0).getBioFileId());
		assertEquals("doc-1", saved.getDocuments().get(0).getDocId());
		verify(idRepoServiceHelper, never()).generateUin();
		verify(objectStoreHelper, times(1)).copyBiometricLiveToDraft(any(), any(), any());
		verify(objectStoreHelper, times(1)).copyDemographicLiveToDraft(any(), any(), any());
	}

	@Test
	public void should_copyDocumentsOnly_when_createDraftV2_hasNoBiometrics() throws Exception {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		Uin uinEntity = buildUinEntity();
		UinDocument document = new UinDocument();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocId("doc-1");
		document.setDocName("name");
		document.setDocHash("hash");
		uinEntity.setDocuments(new ArrayList<>(List.of(document)));
		when(uinRepo.findWithBiometricsByUinHash(any())).thenReturn(Optional.of(uinEntity));
		stubCreateDraftCrypto();
		when(objectStoreHelper.getRidHash("REG123")).thenReturn("rid-hash");

		IdResponseDTO response = idRepoServiceImpl.createDraftV2("REG123", "274390482564", true);

		assertDraftedResponse(response);
		String livePrefix = uinEntity.getUinHash().split("_")[1];
		verify(objectStoreHelper, never()).copyBiometricLiveToDraft(any(), any(), any());
		verify(objectStoreHelper).copyDemographicLiveToDraft(livePrefix, "rid-hash", "doc-1");
		verify(uinDraftRepo).save(any(UinDraft.class));
		UinDraft saved = captureSavedDraft();
		assertEquals("doc-1", saved.getDocuments().get(0).getDocId());
		assertTrue(saved.getBiometrics() == null || saved.getBiometrics().isEmpty());
	}

	@Test
	public void should_copyBiometricsOnly_when_createDraftV2_hasNoDocuments() throws Exception {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		Uin uinEntity = buildUinEntity();
		UinBiometric biometric = new UinBiometric();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBioFileId("bio-1");
		biometric.setBiometricFileName("name");
		biometric.setBiometricFileHash("hash");
		uinEntity.setBiometrics(new ArrayList<>(List.of(biometric)));
		when(uinRepo.findWithBiometricsByUinHash(any())).thenReturn(Optional.of(uinEntity));
		stubCreateDraftCrypto();
		when(objectStoreHelper.getRidHash("REG123")).thenReturn("rid-hash");

		IdResponseDTO response = idRepoServiceImpl.createDraftV2("REG123", "274390482564", true);

		assertDraftedResponse(response);
		String livePrefix = uinEntity.getUinHash().split("_")[1];
		verify(objectStoreHelper).copyBiometricLiveToDraft(livePrefix, "rid-hash", "bio-1");
		verify(objectStoreHelper, never()).copyDemographicLiveToDraft(any(), any(), any());
		UinDraft saved = captureSavedDraft();
		assertEquals("bio-1", saved.getBiometrics().get(0).getBioFileId());
		assertTrue(saved.getDocuments() == null || saved.getDocuments().isEmpty());
	}

	@Test
	public void should_notSaveDraft_when_createDraftV2_demographicCopyFails_afterBioCopy() throws Exception {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		Uin uinEntity = buildUinEntity();
		UinBiometric biometric = new UinBiometric();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBioFileId("bio-1");
		biometric.setBiometricFileName("name");
		biometric.setBiometricFileHash("hash");
		uinEntity.setBiometrics(new ArrayList<>(List.of(biometric)));
		UinDocument document = new UinDocument();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocId("doc-1");
		document.setDocName("name");
		document.setDocHash("hash");
		uinEntity.setDocuments(new ArrayList<>(List.of(document)));
		when(uinRepo.findWithBiometricsByUinHash(any())).thenReturn(Optional.of(uinEntity));
		stubCreateDraftCrypto();
		when(objectStoreHelper.getRidHash("REG123")).thenReturn("rid-hash");
		doThrow(new IdRepoAppException(IdRepoErrorConstants.DRAFT_OBJECT_COPY_FAILED.getErrorCode(),
				IdRepoErrorConstants.DRAFT_OBJECT_COPY_FAILED.getErrorMessage()))
				.when(objectStoreHelper).copyDemographicLiveToDraft(any(), any(), any());

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraftV2("REG123", "274390482564", true));

		assertEquals(IdRepoErrorConstants.DRAFT_OBJECT_COPY_FAILED.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper).copyBiometricLiveToDraft(any(), any(), any());
		verify(uinDraftRepo, never()).save(any());
	}

	@Test
	public void should_notSaveDraft_when_createDraftV2_objectStoreCopyFails() throws Exception {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		Uin uinEntity = buildUinEntity();
		UinBiometric biometric = new UinBiometric();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBioFileId("bio-1");
		biometric.setBiometricFileName("name");
		biometric.setBiometricFileHash("hash");
		uinEntity.setBiometrics(new ArrayList<>(List.of(biometric)));
		when(uinRepo.findWithBiometricsByUinHash(any())).thenReturn(Optional.of(uinEntity));
		stubCreateDraftCrypto();
		when(objectStoreHelper.getRidHash("REG123")).thenReturn("rid-hash");
		doThrow(new IdRepoAppException(IdRepoErrorConstants.DRAFT_OBJECT_COPY_FAILED.getErrorCode(),
				IdRepoErrorConstants.DRAFT_OBJECT_COPY_FAILED.getErrorMessage()))
				.when(objectStoreHelper).copyBiometricLiveToDraft(any(), any(), any());

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraftV2("REG123", "274390482564", true));

		assertEquals(IdRepoErrorConstants.DRAFT_OBJECT_COPY_FAILED.getErrorCode(), thrown.getErrorCode());
		verify(uinDraftRepo, never()).save(any());
		verify(idRepoServiceHelper, never()).generateUin();
	}

	@Test
	public void should_throwUinGenerationFailed_when_createDraftV2_generateUinFails() throws Exception {
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		when(idRepoServiceHelper.generateUin())
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.UIN_GENERATION_FAILED));

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraftV2("REG123", null, true));

		assertEquals(IdRepoErrorConstants.UIN_GENERATION_FAILED.getErrorCode(), thrown.getErrorCode());
		verify(uinDraftRepo, never()).save(any());
		verify(objectStoreHelper, never()).copyBiometricLiveToDraft(any(), any(), any());
	}

	@Test
	public void should_throwRidOlderThanLatestProcessed_when_createDraftV2_reprocessIsNotLatest() throws IdRepoAppException {
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(true);
		when(uinRepo.existsByRegId(any())).thenReturn(false);

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraftV2("REG123", null, true));

		assertEquals(IdRepoErrorConstants.RID_OLDER_THAN_LATEST_PROCESSED.getErrorCode(), thrown.getErrorCode());
		verify(uinDraftRepo, never()).save(any());
		verify(idRepoServiceHelper, never()).generateUin();
	}

	@Test
	public void should_throwNoRecordFound_when_createDraftV2_reprocessLiveUinMissing() throws IdRepoAppException {
		when(uinDraftRepo.existsByRegId(any())).thenReturn(false);
		when(uinHistoryRepo.existsByRegId(any())).thenReturn(true);
		when(uinRepo.existsByRegId(any())).thenReturn(true);
		when(uinRepo.findWithBiometricsByRegId("REG123")).thenReturn(Optional.empty());

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraftV2("REG123", null, true));

		assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), thrown.getErrorCode());
		verify(uinDraftRepo, never()).save(any());
		verify(idRepoServiceHelper, never()).generateUin();
	}

	@Test
	public void should_throwUnknownError_when_createDraftV2_unexpectedRuntimeException() {
		when(uinDraftRepo.existsByRegId(any())).thenThrow(new IllegalStateException("unexpected"));

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.createDraftV2("REG123", "274390482564", true));

		assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), thrown.getErrorCode());
		verify(uinDraftRepo, never()).save(any());
	}

	// ── updateDraftUinData — success ─────────────────────────────────────────

	@Test
	public void should_updateUinOnDraft_when_ridExists() throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		// getUinHash("274390482564") resolves to "1234_some-hash" under the stubs below —
		// align the draft's stored hash so the "restamp with different UIN" guard doesn't trip.
		draft.setUinHash("1234_some-hash");
		when(uinDraftRepo.findByRegId(anyString())).thenReturn(Optional.of(draft));
		when(uinRepo.existsByUinHash(any())).thenReturn(true);
		when(uinRepo.findByUinHash(any())).thenReturn(Optional.of(buildLiveUin()));
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(1234);
		when(uinEncryptSaltRepo.retrieveSaltById(anyInt())).thenReturn("YWJj");
		when(securityManager.encryptWithSalt(any(), any(), any())).thenReturn("encrypted".getBytes());
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hashSalt");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("some-hash");
		IdResponseDTO response = idRepoServiceImpl.updateDraftUinData("REG123", "274390482564");
		assertNotNull(response);
		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertEquals("1234_some-hash", draft.getUinHash());
		assertEquals("1234_encrypted", draft.getUin());
		verify(uinDraftRepo).flush();
		verify(uinDraftRepo, never()).save(any());
	}

	// Regression for a NullPointerException seen in production: a LOST-packet draft has
	// uinData populated by an earlier updateDraftV2 call (which never sets uinHash), so
	// the "already stamped" guard must key off uinHash being non-null, not uinData.
	@Test
	public void should_stampUin_when_draftHasUinDataButNoUinHash_lostPacketFirstStamp()
			throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		draft.setUinHash(null);
		when(uinDraftRepo.findByRegId(anyString())).thenReturn(Optional.of(draft));
		when(uinRepo.existsByUinHash(any())).thenReturn(true);
		when(uinRepo.findByUinHash(any())).thenReturn(Optional.of(buildLiveUin()));
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(1234);
		when(uinEncryptSaltRepo.retrieveSaltById(anyInt())).thenReturn("YWJj");
		when(securityManager.encryptWithSalt(any(), any(), any())).thenReturn("encrypted".getBytes());
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hashSalt");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("some-hash");

		IdResponseDTO response = idRepoServiceImpl.updateDraftUinData("REG123", "274390482564");

		assertNotNull(response);
		assertEquals("1234_some-hash", draft.getUinHash());
	}

	// Regression for the LOST-packet IDR-IDC-001 publish failure: when the ABIS-matched
	// resident's UIN is stamped onto the draft, missing demographic fields (e.g.
	// addressLine1) must be backfilled from the live Uin, while any field already present
	// on the draft (the resident's own submitted data) must be preserved on conflict.
	@Test
	public void should_backfillMissingFields_and_preserveDraftValues_onConflict_when_stampingUin()
			throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		draft.setUinHash(null);
		draft.setUinData("{\"UIN\":\"274390482564\",\"email\":\"draft@mosip.net\"}".getBytes());
		when(uinDraftRepo.findByRegId(anyString())).thenReturn(Optional.of(draft));
		when(uinRepo.existsByUinHash(any())).thenReturn(true);

		Uin liveUin = new Uin();
		liveUin.setUinData(
				"{\"UIN\":\"274390482564\",\"email\":\"live@mosip.net\",\"addressLine1\":\"live-address\"}"
						.getBytes());
		when(uinRepo.findByUinHash(any())).thenReturn(Optional.of(liveUin));

		when(securityManager.getSaltKeyForId(anyString())).thenReturn(1234);
		when(uinEncryptSaltRepo.retrieveSaltById(anyInt())).thenReturn("YWJj");
		when(securityManager.encryptWithSalt(any(), any(), any())).thenReturn("encrypted".getBytes());
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hashSalt");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("some-hash");

		idRepoServiceImpl.updateDraftUinData("REG123", "274390482564");

		Map<String, Object> mergedData = mapper.readValue(draft.getUinData(), new TypeReference<Map<String, Object>>() {});
		assertEquals("draft@mosip.net", mergedData.get("email"));
		assertEquals("live-address", mergedData.get("addressLine1"));
	}

	@Test
	public void should_throwRecordExists_when_updateDraftUinData_flushHitsUniqueConstraint()
			throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		draft.setUinHash(null);
		when(uinDraftRepo.findByRegId(anyString())).thenReturn(Optional.of(draft));
		when(uinRepo.existsByUinHash(any())).thenReturn(true);
		when(uinRepo.findByUinHash(any())).thenReturn(Optional.of(buildLiveUin()));
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(1234);
		when(uinEncryptSaltRepo.retrieveSaltById(anyInt())).thenReturn("YWJj");
		when(securityManager.encryptWithSalt(any(), any(), any())).thenReturn("encrypted".getBytes());
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hashSalt");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("some-hash");
		doThrow(new DataIntegrityViolationException("uk_uin_hash")).when(uinDraftRepo).flush();

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.updateDraftUinData("REG123", "274390482564"));

		assertEquals(IdRepoErrorConstants.RECORD_EXISTS.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_skipBackfill_when_updateDraftUinData_liveUinDataIsNull()
			throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		draft.setUinHash(null);
		byte[] originalUinData = draft.getUinData();
		when(uinDraftRepo.findByRegId(anyString())).thenReturn(Optional.of(draft));
		when(uinRepo.existsByUinHash(any())).thenReturn(true);
		Uin liveUin = Mockito.spy(new Uin());
		doReturn(null).when(liveUin).getUinData();
		when(uinRepo.findByUinHash(any())).thenReturn(Optional.of(liveUin));
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(1234);
		when(uinEncryptSaltRepo.retrieveSaltById(anyInt())).thenReturn("YWJj");
		when(securityManager.encryptWithSalt(any(), any(), any())).thenReturn("encrypted".getBytes());
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hashSalt");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("some-hash");

		IdResponseDTO response = idRepoServiceImpl.updateDraftUinData("REG123", "274390482564");

		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertEquals("1234_some-hash", draft.getUinHash());
		assertSame(originalUinData, draft.getUinData());
		verify(uinDraftRepo).flush();
	}

	// ── updateDraftV2 ────────────────────────────────────────────────────────

	@Test
	public void should_updateDraftV2_successfully() throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		IdRequestDTO request = new IdRequestDTO();
		request.setRequest(new RequestDTO());
		IdResponseDTO response = idRepoServiceImpl.updateDraftV2("1234567890", request);
		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertNull(response.getResponse().getIdentity());
		assertNull(response.getResponse().getDocuments());
		verify(uinDraftRepo).save(draft);
		verify(objectStoreHelper, never()).getRidHash(anyString());
	}

	@Test
	public void should_updateDraftV2_firstLostUpdate_when_uinDataAndUinHashAreNull()
			throws IdRepoAppException, IOException {
		UinDraft draft = new UinDraft();
		draft.setRegId("1234567890");
		draft.setStatusCode("DRAFT");
		draft.setUin(null);
		draft.setUinHash(null);
		draft.setUinData(null);
		draft.setBiometrics(new ArrayList<>());
		draft.setDocuments(new ArrayList<>());
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		when(securityManager.hash(any())).thenReturn("DATAHASH");
		IdRequestDTO request = identityUpdateRequest(Map.of("email", "lost@mosip.net"));

		IdResponseDTO response = idRepoServiceImpl.updateDraftV2("1234567890", request);

		assertNotNull(response);
		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertNull(draft.getUinHash());
		assertNotNull(draft.getUinData());
		Map<String, Object> stored = mapper.readValue(draft.getUinData(), new TypeReference<Map<String, Object>>() {});
		assertEquals("lost@mosip.net", stored.get("email"));
		verify(uinDraftRepo).save(draft);
		verify(identityUpdateTracker, never()).save(any());
	}

	@Test
	public void should_throwDraftUinDetailsNotFound_when_updateDraftV2_secondIdentityChange_withoutUinHash()
			throws IdRepoAppException {
		UinDraft draft = lostDraftWithUinData(Map.of("email", "first@mosip.net"));
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		IdRequestDTO request = identityUpdateRequest(Map.of("email", "second@mosip.net"));

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.updateDraftV2("1234567890", request));

		assertEquals(IdRepoErrorConstants.DRAFT_UIN_DETAILS_NOT_FOUND.getErrorCode(), thrown.getErrorCode());
		assertEquals(IdRepoErrorConstants.DRAFT_UIN_DETAILS_NOT_FOUND.getErrorMessage(), thrown.getErrorText());
		verify(uinDraftRepo, never()).save(any());
		verify(identityUpdateTracker, never()).findById(any());
		verify(identityUpdateTracker, never()).save(any());
	}

	@Test
	public void should_updateDraftV2_when_secondCallHasSameIdentity_withoutUinHash()
			throws IdRepoAppException {
		Map<String, Object> identity = Map.of("email", "lost@mosip.net");
		UinDraft draft = lostDraftWithUinData(identity);
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		when(securityManager.hash(any())).thenReturn("DATAHASH");
		IdRequestDTO request = identityUpdateRequest(identity);

		IdResponseDTO response = idRepoServiceImpl.updateDraftV2("1234567890", request);

		assertNotNull(response);
		assertEquals("DRAFTED", response.getResponse().getStatus());
		verify(uinDraftRepo).save(draft);
		verify(identityUpdateTracker, never()).save(any());
	}

	@Test
	public void should_updateDraftV2_when_secondCallHasDocumentsOnly_withoutUinHash()
			throws IdRepoAppException {
		UinDraft draft = lostDraftWithUinData(Map.of("email", "lost@mosip.net"));
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");
		IdRequestDTO request = new IdRequestDTO();
		RequestDTO req = new RequestDTO();
		req.setRegistrationId("1234567890");
		request.setRequest(req);

		IdResponseDTO response = idRepoServiceImpl.updateDraftV2("1234567890", request);

		assertNotNull(response);
		assertEquals("DRAFTED", response.getResponse().getStatus());
		verify(uinDraftRepo).save(draft);
		verify(identityUpdateTracker, never()).save(any());
	}

	@Test
	public void should_updateDraftV2_secondIdentityChange_when_uinHashIsPresent()
			throws IdRepoAppException {
		UinDraft draft = lostDraftWithUinData(Map.of("email", "first@mosip.net"));
		draft.setUinHash("123_some-hash");
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		ReflectionTestUtils.setField(idRepoServiceImpl, "identityUpdateTracker", identityUpdateTracker);
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		when(securityManager.hash(any())).thenReturn("DATAHASH");
		IdRequestDTO request = identityUpdateRequest(Map.of("email", "second@mosip.net"));

		IdResponseDTO response = idRepoServiceImpl.updateDraftV2("1234567890", request);

		assertNotNull(response);
		assertEquals("DRAFTED", response.getResponse().getStatus());
		verify(uinDraftRepo).save(draft);
		verify(identityUpdateTracker).save(any());
	}

	private UinDraft lostDraftWithUinData(Map<String, Object> identity) {
		UinDraft draft = new UinDraft();
		draft.setRegId("1234567890");
		draft.setStatusCode("DRAFT");
		draft.setUin(null);
		draft.setUinHash(null);
		draft.setUinData(convertIdentity(identity));
		draft.setBiometrics(new ArrayList<>());
		draft.setDocuments(new ArrayList<>());
		return draft;
	}

	private IdRequestDTO identityUpdateRequest(Map<String, Object> identity) {
		IdRequestDTO request = new IdRequestDTO();
		RequestDTO req = new RequestDTO();
		req.setRegistrationId("1234567890");
		req.setIdentity(new HashMap<>(identity));
		request.setRequest(req);
		return request;
	}

	private byte[] convertIdentity(Map<String, Object> identity) {
		try {
			return mapper.writeValueAsBytes(identity);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	public void should_throwNoRecordFound_when_updateDraftV2_ridDoesNotExist() {
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.empty());
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.updateDraftV2("REG123", new IdRequestDTO()));
		assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_throwDatabaseAccessError_when_updateDraftV2_jdbcException() {
		when(uinDraftRepo.findByRegId(any())).thenThrow(JDBCConnectionException.class);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.updateDraftV2("REG123", new IdRequestDTO()));
		assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_throwUnknownError_when_updateDraftV2_jsonException() {
		when(uinDraftRepo.findByRegId(any())).thenThrow(InvalidJsonException.class);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.updateDraftV2("REG123", new IdRequestDTO()));
		assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), thrown.getErrorCode());
	}

	// ── publishDraftV2 ───────────────────────────────────────────────────────

	@Test
	public void should_throwNoRecordFound_when_publishDraftV2_ridDoesNotExist() {
		ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.empty());
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.publishDraftV2("REG123"));
		assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_throwDraftUinDetailsNotFound_when_publishDraftV2_uinIsNull()
			throws IOException, IdRepoAppException {
		ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
		UinDraft draft = buildMinimalDraft();
		draft.setUin(null);
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.publishDraftV2("1234567890"));
		assertEquals(IdRepoErrorConstants.DRAFT_UIN_DETAILS_NOT_FOUND.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper, never()).moveAllDraftBiometricsToLive(anyString(), anyString());
		verify(objectStoreHelper, never()).moveAllDraftDemographicsToLive(anyString(), anyString());
	}

	@Test
	public void should_throwDraftUinDetailsNotFound_when_publishDraftV2_uinHashIsNull() throws IOException {
		ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
		UinDraft draft = buildMinimalDraft();
		draft.setUinHash(null);
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.publishDraftV2("1234567890"));
		assertEquals(IdRepoErrorConstants.DRAFT_UIN_DETAILS_NOT_FOUND.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_throwDraftUinDetailsNotFound_when_publishDraftV2_uinDataIsNull() throws IOException {
		ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
		UinDraft draft = buildMinimalDraft();
		draft.setUinData(null);
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.publishDraftV2("1234567890"));
		assertEquals(IdRepoErrorConstants.DRAFT_UIN_DETAILS_NOT_FOUND.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_throwDatabaseAccessError_when_publishDraftV2_jdbcException() {
		ReflectionTestUtils.setField(idRepoServiceImpl, "anonymousProfileHelper", anonymousProfileHelper);
		when(uinDraftRepo.findByRegId(any())).thenThrow(JDBCConnectionException.class);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.publishDraftV2("REG123"));
		assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
	}

	// ── extractBiometricsV2 ──────────────────────────────────────────────────

	@Test
	public void should_returnEmptyResponse_when_extractBiometricsV2_extractionFormatsEmpty() throws IdRepoAppException {
		IdResponseDTO response = idRepoServiceImpl.extractBiometricsV2("1234567890", new HashMap<>());
		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertNull(response.getResponse().getIdentity());
		assertNull(response.getResponse().getDocuments());
		verify(uinDraftRepo, never()).findByRegId(any());
		verify(objectStoreHelper, never()).getRidHash(anyString());
	}

	@Test
	public void should_throwNoRecordFound_when_extractBiometricsV2_ridDoesNotExist()
			throws IdRepoAppException {
		Map<String, String> formats = new HashMap<>();
		formats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.empty());
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.extractBiometricsV2("REG123", formats));
		assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper, never()).getRidHash(anyString());
	}

	@Test
	public void should_throwDatabaseAccessError_when_extractBiometricsV2_jdbcException()
			throws IdRepoAppException {
		Map<String, String> formats = new HashMap<>();
		formats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		when(uinDraftRepo.findByRegId(any())).thenThrow(JDBCConnectionException.class);
		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.extractBiometricsV2("REG123", formats));
		assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper, never()).getRidHash(anyString());
	}

	// ── publishDraftV2 success-path ──────────────────────────────────────────

	private void stubPublishDraftV2(UinDraft draft) throws IdRepoAppException {
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		when(uinEncryptSaltRepo.getOne(anyInt())).thenReturn(uinEncryptSalt);
		when(uinEncryptSalt.getSalt()).thenReturn("dGVzdA==");
		when(securityManager.decryptWithSalt(any(), any(), any())).thenReturn("274390482564".getBytes());
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(123);
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hashSalt");
		when(securityManager.hashwithSalt(any(), any()))
				.thenReturn("5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		ReflectionTestUtils.setField(idRepoServiceImpl, "activeStatus", "ACTIVATED");
		when(uinEncryptSaltRepo.retrieveSaltById(anyInt())).thenReturn("encSalt");
		when(securityManager.hash(any())).thenReturn("DATAHASH");
		Uin savedUin = new Uin();
		savedUin.setUinRefId("ref-id-123");
		savedUin.setUin("encrypted-uin");
		savedUin.setUinData("{}".getBytes());
		savedUin.setStatusCode("ACTIVATED");
		when(uinRepo.save(any())).thenReturn(savedUin);
		when(securityManager.getIdHashWithSaltModuloByPlainIdHash(anyString(), any())).thenReturn("IDHASH");
		when(anonymousProfileHelper.setRegId(anyString())).thenReturn(anonymousProfileHelper);
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");
		when(vidDraftHelper.generateDraftVid(any())).thenReturn("VID-1");
		// deleteDraftDbRecords uses executeWithoutResult; invoke the callback so deletes run.
		doAnswer(invocation -> {
			java.util.function.Consumer<org.springframework.transaction.TransactionStatus> action = invocation.getArgument(0);
			action.accept(null);
			return null;
		}).when(transactionTemplate).executeWithoutResult(any());
	}

	@Test
	public void should_publishDraftV2_moveBiometricAndDemographic_with_ridHashSrc_and_uinHashDest()
			throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		draft.setUin("1_YWJj");
		stubPublishDraftV2(draft);

		IdResponseDTO response = idRepoServiceImpl.publishDraftV2("1234567890");

		assertNotNull(response);
		assertEquals("ACTIVATED", response.getResponse().getStatus());
		assertEquals(Map.of("vid", "VID-1"), response.getMetadata());
		String expectedDest = "5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7";
		InOrder inOrder = inOrder(objectStoreHelper, uinRepo, uinBiometricDraftRepo, uinDocumentDraftRepo, uinDraftRepo);
		inOrder.verify(objectStoreHelper).moveAllDraftBiometricsToLive("RID_HASH_TEST", expectedDest);
		inOrder.verify(objectStoreHelper).moveAllDraftDemographicsToLive("RID_HASH_TEST", expectedDest);
		inOrder.verify(uinRepo).save(any());
		inOrder.verify(uinBiometricDraftRepo).deleteByRegId("1234567890");
		inOrder.verify(uinDocumentDraftRepo).deleteByRegId("1234567890");
		inOrder.verify(uinDraftRepo).deleteByRegId("1234567890");
		verify(vidDraftHelper).generateDraftVid(any());
		verify(vidDraftHelper).activateDraftVid(any());
	}

	@Test
	public void should_publishDraftV2_moveFiles_before_deleteDbRecords()
			throws IdRepoAppException, IOException {
		UinDraft draft = buildMinimalDraft();
		draft.setUin("1_YWJj");
		stubPublishDraftV2(draft);

		idRepoServiceImpl.publishDraftV2("1234567890");

		// Files must move BEFORE live write and BEFORE draft DB delete.
		InOrder inOrder = inOrder(objectStoreHelper, uinBiometricDraftRepo, uinDocumentDraftRepo, uinDraftRepo);
		inOrder.verify(objectStoreHelper).moveAllDraftBiometricsToLive(anyString(), anyString());
		inOrder.verify(objectStoreHelper).moveAllDraftDemographicsToLive(anyString(), anyString());
		inOrder.verify(uinBiometricDraftRepo).deleteByRegId("1234567890");
		inOrder.verify(uinDocumentDraftRepo).deleteByRegId("1234567890");
		inOrder.verify(uinDraftRepo).deleteByRegId("1234567890");
	}

	@Test
	public void should_notWriteLiveOrDeleteDraft_when_publishDraftV2_s3MoveFails()
			throws IOException, IdRepoAppException {
		UinDraft draft = buildMinimalDraft();
		draft.setUin("1_YWJj");
		stubPublishDraftV2(draft);
		doThrow(new IdRepoAppException(IdRepoErrorConstants.DRAFT_OBJECT_MOVE_FAILED.getErrorCode(),
				IdRepoErrorConstants.DRAFT_OBJECT_MOVE_FAILED.getErrorMessage()))
				.when(objectStoreHelper).moveAllDraftBiometricsToLive(anyString(), anyString());

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class,
				() -> idRepoServiceImpl.publishDraftV2("1234567890"));

		assertEquals(IdRepoErrorConstants.DRAFT_OBJECT_MOVE_FAILED.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper).moveAllDraftBiometricsToLive(anyString(), anyString());
		verify(objectStoreHelper, never()).moveAllDraftDemographicsToLive(anyString(), anyString());
		verify(uinRepo, never()).save(any());
		verify(vidDraftHelper, never()).generateDraftVid(any());
		verify(vidDraftHelper, never()).activateDraftVid(any());
		verify(uinDraftRepo, never()).deleteByRegId(anyString());
		verify(uinBiometricDraftRepo, never()).deleteByRegId(anyString());
		verify(uinDocumentDraftRepo, never()).deleteByRegId(anyString());
	}

	@Test
	public void should_notWriteLiveOrDeleteDraft_when_publishDraftV2_demographicMoveFails_afterBioMove()
			throws IOException, IdRepoAppException {
		UinDraft draft = buildMinimalDraft();
		draft.setUin("1_YWJj");
		stubPublishDraftV2(draft);
		doThrow(new IdRepoAppException(IdRepoErrorConstants.DRAFT_OBJECT_MOVE_FAILED.getErrorCode(),
				IdRepoErrorConstants.DRAFT_OBJECT_MOVE_FAILED.getErrorMessage()))
				.when(objectStoreHelper).moveAllDraftDemographicsToLive(anyString(), anyString());

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class,
				() -> idRepoServiceImpl.publishDraftV2("1234567890"));

		assertEquals(IdRepoErrorConstants.DRAFT_OBJECT_MOVE_FAILED.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper).moveAllDraftBiometricsToLive(anyString(), anyString());
		verify(uinRepo, never()).save(any());
		verify(vidDraftHelper, never()).generateDraftVid(any());
		verify(uinDraftRepo, never()).deleteByRegId(anyString());
	}

	@Test
	public void should_notDeleteDraft_when_publishDraftV2_generateVidFails_afterS3()
			throws IOException, IdRepoAppException {
		UinDraft draft = buildMinimalDraft();
		draft.setUin("1_YWJj");
		stubPublishDraftV2(draft);
		when(vidDraftHelper.generateDraftVid(any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.VID_GENERATION_FAILED));

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class,
				() -> idRepoServiceImpl.publishDraftV2("1234567890"));

		assertEquals(IdRepoErrorConstants.VID_GENERATION_FAILED.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper).moveAllDraftBiometricsToLive(anyString(), anyString());
		verify(objectStoreHelper).moveAllDraftDemographicsToLive(anyString(), anyString());
		verify(uinRepo, never()).save(any());
		verify(vidDraftHelper, never()).activateDraftVid(any());
		verify(uinDraftRepo, never()).deleteByRegId(anyString());
	}

	@Test
	public void should_notDeleteDraft_when_publishDraftV2_activateVidFails()
			throws IOException, IdRepoAppException {
		UinDraft draft = buildMinimalDraft();
		draft.setUin("1_YWJj");
		stubPublishDraftV2(draft);
		when(vidDraftHelper.generateDraftVid(any())).thenReturn("VID-1");
		doThrow(new IdRepoAppException(IdRepoErrorConstants.VID_GENERATION_FAILED))
				.when(vidDraftHelper).activateDraftVid("VID-1");

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class,
				() -> idRepoServiceImpl.publishDraftV2("1234567890"));

		assertEquals(IdRepoErrorConstants.VID_GENERATION_FAILED.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper).moveAllDraftBiometricsToLive(anyString(), anyString());
		verify(objectStoreHelper).moveAllDraftDemographicsToLive(anyString(), anyString());
		verify(uinRepo).save(any());
		verify(uinDraftRepo, never()).deleteByRegId(anyString());
		verify(uinBiometricDraftRepo, never()).deleteByRegId(anyString());
		verify(uinDocumentDraftRepo, never()).deleteByRegId(anyString());
	}

	@Test
	public void should_notDeleteDraft_when_publishDraftV2_liveWriteFails_afterS3()
			throws IOException, IdRepoAppException {
		UinDraft draft = buildMinimalDraft();
		draft.setUin("1_YWJj");
		stubPublishDraftV2(draft);
		when(uinRepo.save(any())).thenThrow(JDBCConnectionException.class);

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class,
				() -> idRepoServiceImpl.publishDraftV2("1234567890"));

		assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper).moveAllDraftBiometricsToLive(anyString(), anyString());
		verify(objectStoreHelper).moveAllDraftDemographicsToLive(anyString(), anyString());
		verify(uinDraftRepo, never()).deleteByRegId(anyString());
	}

	@Test
	public void should_keepLiveWrite_when_publishDraftV2_draftDeleteFails_afterS3AndCommit()
			throws IOException, IdRepoAppException {
		UinDraft draft = buildMinimalDraft();
		draft.setUin("1_YWJj");
		stubPublishDraftV2(draft);
		doThrow(JDBCConnectionException.class)
				.when(uinDraftRepo).deleteByRegId(anyString());

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class,
				() -> idRepoServiceImpl.publishDraftV2("1234567890"));

		assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
		verify(objectStoreHelper).moveAllDraftBiometricsToLive(anyString(), anyString());
		verify(objectStoreHelper).moveAllDraftDemographicsToLive(anyString(), anyString());
		verify(uinRepo).save(any());
		verify(uinDraftRepo).deleteByRegId("1234567890");
	}

	@Test
	public void should_updateExistingLiveUin_and_notCreateSecondRow_when_publishDraftV2_retried()
			throws Exception {
		UinDraft draft = buildMinimalDraft();
		draft.setUin("1_YWJj");
		stubPublishDraftV2(draft);
		when(uinRepo.existsByUinHash(any())).thenReturn(true);
		Uin existing = new Uin();
		existing.setUinRefId("ref-id-123");
		existing.setStatusCode("ACTIVATED");
		existing.setUinData("{}".getBytes());
		IdRepoDraftServiceImpl spy = Mockito.spy(idRepoServiceImpl);
		doReturn(existing).when(spy).updateIdentity(any(), anyString());

		IdResponseDTO response = spy.publishDraftV2("1234567890");

		assertNotNull(response);
		assertEquals("ACTIVATED", response.getResponse().getStatus());
		assertNull(response.getMetadata());
		verify(spy).updateIdentity(any(), anyString());
		verify(vidDraftHelper, never()).generateDraftVid(any());
		verify(vidDraftHelper, never()).activateDraftVid(any());
		verify(objectStoreHelper).moveAllDraftBiometricsToLive(anyString(), anyString());
		verify(objectStoreHelper).moveAllDraftDemographicsToLive(anyString(), anyString());
		verify(uinDraftRepo).deleteByRegId("1234567890");
	}

	@Test
	public void should_addIdentity_when_publishDraftV2_generateDraftVidReturnsNull()
			throws Exception {
		UinDraft draft = buildMinimalDraft();
		draft.setUin("1_YWJj");
		stubPublishDraftV2(draft);
		when(uinRepo.existsByUinHash(any())).thenReturn(false);
		when(vidDraftHelper.generateDraftVid(any())).thenReturn(null);
		Uin saved = new Uin();
		saved.setUinRefId("ref-id-123");
		saved.setStatusCode("ACTIVATED");
		saved.setUinData("{}".getBytes());
		IdRepoDraftServiceImpl spy = Mockito.spy(idRepoServiceImpl);
		doReturn(saved).when(spy).addIdentity(any(), anyString());

		IdResponseDTO response = spy.publishDraftV2("1234567890");

		assertNotNull(response);
		assertEquals("ACTIVATED", response.getResponse().getStatus());
		assertNull(response.getMetadata());
		verify(spy).addIdentity(any(), anyString());
		verify(spy, never()).updateIdentity(any(), anyString());
		verify(vidDraftHelper).generateDraftVid(any());
		verify(vidDraftHelper).activateDraftVid(null);
	}

	@Test
	public void should_ignoreDuplicateLiveBioRows_when_publishDraftV2_retriedAfterPartialWrite()
			throws IOException, IdRepoAppException {
		UinDraft draft = buildMinimalDraft();
		draft.setUin("1_YWJj");
		stubPublishDraftV2(draft);
		when(uinBiometricRepo.saveAll(any())).thenThrow(new DataIntegrityViolationException("uk_uinb"));

		IdResponseDTO response = idRepoServiceImpl.publishDraftV2("1234567890");

		assertNotNull(response);
		assertEquals("ACTIVATED", response.getResponse().getStatus());
		verify(uinRepo).save(any());
		verify(uinBiometricRepo).saveAll(any());
		verify(uinDocumentRepo).saveAll(any());
		verify(uinDraftRepo).deleteByRegId("1234567890");
	}

	@Test
	public void should_ignoreDuplicateLiveDocRows_when_publishDraftV2_retriedAfterPartialWrite()
			throws IOException, IdRepoAppException {
		UinDraft draft = buildMinimalDraft();
		draft.setUin("1_YWJj");
		stubPublishDraftV2(draft);
		when(uinDocumentRepo.saveAll(any())).thenThrow(new DataIntegrityViolationException("uk_uind"));

		IdResponseDTO response = idRepoServiceImpl.publishDraftV2("1234567890");

		assertNotNull(response);
		assertEquals("ACTIVATED", response.getResponse().getStatus());
		verify(uinRepo).save(any());
		verify(uinBiometricRepo).saveAll(any());
		verify(uinDocumentRepo).saveAll(any());
		verify(uinDraftRepo).deleteByRegId("1234567890");
	}

	// ── extractBiometricsV2 success-path ─────────────────────────────────────

	@Test
	public void should_extractBiometricsV2_readFromDraftPath_using_ridHash()
			throws IdRepoAppException, IOException {
		Map<String, String> formats = new HashMap<>();
		formats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");

		IdResponseDTO response = idRepoServiceImpl.extractBiometricsV2("1234567890", formats);

		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertNull(response.getResponse().getIdentity());
		assertNull(response.getResponse().getDocuments());
		verify(objectStoreHelper).getDraftBiometricObject("RID_HASH_TEST", "1234");
		verify(objectStoreHelper, never()).getBiometricObject(anyString(), anyString());
		verify(objectStoreHelper, never()).deleteBiometricObject(anyString(), anyString());
	}

	@Test
	public void should_extractBiometricsV2_deleteExistingDraftExtractionFile_beforeReExtracting()
			throws IdRepoAppException, IOException {
		Map<String, String> formats = new HashMap<>();
		formats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");

		idRepoServiceImpl.extractBiometricsV2("1234567890", formats);

		// Any previously extracted derivative for this format must be deleted from the
		// draft path before a fresh extraction is attempted, so a stale file can never
		// be mistaken for a freshly extracted one.
		InOrder inOrder = inOrder(objectStoreHelper);
		inOrder.verify(objectStoreHelper).deleteDraftBiometricObject("RID_HASH_TEST", "1234.finger.fingerFormat");
		inOrder.verify(objectStoreHelper).getDraftBiometricObject("RID_HASH_TEST", "1234");
	}

	@Test
	public void should_extractBiometricsV2_wrapDataAccessException_asDatabaseAccessError()
			throws IdRepoAppException, IOException {
		Map<String, String> formats = new HashMap<>();
		formats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");
		// getDraftBiometricObject is evaluated inside extractBiometricsDraftV2's per-file
		// try block (as an argument to the proxyService call), so a DB failure surfacing
		// here exercises the same DataAccessException|JDBCConnectionException catch as a
		// real Hibernate-backed failure would.
		when(objectStoreHelper.getDraftBiometricObject(anyString(), anyString()))
				.thenThrow(JDBCConnectionException.class);

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.extractBiometricsV2("1234567890", formats));
		assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_extractBiometricsV2_wrapGenericException_asBioExtractionError()
			throws IdRepoAppException, IOException {
		Map<String, String> formats = new HashMap<>();
		formats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");
		when(objectStoreHelper.getDraftBiometricObject(anyString(), anyString()))
				.thenThrow(new RuntimeException("unexpected"));

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.extractBiometricsV2("1234567890", formats));
		assertEquals(IdRepoErrorConstants.BIO_EXTRACTION_ERROR.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_extractBiometricsV2_propagateIdRepoAppException_fromDraftRead()
			throws IdRepoAppException, IOException {
		Map<String, String> formats = new HashMap<>();
		formats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");
		when(objectStoreHelper.getDraftBiometricObject(anyString(), anyString()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.BIO_EXTRACTION_ERROR));

		IdRepoAppException thrown = assertThrows(IdRepoAppException.class, () ->
				idRepoServiceImpl.extractBiometricsV2("1234567890", formats));
		assertEquals(IdRepoErrorConstants.BIO_EXTRACTION_ERROR.getErrorCode(), thrown.getErrorCode());
	}

	@Test
	public void should_extractBiometricsV2_skipExtraction_whenDraftHasNoBiometrics()
			throws IdRepoAppException, IOException {
		Map<String, String> formats = new HashMap<>();
		formats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		UinDraft draft = buildMinimalDraft();
		draft.setBiometrics(null);
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");

		idRepoServiceImpl.extractBiometricsV2("1234567890", formats);

		verify(objectStoreHelper, never()).deleteDraftBiometricObject(anyString(), anyString());
		verify(objectStoreHelper, never()).getDraftBiometricObject(anyString(), anyString());
	}

	@Test
	public void should_extractBiometricsV2_skipExtraction_whenDraftBiometricsAreEmpty()
			throws IdRepoAppException, IOException {
		Map<String, String> formats = new HashMap<>();
		formats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		UinDraft draft = buildMinimalDraft();
		draft.setBiometrics(new ArrayList<>());
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");

		IdResponseDTO response = idRepoServiceImpl.extractBiometricsV2("1234567890", formats);

		assertEquals("DRAFTED", response.getResponse().getStatus());
		verify(objectStoreHelper).getRidHash("1234567890");
		verify(objectStoreHelper, never()).deleteDraftBiometricObject(anyString(), anyString());
		verify(objectStoreHelper, never()).getDraftBiometricObject(anyString(), anyString());
	}

	@Test
	public void should_extractBiometricsV2_continue_when_deleteExistingDraftExtractionFails()
			throws IdRepoAppException, IOException {
		Map<String, String> formats = new HashMap<>();
		formats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		UinDraft draft = buildMinimalDraft();
		when(uinDraftRepo.findByRegId(any())).thenReturn(Optional.of(draft));
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");
		doThrow(new RuntimeException("stale-file-delete-failed"))
				.when(objectStoreHelper).deleteDraftBiometricObject(anyString(), anyString());

		IdResponseDTO response = idRepoServiceImpl.extractBiometricsV2("1234567890", formats);

		assertEquals("DRAFTED", response.getResponse().getStatus());
		verify(objectStoreHelper).deleteDraftBiometricObject("RID_HASH_TEST", "1234.finger.fingerFormat");
		verify(objectStoreHelper).getDraftBiometricObject("RID_HASH_TEST", "1234");
	}

	private void stubDocumentUpload() throws Exception {
		when(cbeffUtil.validateXML(any())).thenReturn(true);
		when(securityManager.hash(any()))
				.thenReturn("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
	}

	private RequestDTO documentUpdateRequest(byte[] identityBytes) throws IOException {
		RequestDTO req = new RequestDTO();
		req.setIdentity(mapper.readValue(identityBytes, Object.class));
		DocumentsDTO bio = new DocumentsDTO();
		bio.setCategory("individualBiometrics");
		bio.setValue(Base64.getEncoder().encodeToString("text biomterics".getBytes()));
		DocumentsDTO demo = new DocumentsDTO();
		demo.setCategory("proofOfIdentity");
		demo.setValue(Base64.getEncoder().encodeToString("pdf-bytes".getBytes()));
		req.setDocuments(List.of(bio, demo));
		return req;
	}

	private UinDraft captureSavedDraft() {
		ArgumentCaptor<UinDraft> captor = ArgumentCaptor.forClass(UinDraft.class);
		verify(uinDraftRepo).save(captor.capture());
		return captor.getValue();
	}

	private void assertDraftedResponse(IdResponseDTO response) {
		assertNotNull(response);
		assertNotNull(response.getResponse());
		assertEquals("DRAFTED", response.getResponse().getStatus());
		assertNull(response.getResponse().getIdentity());
		assertNull(response.getResponse().getDocuments());
	}

	private void stubDraftObjectStoreReads() throws IdRepoAppException {
		when(objectStoreHelper.getRidHash(anyString())).thenReturn("RID_HASH_TEST");
		when(objectStoreHelper.getDraftBiometricObject(eq("RID_HASH_TEST"), eq("1234")))
				.thenReturn("cbeff-bytes".getBytes());
		when(proxyService.getBiometricsForRequestedFormatsDraft(any(), any(), any(), any()))
				.thenReturn("extracted-cbeff".getBytes());
		when(objectStoreHelper.getDraftDemographicObject(eq("RID_HASH_TEST"), eq("1236")))
				.thenReturn("doc-bytes".getBytes());
	}

	private void stubCreateDraftCrypto() throws IdRepoAppException {
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(1234);
		when(uinEncryptSaltRepo.retrieveSaltById(anyInt())).thenReturn("YWJj");
		when(securityManager.encryptWithSalt(any(), any(), any())).thenReturn("encrypted".getBytes());
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hashSalt");
		when(securityManager.hashwithSalt(any(), any())).thenReturn("some-hash");
	}

	private Uin buildUinEntity() throws IOException, NoSuchAlgorithmException {
		Uin uinEntity = new Uin();
		String uinHash = DatatypeConverter
				.printHexBinary(MessageDigest.getInstance("SHA-256").digest("274390482564".getBytes())).toUpperCase();
		uinEntity.setUinHash("123_" + uinHash);
		uinEntity.setUin("274390482564");
		uinEntity.setUinData("274390482564".getBytes());
		uinEntity.setUinDataHash(uinHash);
		uinEntity.setStatusCode("ACTIVATED");
		uinEntity.setBiometrics(new ArrayList<>());
		uinEntity.setDocuments(new ArrayList<>());
		return uinEntity;
	}

	private UinDraft buildMinimalDraft() throws IOException {
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		uin.setUinHash("123_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		uin.setRegId("1234567890");
		String identityData = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		uin.setUinData(identityData.getBytes());
		uin.setStatusCode("DRAFTED");
		UinBiometricDraft biometric = new UinBiometricDraft();
		biometric.setBiometricFileType("individualBiometrics");
		biometric.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometric.setBioFileId("1234");
		biometric.setBiometricFileName("name");
		uin.setBiometrics(new ArrayList<>(List.of(biometric)));
		UinDocumentDraft document = new UinDocumentDraft();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocHash("3A6EB0790F39AC87C94F3856B2DD2C5D110E6811602261A9A923D3BB23ADC8B7");
		document.setDocId("1236");
		document.setDocName("name");
		uin.setDocuments(new ArrayList<>(List.of(document)));
		return uin;
	}

	// Live Uin match backing updateDraftUinData's backfill merge — same identity data as the
	// draft's own, so the merge is a no-op and existing assertions stay unaffected.
	private Uin buildLiveUin() throws IOException {
		Uin uin = new Uin();
		String identityData = IOUtils.toString(
				this.getClass().getClassLoader().getResourceAsStream("identity-data.json"), StandardCharsets.UTF_8);
		uin.setUinData(identityData.getBytes());
		return uin;
	}

	/**
	 * Widens {@code getBiometricsForRequestedFormatsDraft} from protected to public
	 * so this test class can stub and verify it. The production method stays
	 * protected; Java only allows a direct call from the same package or a subclass.
	 */
	static class TestableProxyService extends IdRepoProxyServiceImpl {
		@Override
		public byte[] getBiometricsForRequestedFormatsDraft(String ridHash, String fileName,
				Map<String, String> extractionFormats, byte[] originalData) throws IdRepoAppException {
			return super.getBiometricsForRequestedFormatsDraft(ridHash, fileName, extractionFormats, originalData);
		}
	}
}
