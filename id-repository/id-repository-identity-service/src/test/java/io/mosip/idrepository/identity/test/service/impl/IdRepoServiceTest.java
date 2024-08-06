package io.mosip.idrepository.identity.test.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import io.mosip.kernel.core.http.RequestWrapper;
import org.apache.commons.io.IOUtils;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import io.mosip.commons.khazana.spi.ObjectStoreAdapter;
import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.CredentialRequestStatusLifecycle;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.AuthTypeStatusEventDTO;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.RequestDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.repository.CredentialRequestStatusRepo;
import io.mosip.idrepository.core.repository.HandleRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.BiometricExtractionService;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.dto.HandleDto;
import io.mosip.idrepository.identity.entity.IdentityUpdateTracker;
import io.mosip.idrepository.identity.entity.Uin;
import io.mosip.idrepository.identity.entity.UinBiometric;
import io.mosip.idrepository.identity.entity.UinDocument;
import io.mosip.idrepository.identity.helper.AnonymousProfileHelper;
import io.mosip.idrepository.identity.helper.IdRepoServiceHelper;
import io.mosip.idrepository.identity.helper.ObjectStoreHelper;
import io.mosip.idrepository.identity.provider.IdentityUpdateTrackerPolicyProvider;
import io.mosip.idrepository.identity.repository.IdentityUpdateTrackerRepo;
import io.mosip.idrepository.identity.repository.UinBiometricHistoryRepo;
import io.mosip.idrepository.identity.repository.UinDocumentHistoryRepo;
import io.mosip.idrepository.identity.repository.UinDraftRepo;
import io.mosip.idrepository.identity.repository.UinHistoryRepo;
import io.mosip.idrepository.identity.repository.UinRepo;
import io.mosip.idrepository.identity.service.impl.DefaultShardResolver;
import io.mosip.idrepository.identity.service.impl.IdRepoProxyServiceImpl;
import io.mosip.idrepository.identity.service.impl.IdRepoServiceImpl;
import io.mosip.kernel.biometrics.commons.CbeffValidator;
import io.mosip.kernel.biometrics.constant.QualityType;
import io.mosip.kernel.biometrics.entities.RegistryIDType;
import io.mosip.kernel.cbeffutil.impl.CbeffImpl;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.fsadapter.exception.FSAdapterException;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.CryptoUtil;

/**
 * The Class IdRepoServiceTest.
 *
 * @author Manoj SP
 */
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest
@Import(EnvUtil.class)
@ActiveProfiles("test")
@ConfigurationProperties("mosip.idrepo.identity")
public class IdRepoServiceTest {

	private static final String TYPE = "type";

	private static final String ACTIVATED = "ACTIVATED";

	@Mock
	CbeffImpl cbeffUtil;

	@Mock
	AuditHelper auditHelper;

	@Mock
	Environment environment;

	@Mock
	ObjectStoreAdapter connection;

	/** The service. */
	@InjectMocks
	IdRepoProxyServiceImpl proxyService;

	@InjectMocks
	IdRepoServiceImpl service;

	@InjectMocks
	IdRepoSecurityManager securityManager;

	@Mock
	private UinBiometricHistoryRepo uinBioHRepo;

	@Mock
	private UinDocumentHistoryRepo uinDocHRepo;

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

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

	@Mock
	private IdentityUpdateTrackerRepo identityUpdateTracker;

	@Mock
	private IdRepoServiceHelper idRepoServiceHelper;

	@Mock
	private HandleRepo handleRepo;

	/** The id. */
	private Map<String, String> id;

	/** The uin. */
	Uin uin = new Uin();

	/** The request. */
	RequestWrapper<IdRequestDTO<Object>> request = new RequestWrapper<>();

	public Map<String, String> getId() {
		return id;
	}

	public void setId(Map<String, String> id) {
		this.id = id;
	}

	/**
	 * Setup.
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws IdRepoDataValidationException
	 * @throws RestServiceException
	 */
	@Before
	public void setup() throws FileNotFoundException, IOException, IdRepoDataValidationException, RestServiceException {
		RegistryIDType registryIDType = new RegistryIDType();
		registryIDType.setOrganization("257");
		registryIDType.setType("7");
		QualityType quality = new QualityType();
		quality.setScore(95l);
		ReflectionTestUtils.setField(securityManager, "mapper", mapper);
		ReflectionTestUtils.setField(service, "securityManager", securityManager);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManager);
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new RestRequestDTO());
		when(restHelper.requestSync(Mockito.any()))
				.thenReturn(mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class));
		ReflectionTestUtils.setField(proxyService, "mapper", mapper);
		ReflectionTestUtils.setField(proxyService, "id", id);
		ReflectionTestUtils.setField(service, "mapper", mapper);
		ReflectionTestUtils.setField(service, "activeStatus", ACTIVATED);
		ReflectionTestUtils.setField(proxyService, "service", service);
		ReflectionTestUtils.setField(proxyService, "allowedBioAttributes",
				Collections.singletonList("individualBiometrics"));
		ReflectionTestUtils.setField(service, "bioAttributes",
				Lists.newArrayList("individualBiometrics", "parentOrGuardianBiometrics"));
		ReflectionTestUtils.setField(service, "fieldsToReplaceOnUpdate",
				Lists.newArrayList("selectedHandles"));
		IdRequestDTO<Object> req = new IdRequestDTO<>();
		req.setRegistrationId("registrationId");
		request.setRequest(req);
		uin.setUin("1234");
		uin.setUinRefId("uinRefId");
		uin.setUinData(mapper.writeValueAsBytes(request));
		uin.setStatusCode(EnvUtil.getUinActiveStatus());
		when(credRequestRepo.findByIndividualIdHash(Mockito.any())).thenReturn(List.of());
		when(dummyPartner.getDummyOLVPartnerId()).thenReturn("");
		when(anonymousProfileHelper.setRegId(Mockito.any())).thenReturn(anonymousProfileHelper);
		when(anonymousProfileHelper.setOldCbeff(Mockito.any())).thenReturn(anonymousProfileHelper);
		when(anonymousProfileHelper.setNewUinData(Mockito.any())).thenReturn(anonymousProfileHelper);
		when(anonymousProfileHelper.isNewCbeffPresent()).thenReturn(true);
	}

	/**
	 * Test add identity.
	 *
	 * @throws IdRepoAppException   the id repo app exception
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	@Test
	public void testAddIdentity() throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		ObjectNode obj = mapper.readValue(
				"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes(),
				ObjectNode.class);
		IdRequestDTO<Object> req = new IdRequestDTO<>();
		req.setIdentity(obj);
		req.setRegistrationId("27841457360002620190730095024");
		request.setRequest(req);
		uinObj.setUinData("".getBytes());
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(false);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg	");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		IdResponseDTO<Object> addIdentity = proxyService.addIdentity(request.getRequest(), "1234");
		assertEquals(ACTIVATED, addIdentity.getResponse().getStatus());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAddIdentityWithVerifiedAttributes()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		ObjectNode obj = mapper.readValue(
				"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes(),
				ObjectNode.class);
		IdRequestDTO<Object> req = new IdRequestDTO<>();
		req.setIdentity(obj);
		req.setRegistrationId("27841457360002620190730095024");
		req.setVerifiedAttributes(List.of("a", "b"));
		request.setRequest(req);
		uinObj.setUinData(
				"{\"identity\":\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes());
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(false);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		ArgumentCaptor<Uin> captureArg = ArgumentCaptor.forClass(Uin.class);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg	");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		service.addIdentity(request.getRequest(), "1234");
		verify(uinRepo).save(captureArg.capture());
		Uin uinValue = captureArg.getValue();
		List<String> verifiedAttributes = (List<String>) mapper.readValue(uinValue.getUinData(), Map.class)
				.get("verifiedAttributes");
		assertEquals(List.of("a", "b"), verifiedAttributes);
	}

	@Test
	public void testAddDocumentsDataAccessException()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			IdRequestDTO<Object> req = mapper.readValue(
					"{\"identity\":{\"proofOfDateOfBirth\":{\"format\":\"pdf\",\"type\":\"passport\",\"value\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"proofOfDateOfBirth\",\"value\":\"dGVzdA\"}]}"
							.getBytes(),
					IdRequestDTO.class);
			req.setRegistrationId("27841457360002620190730095024");
			request.setRequest(req);
			when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(false);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			when(uinDocHRepo.save(Mockito.any())).thenThrow(new DataAccessResourceFailureException(null));
			proxyService.addIdentity(request.getRequest(), "1234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testAddDocumentsJDBCConnectionException()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			IdRequestDTO<Object> req = mapper.readValue(
					"{\"identity\":{\"proofOfDateOfBirth\":{\"format\":\"pdf\",\"type\":\"passport\",\"value\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"proofOfDateOfBirth\",\"value\":\"dGVzdA\"}]}"
							.getBytes(),
					IdRequestDTO.class);
			req.setRegistrationId("27841457360002620190730095024");
			request.setRequest(req);
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(false);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			when(uinDocHRepo.save(Mockito.any())).thenThrow(new JDBCConnectionException(null, null));
			proxyService.addIdentity(request.getRequest(), "1234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testAddIdentityDocumentStoreFailed() throws Exception {
		try {
			when(cbeffUtil.validateXML(Mockito.any())).thenReturn(true);
			when(connection.putObject(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
					Mockito.any()))
					.thenThrow(new FSAdapterException(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
							IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage()));
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinHash("3A6EB0790F39AC87C94F3856B2DD2C5D110E6811602261A9A923D3BB23ADC8B7");
			uinObj.setUinRefId("1234");
			uinObj.setUinData("".getBytes());
			IdRequestDTO req = mapper
					.readValue(("{\"identity\":{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
							+ IdRepoConstants.FILE_NAME_ATTRIBUTE
							+ "\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"individualBiometrics\",\"value\":\"dGVzdA\"}]}")
							.getBytes(), IdRequestDTO.class);
			req.setRegistrationId("27841457360002620190730095024");
			request.setRequest(req);
			when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(false);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			proxyService.addIdentity(request.getRequest(), "1234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testAddIdentityWithBioDocuments() throws Exception {
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(connection.putObject(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(true);
		when(cbeffUtil.validateXML(Mockito.any())).thenReturn(true);
		when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("data".getBytes());
		when(cbeffUtil.createXML(Mockito.any())).thenReturn("data".getBytes());
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		IdRequestDTO req = mapper
				.readValue(("{\"identity\":{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
						+ IdRepoConstants.FILE_NAME_ATTRIBUTE
						+ "\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"individualBiometrics\",\"value\":\"dGVzdA\"}]}")
						.getBytes(), IdRequestDTO.class);
		req.setRegistrationId("27841457360002620190730095024");
		request.setRequest(req);
		uinObj.setUinData("".getBytes());
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(false);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		IdResponseDTO<Object> addIdentity = proxyService.addIdentity(request.getRequest(), "1234");
		assertEquals(ACTIVATED, addIdentity.getResponse().getStatus());
	}

	@Test
	public void testAddIdentityWithBioDocumentsCbeffValidationFailed() throws Exception {
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(connection.putObject(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(true);
		when(cbeffUtil.validateXML(Mockito.any())).thenThrow(new NullPointerException());
		when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("data".getBytes());
		when(cbeffUtil.createXML(Mockito.any())).thenReturn("data".getBytes());
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		IdRequestDTO req = mapper
				.readValue(("{\"identity\":{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
						+ IdRepoConstants.FILE_NAME_ATTRIBUTE
						+ "\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"individualBiometrics\",\"value\":\"dGVzdA\"}]}")
						.getBytes(), IdRequestDTO.class);
		req.setRegistrationId("27841457360002620190730095024");
		request.setRequest(req);
		uinObj.setUinData("".getBytes());
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(false);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		try {
			proxyService.addIdentity(request.getRequest(), "1234");
		} catch (IdRepoAppException e) {
			assertEquals(INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "documents/" + 0 + "/value"),
					e.getErrorText());
		}
	}

	@Test
	public void testAddIdentityWithBioDocumentsObjectStorePutFailed() throws Exception {
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(connection.putObject(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(true);
		doThrow(new IdRepoAppException(IdRepoErrorConstants.FILE_NOT_FOUND)).when(objectStoreHelper)
				.putBiometricObject(any(), any(), any());
		when(cbeffUtil.validateXML(Mockito.any())).thenReturn(true);
		when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("data".getBytes());
		when(cbeffUtil.createXML(Mockito.any())).thenReturn("data".getBytes());
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		IdRequestDTO req = mapper
				.readValue(("{\"identity\":{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
						+ IdRepoConstants.FILE_NAME_ATTRIBUTE
						+ "\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"individualBiometrics\",\"value\":\"dGVzdA\"}]}")
						.getBytes(), IdRequestDTO.class);
		req.setRegistrationId("27841457360002620190730095024");
		request.setRequest(req);
		uinObj.setUinData("".getBytes());
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(false);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		try {
			proxyService.addIdentity(request.getRequest(), "1234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testAddIdentityRecordExists()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			ObjectNode obj = mapper.readValue(
					"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
							.getBytes(),
					ObjectNode.class);
			IdRequestDTO req = new IdRequestDTO();
			req.setRegistrationId("27841457360002620190730095024");
			req.setIdentity(obj);
			request.setRequest(req);
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			proxyService.addIdentity(request.getRequest(), "1234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.RECORD_EXISTS.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.RECORD_EXISTS.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testAddIdentityDataAccessException()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			ObjectNode obj = mapper.readValue(
					"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
							.getBytes(),
					ObjectNode.class);
			IdRequestDTO req = new IdRequestDTO();
			req.setRegistrationId("27841457360002620190730095024");
			req.setIdentity(obj);
			request.setRequest(req);
			when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			when(uinRepo.save(Mockito.any())).thenThrow(new RecoverableDataAccessException(null));
			proxyService.addIdentity(request.getRequest(), "1234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	/**
	 * Test add identity exception.
	 *
	 * @throws IdRepoAppException   the id repo app exception
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	@Test
	public void testAddIdentityException()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			when(uinRepo.save(Mockito.any())).thenThrow(new DataAccessResourceFailureException(null));
			IdRequestDTO request2 = new IdRequestDTO();
			request2.setIdentity(mapper.readValue(
					"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
							.getBytes(),
					Object.class));
			request2.setRegistrationId("27841457360002620190730095024");
			request.setRequest(request2);
			proxyService.addIdentity(request.getRequest(), "1234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	/**
	 * Test retrieve identity.
	 *
	 * @throws IdRepoAppException   the id repo app exception
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	@Test
	public void testRetrieveIdentity()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		String identity = "{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}";
		uinObj.setUinData(identity.getBytes());
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		IdResponseDTO<Object> retrieveIdentityByUin = proxyService.retrieveIdentity("1234", IdType.UIN, null, null);
		assertEquals(identity, mapper.writeValueAsString(retrieveIdentityByUin.getResponse().getIdentity()));
	}

	@Test
	public void testRetrieveIdentityNoRecordExists()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			uinObj.setUinData(
					"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
							.getBytes());
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(false);
			proxyService.retrieveIdentity("1234", IdType.UIN, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityDataAccessException()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			uinObj.setUinData(
					"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
							.getBytes());
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			when(uinRepo.findByUinHash(Mockito.any())).thenThrow(new JDBCConnectionException("", null));
			proxyService.retrieveIdentity("1234", IdType.UIN, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityWithBioDocuments()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any())).thenReturn("dGVzdA".getBytes());
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		UinBiometric biometrics = new UinBiometric();
		biometrics.setBiometricFileType("individualBiometrics");
		biometrics.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometrics.setBioFileId("1234");
		biometrics.setBiometricFileName("name");
		uinObj.setBiometrics(Collections.singletonList(biometrics));
		String identityWithDoc = "{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}";
		uinObj.setUinData(identityWithDoc.getBytes());
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		IdResponseDTO<Object> retrieveIdentityByUin = proxyService.retrieveIdentity("1234", IdType.UIN, "bio", null);
		assertEquals(identityWithDoc, mapper.writeValueAsString(retrieveIdentityByUin.getResponse().getIdentity()));
	}

	@Test
	public void testRetrieveIdentityWithBioDocumentsFileRetrievalError()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			when(objectStoreHelper.getDemographicObject(Mockito.any(), Mockito.any()))
					.thenThrow(new FSAdapterException("", ""));
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
			UinBiometric biometrics = new UinBiometric();
			biometrics.setBiometricFileType("individualBiometrics");
			biometrics.setBiometricFileHash("A6xnQhbz4Vx2HuGl4lXwZ5U2I8iziLRFnhP5eNfIRvQ");
			biometrics.setBioFileId("1234");
			biometrics.setBiometricFileName("name");
			uinObj.setBiometrics(Collections.singletonList(biometrics));
			uinObj.setUinData(
					"{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}"
							.getBytes());
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			proxyService.retrieveIdentity("1234", IdType.UIN, "bio", null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityWithBioDocumentsFileRetrievalErrorUnknownError()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any()))
					.thenThrow(new IdRepoAppException(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
							IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage()));
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
			UinBiometric biometrics = new UinBiometric();
			biometrics.setBiometricFileType("individualBiometrics");
			biometrics.setBiometricFileHash("A6xnQhbz4Vx2HuGl4lXwZ5U2I8iziLRFnhP5eNfIRvQ");
			biometrics.setBioFileId("1234");
			biometrics.setBiometricFileName("name");
			uinObj.setBiometrics(Collections.singletonList(biometrics));
			uinObj.setUinData(
					"{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}"
							.getBytes());
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			proxyService.retrieveIdentity("1234", IdType.UIN, "bio", null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}

	}

	@Test
	public void testRetrieveIdentityWithBioDocumentsFileRetrievalIOError()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any()))
					.thenThrow(new IdRepoAppUncheckedException(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR));
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
			uinObj.setUinRefId("1234");
			UinBiometric biometrics = new UinBiometric();
			biometrics.setBiometricFileType("individualBiometrics");
			biometrics.setBiometricFileHash("A6xnQhbz4Vx2HuGl4lXwZ5U2I8iziLRFnhP5eNfIRvQ");
			biometrics.setBioFileId("1234");
			biometrics.setBiometricFileName("name");
			uinObj.setBiometrics(Collections.singletonList(biometrics));
			uinObj.setUinData(
					"{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}"
							.getBytes());
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			proxyService.retrieveIdentity("1234", IdType.UIN, "bio", null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityWithBioDocumentsHashFail()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any())).thenReturn("data".getBytes());
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
			UinBiometric biometrics = new UinBiometric();
			biometrics.setBiometricFileType("individualBiometrics");
			biometrics.setBiometricFileHash("A6xnQhbz4Vx2HuGl4lXwZ5U2I8iziLRFnhPeNfIRvQ");
			biometrics.setBioFileId("1234");
			biometrics.setBiometricFileName("name");
			uinObj.setBiometrics(Collections.singletonList(biometrics));
			uinObj.setUinData(
					"{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}"
							.getBytes());
			when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			proxyService.retrieveIdentity("1234", IdType.UIN, "bio", null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DOCUMENT_HASH_MISMATCH.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DOCUMENT_HASH_MISMATCH.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityWithDemoDocuments()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		when(objectStoreHelper.getDemographicObject(Mockito.any(), Mockito.any())).thenReturn("data".getBytes());
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		UinDocument document = new UinDocument();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocHash("3A6EB0790F39AC87C94F3856B2DD2C5D110E6811602261A9A923D3BB23ADC8B7");
		document.setDocId("1234");
		document.setDocName("name");
		uinObj.setDocuments(Collections.singletonList(document));
		String identityWithDoc = "{\"ProofOfIdentity\":{\"format\":\"pdf\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}";
		uinObj.setUinData(identityWithDoc.getBytes());
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		IdResponseDTO<Object> retrieveIdentityByUin = proxyService.retrieveIdentity("1234", IdType.UIN, "demo", null);
		assertEquals(identityWithDoc, mapper.writeValueAsString(retrieveIdentityByUin.getResponse().getIdentity()));
	}

	@Test
	public void testRetrieveIdentityWithDemoDocumentsIOError()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			when(objectStoreHelper.getDemographicObject(Mockito.any(), Mockito.any()))
					.thenThrow(new IdRepoAppException(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR));
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
			UinDocument document = new UinDocument();
			document.setDoccatCode("ProofOfIdentity");
			document.setDocHash("5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
			document.setDocId("1234");
			document.setDocName("name");
			uinObj.setDocuments(Collections.singletonList(document));
			uinObj.setUinData(
					"{\"ProofOfIdentity\":{\"format\":\"pdf\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}"
							.getBytes());
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			proxyService.retrieveIdentity("1234", IdType.UIN, "demo", null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityWithDemoDocumentsFSError()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			when(objectStoreHelper.getDemographicObject(Mockito.any(), Mockito.any()))
					.thenThrow(new IdRepoAppException(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR));
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
			uinObj.setUinRefId("1234");
			UinDocument document = new UinDocument();
			document.setDoccatCode("ProofOfIdentity");
			document.setDocHash("5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
			document.setDocId("1234");
			document.setDocName("name");
			uinObj.setDocuments(Collections.singletonList(document));
			uinObj.setUinData(
					"{\"ProofOfIdentity\":{\"format\":\"pdf\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}"
							.getBytes());
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			proxyService.retrieveIdentity("1234", IdType.UIN, "demo", null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityWithDemoDocumentsFileNotFound()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			when(objectStoreHelper.getDemographicObject(Mockito.any(), Mockito.any()))
					.thenThrow(new IdRepoAppException(IdRepoErrorConstants.FILE_NOT_FOUND));
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
			UinDocument document = new UinDocument();
			document.setDoccatCode("ProofOfIdentity");
			document.setDocHash("5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
			document.setDocId("1234");
			document.setDocName("name");
			uinObj.setDocuments(Collections.singletonList(document));
			uinObj.setUinData(
					"{\"ProofOfIdentity\":{\"format\":\"pdf\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}"
							.getBytes());
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg	");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			proxyService.retrieveIdentity("1234", IdType.UIN, "demo", null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_NOT_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityWithDemoDocumentsHashFail()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			when(objectStoreHelper.getDemographicObject(Mockito.any(), Mockito.any())).thenReturn("data".getBytes());
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			UinDocument document = new UinDocument();
			document.setDoccatCode("ProofOfIdentity");
			document.setDocHash("A6xnQhbz4Vx2HuGl4lXwZ5U28iziLRFnhP5eNfIRvQ");
			document.setDocId("1234");
			document.setDocName("name");
			uinObj.setDocuments(Collections.singletonList(document));
			uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
			uinObj.setUinData(
					"{\"ProofOfIdentity\":{\"format\":\"pdf\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}"
							.getBytes());
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg	");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			proxyService.retrieveIdentity("1234", IdType.UIN, "demo", null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DOCUMENT_HASH_MISMATCH.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DOCUMENT_HASH_MISMATCH.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityWithAllType()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		when(objectStoreHelper.getDemographicObject(Mockito.any(), Mockito.any())).thenReturn("data".getBytes());
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		UinBiometric biometrics = new UinBiometric();
		biometrics.setBiometricFileType("individualBiometrics");
		biometrics.setBiometricFileHash("5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		biometrics.setBioFileId("1234");
		biometrics.setBiometricFileName("name");
		uinObj.setBiometrics(Collections.singletonList(biometrics));
		UinDocument document = new UinDocument();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocHash("3A6EB0790F39AC87C94F3856B2DD2C5D110E6811602261A9A923D3BB23ADC8B7");
		document.setDocId("1234");
		document.setDocName("name");
		uinObj.setDocuments(Collections.singletonList(document));
		uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		String identity = "{\"ProofOfIdentity\":{\"format\":\"pdf\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"},\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}";
		uinObj.setUinData(identity.getBytes());
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		IdResponseDTO<Object> retrieveIdentityByUin = proxyService.retrieveIdentity("1234", IdType.UIN, "all", null);
		assertEquals(identity, mapper.writeValueAsString(retrieveIdentityByUin.getResponse().getIdentity()));
	}

	@Test
	public void testRetrieveIdentityWithUnknownType()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			UinBiometric biometrics = new UinBiometric();
			biometrics.setBiometricFileType("individualBiometrics");
			biometrics.setBiometricFileHash("A6xnQhbz4Vx2HuGl4lXwZ5U2I8iziLRFnhP5eNfIRvQ");
			biometrics.setBioFileId("1234");
			biometrics.setBiometricFileName("name");
			uinObj.setBiometrics(Collections.singletonList(biometrics));
			UinDocument document = new UinDocument();
			document.setDoctypCode("ProofOfIdentity");
			document.setDocHash("47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU");
			document.setDocId("1234");
			document.setDocName("name");
			uinObj.setDocuments(Collections.singletonList(document));
			uinObj.setUinData(
					"{\"ProofOfIdentity\":{\"format\":\"pdf\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"},\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}"
							.getBytes());
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			proxyService.retrieveIdentity("1234", IdType.UIN, "a", null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), TYPE),
					e.getErrorText());
		}
	}

	@Test
	public void updateIdentity_withValidDetail_thenPass() throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		ReflectionTestUtils.setField(service, "trimWhitespaces", true);
		Object obj = mapper.readValue(
				"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"    Manoj       \",\"label\":\"string\"}],\"selectedHandles\":[\"    email     \",\"       dateOfBirth   \"],\"email\":\"   ritik8989@gmail.com     \",\"dateOfBirth\":\"  2000/01/01    \",\"individualBiometrics\":{\"format\":\"cbeff\",\"value\":\"      fileReferenceID   \"}}}"
						.getBytes(),
				Object.class);
		IdRequestDTO req = new IdRequestDTO();
		req.setStatus("REGISTERED");
		req.setRegistrationId("27841457360002620190730095024");
		req.setIdentity(obj);
		request.setRequest(req);
		Map<String, Object> identityData = new HashMap<String, Object>();
		identityData.put("email", "   ritik8989@gmail.com     ");
		identityData.put("dateOfBirth", "  2000/01/01    ");
		Map<String, Object> firstNameMap = new HashMap<String, Object>();
		firstNameMap.put("language", "AR");
		firstNameMap.put("value", "    Manoj       ");
		firstNameMap.put("label", "string");
		identityData.put("firstName", List.of(firstNameMap));
		List<String> handlesList = new ArrayList<String>();
		handlesList.add("    email     ");
		handlesList.add("       dateOfBirth   ");
		identityData.put("selectedHandles", handlesList);
		Map<String, Object> individualBioMap = new HashMap<String, Object>();
		individualBioMap.put("format", "cbeff");
		individualBioMap.put("value", "      fileReferenceID   ");
		identityData.put("individualBiometrics", individualBioMap);
		when(idRepoServiceHelper.convertToMap(any())).thenReturn(identityData);
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode("REGISTERED");
		uinObj.setUinHash("375848393846348345");
		Object obj2 = mapper.readValue(
				"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Mano\",\"label\":\"string\"}],\"lastName\":[{\"language\":\"AR\",\"value\":\"Mano\",\"label\":\"string\"},{\"language\":\"FR\",\"value\":\"Mano\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		uinObj.setUinData(mapper.writeValueAsBytes(obj2));
		when(environment.getProperty("mosip.idrepo.identity.uin-status.registered")).thenReturn("ACTIVE");
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(anonymousProfileHelper.isNewCbeffPresent()).thenReturn(false);
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("http://localhost/v1/vid/{uin}");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		Map<String, List<HandleDto>> inputHandlesMap = new HashMap<>();
		List<HandleDto> inputEmail = new ArrayList<>();
		inputEmail.add(new HandleDto("ritik8989@gmail.com@email", "341_AAFB5CBEB3878A4BA9"));
		inputHandlesMap.put("email", inputEmail);
		List<HandleDto> inputDob = new ArrayList<>();
		inputDob.add(new HandleDto("2000/01/01@dateOfBirth", "341_AAFB5CBEB38ert4r59"));
		inputHandlesMap.put("dateOfBirth", inputDob);
		Map<String, List<HandleDto>> existingHandlesMap = new HashMap<>();
		List<HandleDto> existsPhone = new ArrayList<>();
		existsPhone.add(new HandleDto("8987989789@phone", "341_AAFB5CBEB3878A4we3"));
		existingHandlesMap.put("phone", existsPhone);
		List<HandleDto> existsEmail = new ArrayList<>();
		existsEmail.add(new HandleDto("ritik8989@gmail.com@email", "341_AAFB5CBEB3878A4BA9"));
		existingHandlesMap.put("email", existsEmail);
		when(idRepoServiceHelper.getSelectedHandles(any(), nullable(Map.class))).thenReturn(existingHandlesMap).thenReturn(inputHandlesMap);
		//when(handleRepo.findUinHashByHandleHashes(Mockito.any())).thenReturn(null).thenReturn(Collections.singletonList("375848393846348345"));
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(service, "securityManager", securityManagerMock);
		ResponseWrapper<AuthTypeStatusEventDTO> eventsResponse = new ResponseWrapper<>();
		eventsResponse.setResponse(new AuthTypeStatusEventDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(eventsResponse);
		proxyService.updateIdentity(request.getRequest(), "234").getResponse().equals(obj2);
	}

	@Ignore
	@Test
	public void updateIdentity_withDuplicateHandle_thenFail()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			Object obj = mapper.readValue(
					"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}],\"selectedHandles\":[\"email\"],\"email\":\"ritik8989@gmail.com\"}}"
							.getBytes(),
					Object.class);
			IdRequestDTO req = new IdRequestDTO();
			req.setStatus("ACTIVATED");
			req.setRegistrationId("27841457360002620190730095024");
			req.setIdentity(obj);
			request.setRequest(req);
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			uinObj.setStatusCode("ACTIVATED");
			uinObj.setUinHash("375848393846348345");
			Object obj2 = mapper.readValue(
					"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Mano\",\"label\":\"string\"}],\"lastName\":[{\"language\":\"AR\",\"value\":\"Mano\",\"label\":\"string\"},{\"language\":\"FR\",\"value\":\"Mano\",\"label\":\"string\"}]}}"
							.getBytes(),
					Object.class);
			uinObj.setUinData(mapper.writeValueAsBytes(obj2));
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			Map<String, List<HandleDto>> inputHandlesMap = new HashMap<>();
			inputHandlesMap.put("email", List.of(new HandleDto("ritik8989@gmail.com@email", "341_AAFB5CBEB3878A4BA9")));
			Map<String, List<HandleDto>> existingHandlesMap = new HashMap<>();
			existingHandlesMap.put("phone", List.of(new HandleDto("8987989789@phone", "341_AAFB5CBEB3878A4we3")));
			when(idRepoServiceHelper.getSelectedHandles(any(), nullable(Map.class)))
					.thenReturn(existingHandlesMap).thenReturn(inputHandlesMap);
			when(handleRepo.findUinHashByHandleHash(Mockito.anyString())).thenReturn("125355668848368");
			proxyService.updateIdentity(request.getRequest(), "234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.HANDLE_RECORD_EXISTS.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.HANDLE_RECORD_EXISTS.getErrorMessage(), List.of("email")),
					e.getErrorText());
		}
	}

	@Test
	public void testUpdateIdentityInvalidJsonException()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		try {
			Object obj = mapper.readValue(
					"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Mano\",\"label\":\"string\"},{\"language\":\"FR\",\"value\":\"Mano\",\"label\":\"string\"}]}}"
							.getBytes(),
					Object.class);

			IdRequestDTO req = new IdRequestDTO();
			req.setStatus("REGISTERED");
			req.setRegistrationId("27841457360002620190730095024");
			req.setIdentity(obj);
			request.setRequest(req);
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			uinObj.setStatusCode("REGISTERED");
			uinObj.setUinData(
					"rgAADOjjov89sjVwvI8Gc4ngK9lQgPxMpNDe+LXb5qI=|P6NGM4tYz1Zdy+ZC/ikKYNp1csxrarX/dCEta1HCHWE=|P6NGM4tYz1Zdy+ZC/ikKYNp1csxrarX/dCEta1HCHWE="
							.getBytes());
			when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.getStatusByUin(Mockito.any())).thenReturn("REGISTERED");
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			proxyService.updateIdentity(request.getRequest(), "1234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.ID_OBJECT_PROCESSING_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.ID_OBJECT_PROCESSING_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testUpdateIdentityMissingValue() throws IdRepoAppException, IOException {
		Object obj = mapper.readValue(
				"{\"lastName\":[{\"language\":\"EN\",\"value\":\"Mano\"}],\"IDSchemaVersion\":1.0}".getBytes(),
				Object.class);

		IdRequestDTO req = new IdRequestDTO();
		req.setStatus(ACTIVATED);
		req.setRegistrationId("27841457360002620190730095024");
		req.setIdentity(obj);
		request.setRequest(req);
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		Object obj2 = mapper.readValue(
				"{\"firstName\":[{\"language\":\"AR\",\"value\":\"Mano\"}],\"lastName\":[{\"language\":\"AR\",\"value\":\"Mano\"},{\"language\":\"FR\",\"value\":\"Mano\"}],\"IDSchemaVersion\":1.0}"
						.getBytes(),
				Object.class);
		uinObj.setUinData(mapper.writeValueAsBytes(obj2));
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(environment.getProperty("mosip.idrepo.identity.uin-status.registered")).thenReturn("ACTIVE");
		when(uinRepo.getStatusByUin(Mockito.any())).thenReturn(ACTIVATED);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("http://localhost/v1/vid/{uin}");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		ResponseWrapper<AuthTypeStatusEventDTO> eventsResponse = new ResponseWrapper<>();
		eventsResponse.setResponse(new AuthTypeStatusEventDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(eventsResponse);
		ResponseDTO<Object> response = (ResponseDTO<Object>) proxyService.updateIdentity(request.getRequest(), "234").getResponse();
		assertEquals(ACTIVATED, response.getStatus());
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IdRepoAppException.class)
	public void testConvertToBytes() throws Throwable {
		ObjectMapper mockMapper = Mockito.mock(ObjectMapper.class);
		ReflectionTestUtils.setField(service, "mapper", mockMapper);
		try {
			when(mockMapper.writeValueAsBytes(Mockito.any())).thenThrow(new JsonMappingException(""));
			ReflectionTestUtils.invokeMethod(service, "convertToBytes", "1234");
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IdRepoAppException.class)
	public void testConvertToObjectProxy() throws Throwable {
		ObjectMapper mockMapper = Mockito.mock(ObjectMapper.class);
		ReflectionTestUtils.setField(proxyService, "mapper", mockMapper);
		try {
			when(mockMapper.readValue("1234".getBytes(), String.class)).thenThrow(new JsonMappingException(""));
			ReflectionTestUtils.invokeMethod(proxyService, "convertToObject", "1234".getBytes(), String.class);
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	@SuppressWarnings("deprecation")
	@Test(expected = IdRepoAppUncheckedException.class)
	public void testConvertToObject() throws Throwable {
		ObjectMapper mockMapper = Mockito.mock(ObjectMapper.class);
		ReflectionTestUtils.setField(service, "mapper", mockMapper);
		try {
			when(mockMapper.readValue("1234".getBytes(), String.class)).thenThrow(new JsonMappingException(""));
			ReflectionTestUtils.invokeMethod(service, "convertToObject", "1234".getBytes(), String.class);
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	@Test
	public void testUpdateIdentityInvalidRegId() throws IdRepoAppException {
		try {
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			IdRequestDTO<Object> request = new IdRequestDTO<>();
			request.setRegistrationId("27841457360002620190730095024");
			proxyService.updateIdentity(request, "12343");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.RECORD_EXISTS.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.RECORD_EXISTS.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testUpdateIdentityUinNotExists() throws IdRepoAppException {
		try {
			IdRequestDTO<Object> requestDTO = new IdRequestDTO();
			requestDTO.setRegistrationId("1234");
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(false);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			proxyService.updateIdentity(requestDTO, "12343");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testUpdateIdentityDataAccessError() throws IdRepoAppException {
		try {
			IdRequestDTO idRequestDTO = new IdRequestDTO();
			idRequestDTO.setRegistrationId("1234");
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			when(uinRepo.existsByUinHash(Mockito.any())).thenThrow(new DataAccessResourceFailureException(""));
			proxyService.updateIdentity(idRequestDTO, "12343");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testUpdateIdentityUpdateStatus()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setRegId("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode("");
		uinObj.setUinData(new byte[] { 0 });
		uinObj.setUinDataHash("");
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(environment.getProperty("mosip.idrepo.identity.uin-status.registered")).thenReturn("ACTIVE");
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		IdRequestDTO<Object> request = new IdRequestDTO();
		String status = "status";
		request.setStatus(status);
		request.setRegistrationId("27841457360002620190730095024");
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("http://localhost/v1/vid/{uin}");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		ResponseWrapper<AuthTypeStatusEventDTO> eventsResponse = new ResponseWrapper<>();
		eventsResponse.setResponse(new AuthTypeStatusEventDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(eventsResponse);
		IdResponseDTO<Object> updateIdentity = proxyService.updateIdentity(request, "12343");
		assertEquals(status, updateIdentity.getResponse().getStatus());
	}

	@Test(expected = IdRepoAppException.class)
	public void testEncryptDecryptDocumentsExceptionProxy() throws Throwable {
		try {
			RestRequestDTO restRequestDTO = new RestRequestDTO();
			when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restRequestDTO);
			when(restHelper.requestSync(Mockito.any()))
					.thenThrow(new RestServiceException(IdRepoErrorConstants.CLIENT_ERROR));
			Uin uin = new Uin();
			uin.setUinData(new byte[] { 0 });
			ReflectionTestUtils.invokeMethod(securityManager, "encryptDecryptData", restRequestDTO);
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	@Test(expected = IdRepoAppException.class)
	public void testEncryptDecryptDocumentsException() throws Throwable {
		try {
			RestRequestDTO restRequestDTO = new RestRequestDTO();
			when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restRequestDTO);
			when(restHelper.requestSync(Mockito.any()))
					.thenThrow(new RestServiceException(IdRepoErrorConstants.CLIENT_ERROR));
			Uin uin = new Uin();
			uin.setUinData(new byte[] { 0 });
			ReflectionTestUtils.invokeMethod(securityManager, "encryptDecryptData", restRequestDTO);
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	@Test(expected = IdRepoAppException.class)
	public void testEncryptDecryptDocumentsNoData() throws Throwable {
		try {
			RestRequestDTO restRequestDTO = new RestRequestDTO();
			when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restRequestDTO);
			when(restHelper.requestSync(Mockito.any())).thenReturn(mapper.readValue("{}".getBytes(), ObjectNode.class));
			Uin uin = new Uin();
			uin.setUinData(new byte[] { 0 });
			ReflectionTestUtils.invokeMethod(securityManager, "encryptDecryptData", restRequestDTO);
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	@Test
	public void testIdentityUpdateBioDocuments() throws Exception {
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinHash("1234_1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		IdRequestDTO req = mapper
				.readValue(("{\"identity\":{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
						+ IdRepoConstants.FILE_NAME_ATTRIBUTE
						+ "\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"individualBiometrics\",\"value\":\"dGVzdA\"}]}")
						.getBytes(), IdRequestDTO.class);
		req.setRegistrationId("27841457360002620190730095024");
		request.setRequest(req);
		UinBiometric biometrics = new UinBiometric();
		biometrics.setBiometricFileType("individualBiometrics");
		biometrics.setBiometricFileHash("W3LDtXpyxkl0YSifynsfhl7W-wWWtEb-ofkq-TGl1Lc");
		biometrics.setBioFileId("1234.cbeff");
		biometrics.setBiometricFileName("name");
		uinObj.setBiometrics(Collections.singletonList(biometrics));
		uinObj.setUinData(
				("{\"status\": \"ACTIVATED\",\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
						+ IdRepoConstants.FILE_NAME_ATTRIBUTE + "\":\"fileReferenceID\"}}").getBytes());
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(environment.getProperty("mosip.idrepo.identity.uin-status.registered")).thenReturn(ACTIVATED);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("value".getBytes());
		when(cbeffUtil.createXML(Mockito.any())).thenReturn("value".getBytes());
		when(cbeffUtil.validateXML(Mockito.any())).thenReturn(true);
		when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any())).thenReturn("dGVzdA".getBytes());
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("http://localhost/v1/vid/{uin}");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		ResponseWrapper<AuthTypeStatusEventDTO> eventsResponse = new ResponseWrapper<>();
		eventsResponse.setResponse(new AuthTypeStatusEventDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(
				mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class),
				mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class), eventsResponse);
		IdResponseDTO<Object> updateIdentity = proxyService.updateIdentity(request.getRequest(), "1234");
		assertEquals(ACTIVATED, updateIdentity.getResponse().getStatus());
	}

	@Test
	public void testIdentityUpdateBioDocumentsUnknownError() throws Exception {
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinHash("1234_1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		IdRequestDTO req = mapper
				.readValue(("{\"identity\":{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
						+ IdRepoConstants.FILE_NAME_ATTRIBUTE
						+ "\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"individualBiometrics\",\"value\":\"dGVzdA\"}]}")
						.getBytes(), IdRequestDTO.class);
		req.setRegistrationId("27841457360002620190730095024");
		request.setRequest(req);
		UinBiometric biometrics = new UinBiometric();
		biometrics.setBiometricFileType("individualBiometrics");
		biometrics.setBiometricFileHash("W3LDtXpyxkl0YSifynsfhl7W-wWWtEb-ofkq-TGl1Lc");
		biometrics.setBioFileId("1234.cbeff");
		biometrics.setBiometricFileName("name");
		uinObj.setBiometrics(Collections.singletonList(biometrics));
		uinObj.setUinData(
				("{\"status\": \"ACTIVATED\",\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
						+ IdRepoConstants.FILE_NAME_ATTRIBUTE + "\":\"fileReferenceID\"}}").getBytes());
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("value".getBytes());
		when(cbeffUtil.createXML(Mockito.any())).thenReturn("value".getBytes());
		when(cbeffUtil.validateXML(Mockito.any())).thenReturn(true);
		when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any())).thenThrow(new NullPointerException());
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("http://localhost/v1/vid/{uin}");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		ResponseWrapper<AuthTypeStatusEventDTO> eventsResponse = new ResponseWrapper<>();
		eventsResponse.setResponse(new AuthTypeStatusEventDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(
				mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class),
				mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class), eventsResponse);
		try {
			proxyService.updateIdentity(request.getRequest(), "1234");
		} catch (IdRepoAppException e) {
			assertEquals(INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "documents/" + 0 + "/value"),
					e.getErrorText());
		}
	}

	@Test
	public void testIdentityUpdateNewBioDocument() throws Exception {
		MockEnvironment mockEnv = new MockEnvironment();
		mockEnv.setProperty("mosip.fingerprint.fmr.enabled", "true");
		env.merge(mockEnv);
		ReflectionTestUtils.setField(service, "env", env);
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		IdRequestDTO req = mapper
				.readValue(("{\"identity\":{\"parentOrGuardianBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
						+ IdRepoConstants.FILE_NAME_ATTRIBUTE
						+ "\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"parentOrGuardianBiometrics\",\"value\":\"dGVzdA\"}]}")
						.getBytes(), IdRequestDTO.class);
		req.setRegistrationId("27841457360002620190730095024");
		request.setRequest(req);

		UinBiometric biometrics = new UinBiometric();
		biometrics.setBiometricFileType("individualBiometrics");
		biometrics.setBiometricFileHash("W3LDtXpyxkl0YSifynsfhl7W-wWWtEb-ofkq-TGl1Lc");
		biometrics.setBioFileId("1234.cbeff");
		biometrics.setBiometricFileName("name");
		uinObj.setBiometrics(Lists.newArrayList(biometrics));
		uinObj.setUinData(("{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
				+ IdRepoConstants.FILE_NAME_ATTRIBUTE + "\":\"fileReferenceID\"}}").getBytes());
		when(environment.getProperty("mosip.idrepo.identity.uin-status.registered")).thenReturn(ACTIVATED);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("value".getBytes());
		when(anonymousProfileHelper.isNewCbeffPresent()).thenReturn(false);
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("http://localhost/v1/vid/{uin}");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		ResponseWrapper<AuthTypeStatusEventDTO> eventsResponse = new ResponseWrapper<>();
		eventsResponse.setResponse(new AuthTypeStatusEventDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(
				mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class), eventsResponse);
		IdResponseDTO<Object> updateIdentity = proxyService.updateIdentity(request.getRequest(), "1234");
		assertEquals(ACTIVATED, updateIdentity.getResponse().getStatus());
	}

	@Test
	public void testIdentityUpdateNewBioDocumentCredentialExists() throws Exception {
		when(credRequestRepo.findByIndividualIdHash(any())).thenReturn(List.of(new CredentialRequestStatus()));
		MockEnvironment mockEnv = new MockEnvironment();
		mockEnv.setProperty("mosip.fingerprint.fmr.enabled", "true");
		env.merge(mockEnv);
		ReflectionTestUtils.setField(service, "env", env);
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		IdRequestDTO req = mapper
				.readValue(("{\"identity\":{\"parentOrGuardianBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
						+ IdRepoConstants.FILE_NAME_ATTRIBUTE
						+ "\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"parentOrGuardianBiometrics\",\"value\":\"dGVzdA\"}]}")
						.getBytes(), IdRequestDTO.class);
		req.setRegistrationId("27841457360002620190730095024");
		request.setRequest(req);

		UinBiometric biometrics = new UinBiometric();
		biometrics.setBiometricFileType("individualBiometrics");
		biometrics.setBiometricFileHash("W3LDtXpyxkl0YSifynsfhl7W-wWWtEb-ofkq-TGl1Lc");
		biometrics.setBioFileId("1234.cbeff");
		biometrics.setBiometricFileName("name");
		uinObj.setBiometrics(Lists.newArrayList(biometrics));
		uinObj.setUinData(("{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
				+ IdRepoConstants.FILE_NAME_ATTRIBUTE + "\":\"fileReferenceID\"}}").getBytes());
		when(environment.getProperty("mosip.idrepo.identity.uin-status.registered")).thenReturn(ACTIVATED);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("value".getBytes());
		when(anonymousProfileHelper.isNewCbeffPresent()).thenReturn(false);
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("http://localhost/v1/vid/{uin}");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		ResponseWrapper<AuthTypeStatusEventDTO> eventsResponse = new ResponseWrapper<>();
		eventsResponse.setResponse(new AuthTypeStatusEventDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(
				mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class), eventsResponse);
		IdResponseDTO<Object> updateIdentity = proxyService.updateIdentity(request.getRequest(), "1234");
		assertEquals(ACTIVATED, updateIdentity.getResponse().getStatus());
		ArgumentCaptor<CredentialRequestStatus> argCapture = ArgumentCaptor.forClass(CredentialRequestStatus.class);
		verify(credRequestRepo).save(argCapture.capture());
		CredentialRequestStatus credStatus = argCapture.getValue();
		assertEquals(CredentialRequestStatusLifecycle.NEW.toString(), credStatus.getStatus());
		assertEquals("", credStatus.getUpdatedBy());
		assertNotNull(credStatus.getUpdDTimes());
	}

	@Test
	public void testIdentityUpdateNewBioDocumentCredentialExistsRecordNotActive() throws Exception {
		when(credRequestRepo.findByIndividualIdHash(any())).thenReturn(List.of(new CredentialRequestStatus()));
		MockEnvironment mockEnv = new MockEnvironment();
		mockEnv.setProperty("mosip.fingerprint.fmr.enabled", "true");
		env.merge(mockEnv);
		ReflectionTestUtils.setField(service, "env", env);
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode("DEACTIVATED");
		IdRequestDTO req = mapper
				.readValue(("{\"identity\":{\"parentOrGuardianBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
						+ IdRepoConstants.FILE_NAME_ATTRIBUTE
						+ "\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"parentOrGuardianBiometrics\",\"value\":\"dGVzdA\"}]}")
						.getBytes(), IdRequestDTO.class);
		req.setRegistrationId("27841457360002620190730095024");
		request.setRequest(req);

		UinBiometric biometrics = new UinBiometric();
		biometrics.setBiometricFileType("individualBiometrics");
		biometrics.setBiometricFileHash("W3LDtXpyxkl0YSifynsfhl7W-wWWtEb-ofkq-TGl1Lc");
		biometrics.setBioFileId("1234.cbeff");
		biometrics.setBiometricFileName("name");
		uinObj.setBiometrics(Lists.newArrayList(biometrics));
		uinObj.setUinData(("{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
				+ IdRepoConstants.FILE_NAME_ATTRIBUTE + "\":\"fileReferenceID\"}}").getBytes());
		when(environment.getProperty("mosip.idrepo.identity.uin-status.registered")).thenReturn(ACTIVATED);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("value".getBytes());
		when(anonymousProfileHelper.isNewCbeffPresent()).thenReturn(false);
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("http://localhost/v1/vid/{uin}");
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		ResponseWrapper<AuthTypeStatusEventDTO> eventsResponse = new ResponseWrapper<>();
		eventsResponse.setResponse(new AuthTypeStatusEventDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(
				mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class), eventsResponse);
		IdResponseDTO<Object> updateIdentity = proxyService.updateIdentity(request.getRequest(), "1234");
		assertEquals("DEACTIVATED", updateIdentity.getResponse().getStatus());
		ArgumentCaptor<CredentialRequestStatus> argCapture = ArgumentCaptor.forClass(CredentialRequestStatus.class);
		verify(credRequestRepo).save(argCapture.capture());
		CredentialRequestStatus credStatus = argCapture.getValue();
		assertEquals(CredentialRequestStatusLifecycle.DELETED.toString(), credStatus.getStatus());
		assertEquals("", credStatus.getUpdatedBy());
		assertNotNull(credStatus.getUpdDTimes());
	}

	@Test
	public void testIdentityUpdateNewBioDocumentNonCbeff() throws Exception {
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		IdRequestDTO req = mapper
				.readValue(("{\"identity\":{\"parentOrGuardianBiometrics\":{\"format\":\"pdf\",\"version\":1.0,\""
						+ IdRepoConstants.FILE_NAME_ATTRIBUTE
						+ "\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"parentOrGuardianBiometrics\",\"value\":\"dGVzdA\"}]}")
						.getBytes(), IdRequestDTO.class);
		req.setRegistrationId("27841457360002620190730095024");
		request.setRequest(req);
		UinBiometric biometrics = new UinBiometric();
		biometrics.setBiometricFileType("individualBiometrics");
		biometrics.setBiometricFileHash("W3LDtXpyxkl0YSifynsfhl7W-wWWtEb-ofkq-TGl1Lc");
		biometrics.setBioFileId("1234.cbeff");
		biometrics.setBiometricFileName("name");
		uinObj.setBiometrics(Lists.newArrayList(biometrics));
		uinObj.setUinData(("{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\""
				+ IdRepoConstants.FILE_NAME_ATTRIBUTE + "\":\"fileReferenceID\"}}").getBytes());
		when(environment.getProperty("mosip.idrepo.identity.uin-status.registered")).thenReturn(ACTIVATED);
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("value".getBytes());
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("http://localhost/v1/vid/{uin}");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		ResponseWrapper<AuthTypeStatusEventDTO> eventsResponse = new ResponseWrapper<>();
		eventsResponse.setResponse(new AuthTypeStatusEventDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(
				mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class), eventsResponse);
		IdResponseDTO<Object> updateIdentity = proxyService.updateIdentity(request.getRequest(), "1234");
		assertEquals(ACTIVATED, updateIdentity.getResponse().getStatus());
	}

	@Test
	public void testIdentityUpdateBioDocumentIdRepoAppException() throws Exception {
		try {
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			IdRequestDTO req = mapper.readValue(
					"{\"identity\":{\"parentOrGuardianBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"parentOrGuardianBiometrics\",\"value\":\"dGVzdA\"}]}"
							.getBytes(),
					IdRequestDTO.class);
			req.setRegistrationId("27841457360002620190730095024");
			request.setRequest(req);
			UinBiometric biometrics = new UinBiometric();
			biometrics.setBiometricFileType("parentOrGuardianBiometrics");
			biometrics.setBiometricFileHash("W3LDtXpyxkl0YSifynsfhl7W-wWWtEb-ofkq-TGl1Lc");
			biometrics.setBioFileId("1234");
			biometrics.setBiometricFileName("name");
			uinObj.setUinHash("123_123");
			uinObj.setBiometrics(Collections.singletonList(biometrics));
			uinObj.setUinData(
					"{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}"
							.getBytes());
			when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
			when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("value".getBytes());
			when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any()))
					.thenThrow(new IdRepoAppUncheckedException(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR));
			proxyService.updateIdentity(request.getRequest(), "1234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testIdentityUpdateBioDocumentFSAdpapterException() throws Exception {
		try {
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			IdRequestDTO req = mapper.readValue(
					"{\"identity\":{\"parentOrGuardianBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"parentOrGuardianBiometrics\",\"value\":\"dGVzdA\"}]}"
							.getBytes(),
					IdRequestDTO.class);
			req.setRegistrationId("27841457360002620190730095024");
			request.setRequest(req);
			UinBiometric biometrics = new UinBiometric();
			biometrics.setBiometricFileType("parentOrGuardianBiometrics");
			biometrics.setBiometricFileHash("W3LDtXpyxkl0YSifynsfhl7W-wWWtEb-ofkq-TGl1Lc");
			biometrics.setBioFileId("1234");
			biometrics.setBiometricFileName("name");
			uinObj.setBiometrics(Collections.singletonList(biometrics));
			uinObj.setUinHash("123_123");
			uinObj.setUinData(
					"{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}"
							.getBytes());
			when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
			when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("value".getBytes());
			when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any())).thenThrow(
					new IdRepoAppUncheckedException(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(),
							IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage()));
			proxyService.updateIdentity(request.getRequest(), "1234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.FILE_STORAGE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testIdentityUpdateBioDocumentException() throws Exception {
		try {
			Uin uinObj = new Uin();
			uinObj.setUin("1234");
			uinObj.setUinRefId("1234");
			IdRequestDTO req = mapper.readValue(
					"{\"identity\":{\"parentOrGuardianBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"parentOrGuardianBiometrics\",\"value\":\"dGVzdA\"}]}"
							.getBytes(),
					IdRequestDTO.class);
			req.setRegistrationId("27841457360002620190730095024");
			request.setRequest(req);
			UinBiometric biometrics = new UinBiometric();
			biometrics.setBiometricFileType("parentOrGuardianBiometrics");
			biometrics.setBiometricFileHash("W3LDtXpyxkl0YSifynsfhl7W-wWWtEb-ofkq-TGl1Lc");
			biometrics.setBioFileId("1234");
			biometrics.setBiometricFileName("name");
			uinObj.setBiometrics(Collections.singletonList(biometrics));
			uinObj.setUinData(
					"{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}"
							.getBytes());
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
			when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
			when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("value".getBytes());
			proxyService.updateIdentity(request.getRequest(), "1234");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.RECORD_EXISTS.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.RECORD_EXISTS.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testIdentityUpdateNewDemoDocuments() throws Exception {
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		IdRequestDTO req = mapper
				.readValue(("{\"identity\":{\"proofOfRelationship\":{\"format\":\"pdf\",\"type\":\"1.0\",\""
						+ IdRepoConstants.FILE_NAME_ATTRIBUTE
						+ "\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"proofOfRelationship\",\"value\":\"dGVzdA\"}]}")
						.getBytes(), IdRequestDTO.class);
		req.setRegistrationId("27841457360002620190730095024");
		request.setRequest(req);
		UinDocument document = new UinDocument();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocHash("W3LDtXpyxkl0YSifynsfhl7W-wWWtEb-ofkq-TGl1Lc");
		document.setDocId("1234");
		document.setDocName("name");
		uinObj.setDocuments(Lists.newArrayList(document));
		uinObj.setUinData(("{\"ProofOfIdentity\":{\"format\":\"pdf\",\"version\":1.0,\""
				+ IdRepoConstants.FILE_NAME_ATTRIBUTE + "\":\"fileReferenceID\"}}").getBytes());
		when(environment.getProperty("mosip.idrepo.identity.uin-status.registered")).thenReturn(ACTIVATED);
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("value".getBytes());
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("http://localhost/v1/vid/{uin}");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		ResponseWrapper<AuthTypeStatusEventDTO> eventsResponse = new ResponseWrapper<>();
		eventsResponse.setResponse(new AuthTypeStatusEventDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(
				mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class), eventsResponse);
		IdResponseDTO<Object> updateIdentity = proxyService.updateIdentity(request.getRequest(), "1234");
		assertEquals(ACTIVATED, updateIdentity.getResponse().getStatus());
	}

	@Test
	public void testIdentityUpdateDemoDocuments() throws Exception {
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode(ACTIVATED);
		IdRequestDTO req = mapper.readValue(("{\"identity\":{\"ProofOfIdentity\":{\"format\":\"pdf\",\"type\":\"1.0\",\""
				+ IdRepoConstants.FILE_NAME_ATTRIBUTE
				+ "\":\"fileReferenceID\"}},\"documents\":[{\"category\":\"ProofOfIdentity\",\"value\":\"dGVzdA\"}]}")
				.getBytes(), IdRequestDTO.class);
		req.setRegistrationId("27841457360002620190730095024");
		request.setRequest(req);
		UinDocument document = new UinDocument();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocHash("W3LDtXpyxkl0YSifynsfhl7W-wWWtEb-ofkq-TGl1Lc");
		document.setDocId("1234");
		document.setDocName("name");
		uinObj.setDocuments(Lists.newArrayList(document));
		uinObj.setUinData(("{\"ProofOfIdentity\":{\"format\":\"pdf\",\"version\":1.0,\""
				+ IdRepoConstants.FILE_NAME_ATTRIBUTE + "\":\"fileReferenceID\"}}").getBytes());
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		when(environment.getProperty("mosip.idrepo.identity.uin-status.registered")).thenReturn(ACTIVATED);
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(cbeffUtil.updateXML(Mockito.any(), Mockito.any())).thenReturn("value".getBytes());
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("http://localhost/v1/vid/{uin}");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		ResponseWrapper<AuthTypeStatusEventDTO> eventsResponse = new ResponseWrapper<>();
		eventsResponse.setResponse(new AuthTypeStatusEventDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(
				mapper.readValue("{\"response\":{\"data\":\"1234\"}}".getBytes(), ObjectNode.class), eventsResponse);
		IdResponseDTO<Object> updateIdentity = proxyService.updateIdentity(request.getRequest(), "1234");
		assertEquals(ACTIVATED, updateIdentity.getResponse().getStatus());
	}

	@Test
	public void testRetriveIdentityByRid_Valid() throws JsonProcessingException, IdRepoAppException {
		String value = "6158236213";
		String ridValue = "27847657360002520190320095029";
		when(objectStoreHelper.getDemographicObject(Mockito.any(), Mockito.any())).thenReturn("data".getBytes());
		Uin uinObj = new Uin();
		uinObj.setUin(value);
		uinObj.setUinHash("213_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		uinObj.setUinRefId(ridValue);
		UinDocument document = new UinDocument();
		document.setDoccatCode("ProofOfIdentity");
		document.setDocHash("3A6EB0790F39AC87C94F3856B2DD2C5D110E6811602261A9A923D3BB23ADC8B7");
		document.setDocId("1234");
		document.setDocName("name");
		uinObj.setDocuments(Collections.singletonList(document));
		String identity = "{\"ProofOfIdentity\":{\"format\":\"pdf\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}";
		uinObj.setUinData(identity.getBytes());
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		Mockito.when(uinRepo.getUinHashByRid(Mockito.anyString())).thenReturn(value);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		IdResponseDTO<Object> idResponseDTO = ReflectionTestUtils.invokeMethod(proxyService, "retrieveIdentity", ridValue,
				IdType.ID, "demo", null);
		assertEquals(identity, mapper.writeValueAsString(idResponseDTO.getResponse().getIdentity()));
	}

	@Test(expected = IdRepoAppException.class)
	public void testRetrieveIdentityByRid_Invalid() throws Throwable {
		try {
			Mockito.when(uinRepo.getUinHashByRid(Mockito.anyString())).thenReturn(null);
			ReflectionTestUtils.invokeMethod(proxyService, "retrieveIdentity", "27847657360002520190320095029",
					IdType.ID, "demo", null);
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	/**
	 * Test retrieve identity.
	 *
	 * @throws IdRepoAppException   the id repo app exception
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	@Test
	public void testRetrieveIdentityByVid_valid()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		RestRequestDTO restRequest = new RestRequestDTO();
		restRequest.setUri("{vid}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restRequest);
		ResponseWrapper<Map<String, String>> vidResponse = new ResponseWrapper<>();
		vidResponse.setResponse(Map.of("UIN", "1234"));
		when(restHelper.requestSync(any())).thenReturn(vidResponse);
		when(securityManagerMock.getSaltKeyForId(any())).thenReturn(1);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(securityManagerMock.hashwithSalt(any(), any())).thenReturn("hash");
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		String identity = "{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}";
		uinObj.setUinData(identity.getBytes());
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		IdResponseDTO<Object> retrieveIdentityByUin = proxyService.retrieveIdentity("1234", IdType.VID, null, null);
		assertEquals(identity, mapper.writeValueAsString(retrieveIdentityByUin.getResponse().getIdentity()));
	}

	@Test
	public void testRetrieveIdentityByVid_invalid()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		RestRequestDTO restRequest = new RestRequestDTO();
		restRequest.setUri("{vid}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restRequest);
		ResponseWrapper<Map<String, String>> vidResponse = new ResponseWrapper<>();
		vidResponse.setResponse(Map.of("UIN", "1234"));
		vidResponse.setErrors(List.of(new ServiceError(IdRepoErrorConstants.INVALID_UIN.getErrorCode(),
				IdRepoErrorConstants.INVALID_UIN.getErrorMessage())));
		when(restHelper.requestSync(any())).thenThrow(new RestServiceException(IdRepoErrorConstants.CLIENT_ERROR,
				mapper.writeValueAsString(vidResponse), vidResponse));
		try {
			proxyService.retrieveIdentity("1234", IdType.VID, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_UIN.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.INVALID_UIN.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityByVid_rest_call_failed()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		RestRequestDTO restRequest = new RestRequestDTO();
		restRequest.setUri("{vid}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restRequest);
		when(restHelper.requestSync(any())).thenThrow(new RestServiceException(IdRepoErrorConstants.CLIENT_ERROR));
		try {
			proxyService.retrieveIdentity("1234", IdType.VID, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@SuppressWarnings("serial")
	@Test
	public void testRetrieveIdentityByRid_db_call_failed()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		when(uinRepo.getUinHashByRid(any())).thenThrow(new DataAccessException("") {
		});
		try {
			proxyService.retrieveIdentity("1234", IdType.ID, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityByRid_unchecked_exception()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		when(uinRepo.getUinHashByRid(any()))
				.thenThrow(new IdRepoAppUncheckedException(IdRepoErrorConstants.DATABASE_ACCESS_ERROR));
		try {
			proxyService.retrieveIdentity("1234", IdType.ID, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testAddIdentityUncheckedException() {
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		when(securityManagerMock.getSaltKeyForId(any()))
				.thenThrow(new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			proxyService.addIdentity(null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityWithBioDocumentsAndWithExtractionFormatInvalidExtractionFormat() throws Exception {
		String cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		byte[] cbeffXml = CryptoUtil.decodeURLSafeBase64(cbeff);
		when(cbeffUtil.createXML(any())).thenReturn(cbeffXml);
		when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any())).thenReturn(cbeffXml);
//		when(biometricExtractionService.extractTemplate(any(), any(), any(), any(), any()))
//				.thenReturn(CompletableFuture.completedFuture(CbeffValidator.getBIRFromXML(cbeffXml).getBirs()));
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		UinBiometric biometrics = new UinBiometric();
		biometrics.setBiometricFileType("individualBiometrics");
		biometrics.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometrics.setBioFileId("1234");
		biometrics.setBiometricFileName("name");
		uinObj.setBiometrics(Collections.singletonList(biometrics));
		String identityWithDoc = "{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}";
		uinObj.setUinData(identityWithDoc.getBytes());
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		IdResponseDTO<Object> retrieveIdentityByUin = proxyService.retrieveIdentity("1234", IdType.UIN, "bio",
				Map.of("extraction", "format"));
		assertEquals(identityWithDoc, mapper.writeValueAsString(retrieveIdentityByUin.getResponse().getIdentity()));
	}

	@Test
	public void testRetrieveIdentityWithBioDocumentsAndWithExtractionFormatValidExtractionFormat() throws Exception {
		String cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		byte[] cbeffXml = CryptoUtil.decodeURLSafeBase64(cbeff);
		when(cbeffUtil.createXML(any())).thenReturn(cbeffXml);
		when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any())).thenReturn(cbeffXml);
		when(biometricExtractionService.extractTemplate(any(), any(), any(), any(), any()))
				.thenReturn(CompletableFuture.completedFuture(CbeffValidator.getBIRFromXML(cbeffXml).getBirs()));
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		UinBiometric biometrics = new UinBiometric();
		biometrics.setBiometricFileType("individualBiometrics");
		biometrics.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometrics.setBioFileId("1234");
		biometrics.setBiometricFileName("name");
		uinObj.setBiometrics(Collections.singletonList(biometrics));
		String identityWithDoc = "{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}";
		uinObj.setUinData(identityWithDoc.getBytes());
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		IdResponseDTO<Object> retrieveIdentityByUin = proxyService.retrieveIdentity("1234", IdType.UIN, "bio",
				Map.of("fingerExtractionFormat", "format"));
		assertEquals(identityWithDoc, mapper.writeValueAsString(retrieveIdentityByUin.getResponse().getIdentity()));
	}

	@Test
	public void testRetrieveIdentityWithBioDocumentsBioExtractionFailed() throws Exception {
		String cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		byte[] cbeffXml = CryptoUtil.decodeURLSafeBase64(cbeff);
		when(cbeffUtil.createXML(any())).thenReturn(cbeffXml);
		when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any())).thenReturn(cbeffXml);
		when(biometricExtractionService.extractTemplate(any(), any(), any(), any(), any()))
				.thenThrow(new NullPointerException());
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		UinBiometric biometrics = new UinBiometric();
		biometrics.setBiometricFileType("individualBiometrics");
		biometrics.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometrics.setBioFileId("1234");
		biometrics.setBiometricFileName("name");
		uinObj.setBiometrics(Collections.singletonList(biometrics));
		String identityWithDoc = "{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}";
		uinObj.setUinData(identityWithDoc.getBytes());
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		try {
			proxyService.retrieveIdentity("1234", IdType.UIN, "bio", Map.of("fingerExtractionFormat", "format"));
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.BIO_EXTRACTION_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.BIO_EXTRACTION_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveIdentityWithBioDocumentsUncheckedException() throws Exception {
		String cbeff = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("test-cbeff.xml"),
				StandardCharsets.UTF_8);
		byte[] cbeffXml = CryptoUtil.decodeURLSafeBase64(cbeff);
		when(cbeffUtil.createXML(any())).thenReturn(cbeffXml);
		when(objectStoreHelper.getBiometricObject(Mockito.any(), Mockito.any())).thenReturn(cbeffXml);
		when(biometricExtractionService.extractTemplate(any(), any(), any(), any(), any()))
				.thenThrow(new IdRepoAppUncheckedException(IdRepoErrorConstants.BIO_EXTRACTION_ERROR));
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setUinHash("234_5B72C3B57A72C6497461289FCA7B1F865ED6FB0596B446FEA1F92AF931A5D4B7");
		UinBiometric biometrics = new UinBiometric();
		biometrics.setBiometricFileType("individualBiometrics");
		biometrics.setBiometricFileHash("A2C07E94066BE52308E96ABAD995035E62985A1B0D8837E9ACAB47F8F8A52014");
		biometrics.setBioFileId("1234");
		biometrics.setBiometricFileName("name");
		uinObj.setBiometrics(Collections.singletonList(biometrics));
		String identityWithDoc = "{\"individualBiometrics\":{\"format\":\"cbeff\",\"version\":1.0,\"fileReference\":\"fileReferenceID\"}}";
		uinObj.setUinData(identityWithDoc.getBytes());
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		try {
			proxyService.retrieveIdentity("1234", IdType.UIN, "bio", Map.of("fingerExtractionFormat", "format"));
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.BIO_EXTRACTION_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.BIO_EXTRACTION_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetreiveIdentityRecordNotFound() {
		when(uinRepo.findByUinHash(any())).thenReturn(Optional.empty());
		try {
			service.retrieveIdentity("", IdType.UIN, null, null);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateIdentityUpdateVerifiedAttributes()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		Object obj = mapper.readValue(
				"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		IdRequestDTO req = new IdRequestDTO();
		req.setStatus("REGISTERED");
		req.setRegistrationId("27841457360002620190730095024");
		req.setIdentity(obj);
		req.setVerifiedAttributes(List.of("a", "b"));
		request.setRequest(req);
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode("REGISTERED");
		Object obj2 = mapper.readValue(
				"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Mano\",\"label\":\"string\"}],\"lastName\":[{\"language\":\"AR\",\"value\":\"Mano\",\"label\":\"string\"},{\"language\":\"FR\",\"value\":\"Mano\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		uinObj.setUinData(mapper.writeValueAsBytes(obj2));
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(anonymousProfileHelper.isNewCbeffPresent()).thenReturn(false);
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("http://localhost/v1/vid/{uin}");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		ResponseWrapper<AuthTypeStatusEventDTO> eventsResponse = new ResponseWrapper<>();
		eventsResponse.setResponse(new AuthTypeStatusEventDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(eventsResponse);
		Uin updatedIdentity = service.updateIdentity(request.getRequest(), "234");
		List<String> verifiedAttributes = (List<String>) mapper.readValue(updatedIdentity.getUinData(), Map.class)
				.get("verifiedAttributes");
		assertEquals(List.of("a", "b"), verifiedAttributes);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateIdentityUpdateVerifiedAttributesV2()
			throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		Object obj = mapper.readValue(
				"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Manoj\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		IdRequestDTO req = new IdRequestDTO();
		req.setStatus("REGISTERED");
		req.setRegistrationId("27841457360002620190730095024");
		req.setIdentity(obj);
		req.setVerifiedAttributes(Map.of("a", "b"));
		request.setRequest(req);
		Uin uinObj = new Uin();
		uinObj.setUin("1234");
		uinObj.setUinRefId("1234");
		uinObj.setStatusCode("REGISTERED");
		Object obj2 = mapper.readValue(
				"{\"identity\":{\"firstName\":[{\"language\":\"AR\",\"value\":\"Mano\",\"label\":\"string\"}],\"lastName\":[{\"language\":\"AR\",\"value\":\"Mano\",\"label\":\"string\"},{\"language\":\"FR\",\"value\":\"Mano\",\"label\":\"string\"}]}}"
						.getBytes(),
				Object.class);
		uinObj.setUinData(mapper.writeValueAsBytes(obj2));
		when(uinDraftRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.findByUinHash(Mockito.any())).thenReturn(Optional.of(uinObj));
		when(uinRepo.save(Mockito.any())).thenReturn(uinObj);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(anonymousProfileHelper.isNewCbeffPresent()).thenReturn(false);
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("http://localhost/v1/vid/{uin}");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(restReq);
		ResponseWrapper<AuthTypeStatusEventDTO> eventsResponse = new ResponseWrapper<>();
		eventsResponse.setResponse(new AuthTypeStatusEventDTO());
		when(restHelper.requestSync(Mockito.any())).thenReturn(eventsResponse);
		Uin updatedIdentity = service.updateIdentity(request.getRequest(), "234");
		Map<String, Object> verifiedAttributes = (Map) mapper.readValue(updatedIdentity.getUinData(), Map.class)
				.get("verifiedAttributes");
		assertEquals(Map.of("a", "b"), verifiedAttributes);
	}

	@Test
	public void testGetRidByUin() throws IdRepoAppException {
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		when(securityManagerMock.getSaltKeyForId(any())).thenReturn(1);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(securityManagerMock.hashwithSalt(any(), any())).thenReturn("hash");
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.getRidByUinHash(Mockito.any())).thenReturn("1234");
		String ridResponse = proxyService.getRidByIndividualId("1234", IdType.UIN);
		assertEquals("1234", ridResponse);
	}

	@Test
	public void testGetRidById() throws IdRepoAppException {
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		when(securityManagerMock.getSaltKeyForId(any())).thenReturn(1);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(securityManagerMock.hashwithSalt(any(), any())).thenReturn("hash");
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(true);
		when(uinRepo.getRidByUinHash(Mockito.any())).thenReturn("1234");
		String ridResponse = proxyService.getRidByIndividualId("1234", IdType.ID);
		assertEquals("1234", ridResponse);
	}

	@Test
	public void testGetRidByIdNoRecords() throws IdRepoAppException {
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		when(securityManagerMock.getSaltKeyForId(any())).thenReturn(1);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(securityManagerMock.hashwithSalt(any(), any())).thenReturn("hash");
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.existsByRegId(Mockito.any())).thenReturn(false);
		when(uinRepo.getRidByUinHash(Mockito.any())).thenReturn("1234");
		try {
			proxyService.getRidByIndividualId("1234", IdType.ID);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorMessage(), "individualId"),
					e.getErrorText());
		}
	}

	@Test
	public void testGetRidByVid() throws IdRepoAppException {
		RestRequestDTO restRequest = new RestRequestDTO();
		restRequest.setUri("{vid}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restRequest);
		ResponseWrapper<Map<String, String>> vidResponse = new ResponseWrapper<>();
		vidResponse.setResponse(Map.of("UIN", "1234"));
		when(restHelper.requestSync(any())).thenReturn(vidResponse);
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		when(securityManagerMock.getSaltKeyForId(any())).thenReturn(1);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(securityManagerMock.hashwithSalt(any(), any())).thenReturn("hash");
		when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(true);
		when(uinRepo.getRidByUinHash(Mockito.any())).thenReturn("1234");
		String ridResponse = proxyService.getRidByIndividualId("1234", IdType.VID);
		assertEquals("1234", ridResponse);
	}

	@Test
	public void testGetRidByUinNoRecord() throws IdRepoAppException {
		try {
			IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
			ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
			when(securityManagerMock.getSaltKeyForId(any())).thenReturn(1);
			when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			when(securityManagerMock.hashwithSalt(any(), any())).thenReturn("hash");
			when(uinRepo.existsByUinHash(Mockito.any())).thenReturn(false);
			proxyService.getRidByIndividualId("1234", IdType.UIN);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorMessage(), "individualId"),
					e.getErrorText());
		}
	}

	@Test
	public void testGetRemainingUpdateCountByIndividualIdwithUINIdType_valid() throws IdRepoAppException {
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		when(securityManagerMock.getSaltKeyForId(any())).thenReturn(1);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(securityManagerMock.hashwithSalt(any(), any())).thenReturn("hash");
		IdentityUpdateTracker record = new IdentityUpdateTracker();
		record.setId("id");
		record.setIdentityUpdateCount(CryptoUtil.encodeToURLSafeBase64("{\"fullName\":2}".getBytes()).getBytes());
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.of(record));
		ReflectionTestUtils.setField(IdentityUpdateTrackerPolicyProvider.class, "updateCount", Map.of("fullName", 2));
		Map<String, Integer> response = proxyService.getRemainingUpdateCountByIndividualId("1234", IdType.UIN, null);
		assertEquals(Map.of("fullName", 0), response);
	}

	@Test
	public void testGetRemainingUpdateCountByIndividualIdwithUINIdType_UINNotExist() throws IdRepoAppException {
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		when(securityManagerMock.getSaltKeyForId(any())).thenReturn(1);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(securityManagerMock.hashwithSalt(any(), any())).thenReturn("hash");
		IdentityUpdateTracker record = new IdentityUpdateTracker();
		record.setId("id");
		record.setIdentityUpdateCount(CryptoUtil.encodeToURLSafeBase64("{\"fullName\":2}".getBytes()).getBytes());
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.empty());
		ReflectionTestUtils.setField(IdentityUpdateTrackerPolicyProvider.class, "updateCount", Map.of("fullName", 2));
		proxyService.getRemainingUpdateCountByIndividualId("1234", IdType.UIN, null);
	}

	@Test
	public void testGetRemainingUpdateCountByIndividualIdwithIDIdType_valid() throws IdRepoAppException {
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		when(securityManagerMock.getSaltKeyForId(any())).thenReturn(1);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(securityManagerMock.hashwithSalt(any(), any())).thenReturn("hash");
		IdentityUpdateTracker record = new IdentityUpdateTracker();
		record.setId("id");
		record.setIdentityUpdateCount(CryptoUtil.encodeToURLSafeBase64("{\"fullName\":2}".getBytes()).getBytes());
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.of(record));
		ReflectionTestUtils.setField(IdentityUpdateTrackerPolicyProvider.class, "updateCount", Map.of("fullName", 2));
		when(uinRepo.existsByRegId(any())).thenReturn(true);
		when(uinRepo.getUinHashByRid(any())).thenReturn("1234");
		Map<String, Integer> response = proxyService.getRemainingUpdateCountByIndividualId("1234", IdType.ID, null);
		assertEquals(Map.of("fullName", 0), response);
	}

	@Test(expected = IdRepoAppException.class)
	public void testGetRemainingUpdateCountByIndividualIdwithIDIdType_RIDNotExist() throws IdRepoAppException {
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		when(securityManagerMock.getSaltKeyForId(any())).thenReturn(1);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(securityManagerMock.hashwithSalt(any(), any())).thenReturn("hash");
		IdentityUpdateTracker record = new IdentityUpdateTracker();
		record.setId("id");
		record.setIdentityUpdateCount(CryptoUtil.encodeToURLSafeBase64("{\"fullName\":2}".getBytes()).getBytes());
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.of(record));
		ReflectionTestUtils.setField(IdentityUpdateTrackerPolicyProvider.class, "updateCount", Map.of("fullName", 2));
		when(uinRepo.existsByRegId(any())).thenReturn(false);
		when(uinRepo.getUinHashByRid(any())).thenReturn("1234");
		proxyService.getRemainingUpdateCountByIndividualId("1234", IdType.ID, null);
	}

	@Test
	public void testGetRemainingUpdateCountByIndividualIdwithVIDIdType_valid() throws IdRepoAppException {
		RestRequestDTO restRequest = new RestRequestDTO();
		restRequest.setUri("{vid}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restRequest);
		ResponseWrapper<Map<String, String>> vidResponse = new ResponseWrapper<>();
		vidResponse.setResponse(Map.of("UIN", "1234"));
		when(restHelper.requestSync(any())).thenReturn(vidResponse);
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		when(securityManagerMock.getSaltKeyForId(any())).thenReturn(1);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(securityManagerMock.hashwithSalt(any(), any())).thenReturn("hash");
		IdentityUpdateTracker record = new IdentityUpdateTracker();
		record.setId("id");
		record.setIdentityUpdateCount(CryptoUtil.encodeToURLSafeBase64("{\"fullName\":2}".getBytes()).getBytes());
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.of(record));
		ReflectionTestUtils.setField(IdentityUpdateTrackerPolicyProvider.class, "updateCount", Map.of("fullName", 2));
		when(uinRepo.existsByRegId(any())).thenReturn(true);
		when(uinRepo.getUinHashByRid(any())).thenReturn("1234");
		Map<String, Integer> response = proxyService.getRemainingUpdateCountByIndividualId("1234", IdType.VID, List.of());
		assertEquals(Map.of("fullName", 0), response);
	}

	@Test(expected = IdRepoAppException.class)
	public void testGetRemainingUpdateCountByIndividualIdwithVIDIdType_InvalidTrackerData() throws IdRepoAppException {
		RestRequestDTO restRequest = new RestRequestDTO();
		restRequest.setUri("{vid}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restRequest);
		ResponseWrapper<Map<String, String>> vidResponse = new ResponseWrapper<>();
		vidResponse.setResponse(Map.of("UIN", "1234"));
		when(restHelper.requestSync(any())).thenReturn(vidResponse);
		IdRepoSecurityManager securityManagerMock = mock(IdRepoSecurityManager.class);
		ReflectionTestUtils.setField(proxyService, "securityManager", securityManagerMock);
		when(securityManagerMock.getSaltKeyForId(any())).thenReturn(1);
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(securityManagerMock.hashwithSalt(any(), any())).thenReturn("hash");
		IdentityUpdateTracker record = new IdentityUpdateTracker();
		record.setId("id");
		record.setIdentityUpdateCount(CryptoUtil.encodeToURLSafeBase64("{\"fullName\"2}".getBytes()).getBytes());
		when(identityUpdateTracker.findById(any())).thenReturn(Optional.of(record));
		ReflectionTestUtils.setField(IdentityUpdateTrackerPolicyProvider.class, "updateCount", Map.of("fullName", 2));
		when(uinRepo.existsByRegId(any())).thenReturn(true);
		when(uinRepo.getUinHashByRid(any())).thenReturn("1234");
		proxyService.getRemainingUpdateCountByIndividualId("1234", IdType.VID, List.of());
	}
}
