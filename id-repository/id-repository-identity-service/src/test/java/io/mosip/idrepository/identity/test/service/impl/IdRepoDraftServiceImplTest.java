package io.mosip.idrepository.identity.test.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.FACE_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.FINGER_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IRIS_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.MOSIP_KERNEL_IDREPO_JSON_PATH;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.dto.DocumentsDTO;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.RequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
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

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
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
	IdRepoServiceImpl service;

	@Mock
	IdRepoSecurityManager securityManager;

	@Mock
	private UinBiometricHistoryRepo uinBioHRepo;

	@Mock
	private UinDocumentHistoryRepo uinDocHRepo;

	/** The env. */
	@Autowired
	private EnvUtil env;

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
	public void setup() {
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinPath", uinPath);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
	}
	
	@Test
	public void testHasDraft() throws IdRepoAppException {
		boolean flag = idRepoServiceImpl.hasDraft("qsdggtresxcv");
		assertFalse(flag);
	}

	@Ignore
	@Test
	public void testCreateDraft() throws IdRepoAppException {
		EnvUtil.setIdrepoSaltKeyLength(12);
		IdResponseDTO idresponse = idRepoServiceImpl.createDraft("1234567890","274390482564");
		assertNull(idresponse);
	}

	@Test
	public void testGenerateIdentityObject() {
		Object uin1 = "274390482564";
		Object uin = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "generateIdentityObject",uin1 );
		assertNotNull(uin);
	}
	
	@Ignore
	@Test
	public void testGenerateUin() {
		String uin = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "generateUin");
		assertNotNull(uin);
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testUpdateDraft() throws IdRepoAppException {
		IdRequestDTO request = new IdRequestDTO();
		String registrationId = "1234567890";
		IdResponseDTO response = idRepoServiceImpl.updateDraft(registrationId, request);
		assertNull(response);
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

		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		ReflectionTestUtils.setField(idRepoServiceImpl, "uinPath", uinPath);
		request.setRequest(req);
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "updateDemographicData",request,draft );
		
	}
	
	@Test
	@Ignore
	public void testUpdateDocuments() {
		RequestDTO req = new RequestDTO();
		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("individualBiometrics");
		doc1.setValue("text biomterics");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);
		req.setDocuments(docList);
		UinDraft draft =  new UinDraft();
		draft.setUin("274390482564");
		byte[] uinData = "274390482564".getBytes();
		draft.setUinData(uinData);
		UinDocumentDraft docs = new UinDocumentDraft();
		docs.setDocHash(securityManager.hash(doc1.getValue().getBytes()));
		List<UinDocumentDraft> draftDocList = new ArrayList<UinDocumentDraft>();
		draftDocList.add(docs);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		draft.setDocuments(draftDocList);
		draft.setUinDataHash(securityManager.hash(uinData));
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "updateDocuments",req,draft );
	}
	
	@Test
	@Ignore
	public void testConstructIdResponse() {
		byte[] uinData = "274390482564".getBytes();
		DocumentsDTO doc1 = new DocumentsDTO();
		doc1.setCategory("individualBiometrics");
		doc1.setValue("text biomterics");
		List<DocumentsDTO> docList = new ArrayList<>();
		docList.add(doc1);
		ReflectionTestUtils.setField(idRepoServiceImpl, "mapper", mapper);
		IdResponseDTO response = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "constructIdResponse",uinData,"success",docList,"1234567890" );
		assertNotNull(response);
	}
	
	@Test 
	public void testGetModalityForFormat() {
		String response = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "getModalityForFormat","1234567890" );
		assertNotNull(response);
	}
	
	@Test
	@Ignore
	public void testExtractAndGetCombinedCbeff() {
		String uinHash = securityManager.hash("274390482564".getBytes());
		String bioFileId = "1234";
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		byte[] response = ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "extractAndGetCombinedCbeff",uinHash,bioFileId,extractionFormats);
		assertNotNull(response);
	}
	
	@Test
	public void testdeleteExistingExtractedBioData() {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		String uinHash = securityManager.hash("274390482564".getBytes());
		UinBiometricDraft bioDraft =  new UinBiometricDraft();
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		bioDraft.setUin(uin);
		bioDraft.setBioFileId("1234");
		bioDraft.setBiometricFileName("Finger");
		bioDraft.setBiometricFileType("Finger");	
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "deleteExistingExtractedBioData",extractionFormats,uinHash,bioDraft);	
	}
	
//	@Test(expected = IdRepoAppException.class)
	@Test
	@Ignore
	public void testExtractBiometricsDraft() throws IdRepoAppException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		List<UinBiometricDraft> biometrics = new ArrayList<UinBiometricDraft>();
		UinBiometricDraft biometric1 = new UinBiometricDraft();
		biometric1.setBioFileId("1234");
		biometric1.setUin(uin);
		biometric1.setBiometricFileName("Finger");
		biometrics.add(biometric1);
		uin.setBiometrics(biometrics);
		ReflectionTestUtils.invokeMethod(idRepoServiceImpl, "extractBiometricsDraft",extractionFormats,uin);			
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testExtractBiometricswithException() throws IdRepoAppException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		IdResponseDTO response = idRepoServiceImpl.extractBiometrics("1234567890", extractionFormats);
		assertNotNull(response);
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testExtractBiometrics() throws IdRepoAppException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		String uinHash = securityManager.hash("274390482564".getBytes());
		uin.setUinHash(uinHash);
		uin.setRegId("1234567890");
		uin.setUinData("274390482564".getBytes());
		uin.setUinDataHash(securityManager.hash("274390482564".getBytes()));
		Optional<UinDraft> uinOpt =Optional.of(uin);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);;
 		IdResponseDTO response = idRepoServiceImpl.extractBiometrics("1234567890", extractionFormats);
		assertNotNull(response);
	}
	
	@Test(expected = IdRepoAppException.class)
	public void testGetDraftwithException() throws IdRepoAppException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		IdResponseDTO response = idRepoServiceImpl.getDraft("1234567890", extractionFormats);
		assertNotNull(response);
	}
	
	@Test
	@Ignore
	public void testGetDraft() throws IdRepoAppException {
		Map<String, String> extractionFormats = new HashMap<>();
		extractionFormats.put(FINGER_EXTRACTION_FORMAT, "fingerFormat");
		extractionFormats.put(IRIS_EXTRACTION_FORMAT, "irisFormat");
		extractionFormats.put(FACE_EXTRACTION_FORMAT, "faceFormat");
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		String uinHash = securityManager.hash("274390482564".getBytes());
		uin.setUinHash(uinHash);
		uin.setRegId("1234567890");
		uin.setUinData("274390482564".getBytes());
		uin.setUinDataHash(securityManager.hash("274390482564".getBytes()));
		Optional<UinDraft> uinOpt =Optional.of(uin);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);
 		IdResponseDTO response = idRepoServiceImpl.getDraft("1234567890", extractionFormats);
		assertNotNull(response);
	}
	
	@Test
	public void testDiscardDraft() throws IdRepoAppException {
		UinDraft uin = new UinDraft();
		uin.setUin("274390482564");
		String uinHash = securityManager.hash("274390482564".getBytes());
		uin.setUinHash(uinHash);
		uin.setRegId("1234567890");
		uin.setUinData("274390482564".getBytes());
		uin.setUinDataHash(securityManager.hash("274390482564".getBytes()));
		Optional<UinDraft> uinOpt =Optional.of(uin);
		when(uinDraftRepo.findByRegId(Mockito.any())).thenReturn(uinOpt);	
		IdResponseDTO response = idRepoServiceImpl.discardDraft("1234567890");
		assertNotNull(response);
	}
	
}
