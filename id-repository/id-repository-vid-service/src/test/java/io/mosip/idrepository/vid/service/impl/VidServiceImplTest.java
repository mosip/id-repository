package io.mosip.idrepository.vid.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mvel2.MVEL;
import org.powermock.api.mockito.PowerMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.dto.VidPolicy;
import io.mosip.idrepository.core.dto.VidRequestDTO;
import io.mosip.idrepository.core.dto.VidResponseDTO;
import io.mosip.idrepository.core.dto.VidsInfosDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.vid.entity.Vid;
import io.mosip.idrepository.vid.provider.VidPolicyProvider;
import io.mosip.idrepository.vid.repository.VidRepo;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.websub.model.EventModel;

/**
 * @author Manoj SP
 * @author Prem Kumar
 *
 */
@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class,MVEL.class })
@RunWith(SpringRunner.class)
@WebMvcTest @Import(EnvUtil.class)
@ActiveProfiles("test")
@ConfigurationProperties("mosip.idrepo.vid")
public class VidServiceImplTest {

	@InjectMocks
	private VidServiceImpl service;

	@Mock
	private VidRepo vidRepo;

	@Mock
	private VidPolicyProvider vidPolicyProvider;

	@Mock
	private RestRequestBuilder restBuilder;

	@Mock
	private RestHelper restHelper;

	@Mock
	private WebClient webClient;

	/** The security manager. */
	@Mock
	private IdRepoSecurityManager securityManager;

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	EnvUtil environment;

	@Mock
	private UinHashSaltRepo uinHashSaltRepo;

	@Mock
	private UinEncryptSaltRepo uinEncryptSaltRepo;
	
	@Mock
	private CredentialServiceManager credServiceManager;
	
	@Mock
	private IdRepoWebSubHelper websubHelper;

	private Map<String, String> id;

	public void setId(Map<String, String> id) {
		this.id = id;
	}

	@Before
	public void before() {
		ReflectionTestUtils.setField(restHelper, "mapper", mapper);
		ReflectionTestUtils.setField(service, "id", id);
		ReflectionTestUtils.setField(service, "vidActiveStatus", "ACTIVE");
		ReflectionTestUtils.setField(service, "allowedStatus", "ACTIVE,REVOKED,EXPIRED,USED,INVALIDATED,DEACTIVATED");
		
	}

	@Test
	public void testCreateDraftVid() throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		when(securityManager.hash(Mockito.any())).thenReturn("123");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		when(restHelper.requestSync(Mockito.any())).thenReturn(identityResponse);
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(2);
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(policy);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		when(vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(vid));
		when(vidRepo.save(Mockito.any())).thenReturn(vid);
		VidRequestDTO request = new VidRequestDTO();
		request.setUin("2953190571");
		request.setVidStatus(IdRepoConstants.DRAFT_STATUS);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		RestRequestDTO restReq = new RestRequestDTO();
		when(restBuilder.buildRequest(RestServicesConstants.VID_GENERATOR_SERVICE, null, ResponseWrapper.class)).thenReturn(restReq);
		ResponseWrapper<Object> responseWrapper = new ResponseWrapper<>();
		ObjectNode node = mapper.createObjectNode();
		node.put("vid", "12345");
		responseWrapper.setResponse(mapper.readValue(node.toString(), Object.class));
		when(restHelper.requestSync(restReq)).thenReturn(responseWrapper);
		ResponseWrapper<VidResponseDTO> vidResponse = service.generateVid(request);
		assertEquals(vidResponse.getResponse().getVid().toString(), vid.getVid());
		assertEquals(vidResponse.getResponse().getVidStatus(), vid.getStatusCode());
	}

	@Test
	public void testCreateDraftVidRestDataValidationFailure() throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		MockEnvironment env = new MockEnvironment();
		ReflectionTestUtils.setField(environment, "env", env);
		when(securityManager.hash(Mockito.any())).thenReturn("123");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenThrow(new IdRepoDataValidationException(IdRepoErrorConstants.INVALID_INPUT_PARAMETER));
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		when(restHelper.requestSync(Mockito.any())).thenReturn(identityResponse);
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(2);
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(policy);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		when(vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(vid));
		when(vidRepo.save(Mockito.any())).thenReturn(vid);
		VidRequestDTO request = new VidRequestDTO();
		request.setUin("2953190571");
		request.setVidStatus(IdRepoConstants.DRAFT_STATUS);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		RestRequestDTO restReq = new RestRequestDTO();
		ResponseWrapper<Object> responseWrapper = new ResponseWrapper<>();
		ObjectNode node = mapper.createObjectNode();
		node.put("vid", "12345");
		responseWrapper.setResponse(mapper.readValue(node.toString(), Object.class));
		when(restHelper.requestSync(restReq)).thenReturn(responseWrapper);
		try {
			service.generateVid(request);
		} catch (IdRepoAppException e) {
			assertEquals("Invalid Input Parameter - %s", e.getErrorText());
		}
	}

	@Test
	public void testCreateDraftVidWithExpiry() throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		when(securityManager.hash(Mockito.any())).thenReturn("123");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		when(restHelper.requestSync(Mockito.any())).thenReturn(identityResponse);
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(2);
		policy.setValidForInMinutes(2);
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(policy);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		when(vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(vid));
		when(vidRepo.save(Mockito.any())).thenReturn(vid);
		VidRequestDTO request = new VidRequestDTO();
		request.setUin("2953190571");
		request.setVidStatus(IdRepoConstants.DRAFT_STATUS);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		RestRequestDTO restReq = new RestRequestDTO();
		when(restBuilder.buildRequest(RestServicesConstants.VID_GENERATOR_SERVICE, null, ResponseWrapper.class)).thenReturn(restReq);
		ResponseWrapper<Object> responseWrapper = new ResponseWrapper<>();
		ObjectNode node = mapper.createObjectNode();
		node.put("vid", "12345");
		responseWrapper.setResponse(mapper.readValue(node.toString(), Object.class));
		when(restHelper.requestSync(restReq)).thenReturn(responseWrapper);
		ResponseWrapper<VidResponseDTO> vidResponse = service.generateVid(request);
		assertEquals(vidResponse.getResponse().getVid().toString(), vid.getVid());
		assertEquals(vidResponse.getResponse().getVidStatus(), vid.getStatusCode());
	}

	@Test
	public void testCreateVid() throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		when(securityManager.hash(Mockito.any())).thenReturn("123");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		when(restHelper.requestSync(Mockito.any())).thenReturn(identityResponse);
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(2);
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(policy);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		when(vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(vid));
		when(vidRepo.save(Mockito.any())).thenReturn(vid);
		VidRequestDTO request = new VidRequestDTO();
		request.setUin("2953190571");
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		RestRequestDTO restReq = new RestRequestDTO();
		when(restBuilder.buildRequest(RestServicesConstants.VID_GENERATOR_SERVICE, null, ResponseWrapper.class)).thenReturn(restReq);
		ResponseWrapper<Object> responseWrapper = new ResponseWrapper<>();
		ObjectNode node = mapper.createObjectNode();
		node.put("vid", "12345");
		responseWrapper.setResponse(mapper.readValue(node.toString(), Object.class));
		when(restHelper.requestSync(restReq)).thenReturn(responseWrapper);
		ResponseWrapper<VidResponseDTO> vidResponse = service.generateVid(request);
		assertEquals(vidResponse.getResponse().getVid().toString(), vid.getVid());
		assertEquals(vidResponse.getResponse().getVidStatus(), vid.getStatusCode());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateVidAutoRestore() throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		when(securityManager.hash(Mockito.any())).thenReturn("123");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		when(restHelper.requestSync(Mockito.any())).thenReturn(identityResponse);
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(1);
		policy.setAutoRestoreAllowed(true);
		policy.setRestoreOnAction("REVOKED");
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(policy);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		when(vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(vid), Collections.emptyList());
		when(vidRepo.save(Mockito.any())).thenReturn(vid);
		VidRequestDTO request = new VidRequestDTO();
		request.setUin("2953190571");
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		RestRequestDTO restReq = new RestRequestDTO();
		when(restBuilder.buildRequest(RestServicesConstants.VID_GENERATOR_SERVICE, null, ResponseWrapper.class)).thenReturn(restReq);
		ResponseWrapper<Object> responseWrapper = new ResponseWrapper<>();
		ObjectNode node = mapper.createObjectNode();
		node.put("vid", "12345");
		responseWrapper.setResponse(mapper.readValue(node.toString(), Object.class));
		when(restHelper.requestSync(restReq)).thenReturn(responseWrapper);
		ResponseWrapper<VidResponseDTO> vidResponse = service.generateVid(request);
		assertEquals(vidResponse.getResponse().getVid().toString(), vid.getVid());
		assertEquals(vidResponse.getResponse().getVidStatus(), vid.getStatusCode());
	}
	

	@Test
	public void testCreateVidInstanceFail() throws RestServiceException, IdRepoDataValidationException, JsonParseException, JsonMappingException, IOException {
		when(securityManager.hash(Mockito.any())).thenReturn("123");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		when(restHelper.requestSync(Mockito.any())).thenReturn(identityResponse);
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(2);
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(policy);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		when(vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(vid));
		when(vidRepo.save(Mockito.any())).thenReturn(vid);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		VidRequestDTO request = new VidRequestDTO();
		request.setUin("2953190571");
		RestRequestDTO restReq = new RestRequestDTO();
		when(restBuilder.buildRequest(RestServicesConstants.VID_GENERATOR_SERVICE, null, ResponseWrapper.class)).thenReturn(restReq);
		ResponseWrapper<Object> responseWrapper = new ResponseWrapper<>();
		ObjectNode node = mapper.createObjectNode();
		node.put("vid", "12345");
		responseWrapper.setResponse(mapper.readValue(node.toString(), Object.class));
		when(restHelper.requestSync(restReq)).thenReturn(responseWrapper);
		try {
			service.generateVid(request);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.VID_POLICY_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.VID_POLICY_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testCreateVidPolicyFailed() throws RestServiceException, IdRepoDataValidationException {
		when(securityManager.hash(Mockito.any())).thenReturn("123");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		when(restHelper.requestSync(Mockito.any())).thenReturn(identityResponse);
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(0);
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(policy);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		when(vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(vid));
		when(vidRepo.save(Mockito.any())).thenReturn(vid);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		VidRequestDTO request = new VidRequestDTO();
		request.setUin("2953190571");
		request.setVidType("Perpetual");
		try {
			service.generateVid(request);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.VID_POLICY_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.VID_POLICY_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testCreateVidUinNotActive() throws IdRepoAppException {
		when(securityManager.hash(Mockito.any())).thenReturn("123");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("DEACTIVATED");
		identityResponse.setResponse(response);
		when(restHelper.requestSync(Mockito.any())).thenReturn(identityResponse);
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(2);
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(policy);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		when(vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(vid));
		when(vidRepo.save(Mockito.any())).thenReturn(vid);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		VidRequestDTO request = new VidRequestDTO();
		request.setUin("2953190571");
		try {
			service.generateVid(request);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_UIN.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_UIN.getErrorMessage(), "DEACTIVATED"),
					e.getErrorText());
		}
	}

	@Test
	public void testCreateVidUinNotFound() throws IdRepoAppException, JsonProcessingException {
		when(securityManager.hash(Mockito.any())).thenReturn("123");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		identityResponse.setErrors(
				Collections.singletonList(new ServiceError(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(),
						IdRepoErrorConstants.NO_RECORD_FOUND.getErrorMessage())));
		RestServiceException exception = new RestServiceException(IdRepoErrorConstants.NO_RECORD_FOUND,
				mapper.writeValueAsString(identityResponse), null);
		when(restHelper.requestSync(Mockito.any())).thenThrow(exception);
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(2);
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(policy);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		when(vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(vid));
		when(vidRepo.save(Mockito.any())).thenReturn(vid);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		VidRequestDTO vidRequest = new VidRequestDTO();
		vidRequest.setUin("2953190571");
		try {
			service.generateVid(vidRequest);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testCreateVidFailedUinRetrieval() throws IdRepoAppException, JsonProcessingException {
		when(securityManager.hash(Mockito.any())).thenReturn("123");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		identityResponse.setErrors(
				Collections.singletonList(new ServiceError(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(),
						IdRepoErrorConstants.NO_RECORD_FOUND.getErrorMessage())));
		RestServiceException exception = new RestServiceException();
		when(restHelper.requestSync(Mockito.any())).thenThrow(exception);
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(2);
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(policy);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		when(vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(vid));
		when(vidRepo.save(Mockito.any())).thenReturn(vid);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		VidRequestDTO vidRequest = new VidRequestDTO();
		vidRequest.setUin("2953190571");
		try {
			service.generateVid(vidRequest);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UIN_RETRIEVAL_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UIN_RETRIEVAL_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testCreateVidGenerationFailed() throws RestServiceException, IdRepoDataValidationException {
		when(securityManager.hash(Mockito.any())).thenReturn("123");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		when(restHelper.requestSync(Mockito.any())).thenReturn(identityResponse);
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(2);
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(policy);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		when(vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(vid));
		when(vidRepo.save(Mockito.any())).thenReturn(vid);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		VidRequestDTO request = new VidRequestDTO();
		request.setUin("2953190571");
		RestRequestDTO restReq = new RestRequestDTO();
		when(restBuilder.buildRequest(RestServicesConstants.VID_GENERATOR_SERVICE, null, ResponseWrapper.class)).thenReturn(restReq);
		when(restHelper.requestSync(restReq)).thenThrow(new RestServiceException(IdRepoErrorConstants.VID_GENERATION_FAILED));
		try {
			service.generateVid(request);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.VID_GENERATION_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.VID_GENERATION_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testCreateVidIdRepoAppUncheckedException() throws RestServiceException, IdRepoDataValidationException {
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		Mockito.when(securityManager.hashwithSalt(Mockito.any(), Mockito.any()))
				.thenThrow(new IdRepoAppUncheckedException(IdRepoErrorConstants.VID_GENERATION_FAILED));
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		when(restHelper.requestSync(Mockito.any())).thenReturn(identityResponse);
		try {
			Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn("3920450236".getBytes());
			VidRequestDTO request = new VidRequestDTO();
			request.setUin("2953190571");
			service.generateVid(request);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.VID_GENERATION_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.VID_GENERATION_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@SuppressWarnings("serial")
	@Test
	public void testCreateVidTransactionFailed() throws RestServiceException, IdRepoDataValidationException {
		Mockito.when(securityManager.hashwithSalt(Mockito.any(), Mockito.any()))
				.thenThrow(new TransactionException("") {
				});
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		when(restHelper.requestSync(Mockito.any())).thenReturn(identityResponse);
		try {
			Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn("3920450236".getBytes());
			VidRequestDTO request = new VidRequestDTO();
			request.setUin("2953190571");
			service.generateVid(request);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveVidIdRepoAppUncheckedException()
			throws RestServiceException, IdRepoDataValidationException {
		when(vidRepo.findByVid(Mockito.any()))
				.thenThrow(new IdRepoAppUncheckedException(IdRepoErrorConstants.VID_GENERATION_FAILED));
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		try {
			VidRequestDTO request = new VidRequestDTO();
			request.setUin("2953190571");
			service.updateVid("123", request);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.VID_GENERATION_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.VID_GENERATION_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@SuppressWarnings("serial")
	@Test
	public void testUpdateVidTransactionFailed() throws RestServiceException, IdRepoDataValidationException {
		when(vidRepo.findByVid(Mockito.any())).thenThrow(new TransactionException("") {
		});
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		try {
			VidRequestDTO request = new VidRequestDTO();
			request.setUin("2953190571");
			service.updateVid("123", request);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testUpdateVidIdRepoAppUncheckedException() throws RestServiceException, IdRepoDataValidationException {
		when(vidRepo.findByVid(Mockito.any()))
				.thenThrow(new IdRepoAppUncheckedException(IdRepoErrorConstants.VID_GENERATION_FAILED));
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		try {
			service.retrieveUinByVid("123");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.VID_GENERATION_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.VID_GENERATION_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@SuppressWarnings("serial")
	@Test
	public void testRetrieveVidTransactionFailed() throws RestServiceException, IdRepoDataValidationException {
		when(vidRepo.findByVid(Mockito.any())).thenThrow(new TransactionException("") {
		});
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		try {
			service.retrieveUinByVid("123");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testCreateVidRestDataValidationFailed() throws IdRepoAppException, JsonProcessingException {
		when(securityManager.hash(Mockito.any())).thenReturn("123");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class))).thenThrow(
				new IdRepoDataValidationException(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorCode(),
						String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(), "vid")));
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		identityResponse.setErrors(
				Collections.singletonList(new ServiceError(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(),
						IdRepoErrorConstants.NO_RECORD_FOUND.getErrorMessage())));
		RestServiceException exception = new RestServiceException();
		when(restHelper.requestSync(Mockito.any())).thenThrow(exception);
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(2);
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(policy);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		when(vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(vid));
		when(vidRepo.save(Mockito.any())).thenReturn(vid);
		VidRequestDTO request = new VidRequestDTO();
		request.setUin("2953190571");
		try {
			service.generateVid(request);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.MISSING_INPUT_PARAMETER.getErrorMessage(), "vid"),
					e.getErrorText());
		}
	}

	@Test
	public void testRetrieveUinByVid() throws IdRepoAppException {
		LocalDateTime currentTime = DateUtils.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone()))
				.toLocalDateTime().plusDays(1);
		Vid vid = new Vid("18b67aa3-a25a-5cec-94c2-90644bf5b05b", "2015642902372691", "461_null",
				"461_7C9JlRD32RnFTzAmeTfIzg", "perpetual", currentTime, currentTime, "ACTIVE", "IdRepo",
				currentTime, "IdRepo", currentTime, false, currentTime);
		when(securityManager.hash(Mockito.any())).thenReturn("123");
		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any(Class.class)))
				.thenReturn(new RestRequestDTO());
		IdResponseDTO identityResponse = new IdResponseDTO();
		ResponseDTO response = new ResponseDTO();
		response.setStatus("ACTIVATED");
		identityResponse.setResponse(response);
		when(restHelper.requestSync(Mockito.any())).thenReturn(identityResponse);
		Mockito.when(vidRepo.findByVid(Mockito.anyString())).thenReturn(vid);
		Mockito.when(vidRepo.retrieveUinByVid(Mockito.anyString())).thenReturn("1234567");
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		String uin = "3920450236";
		Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(uin.getBytes());
		ResponseWrapper<VidResponseDTO> retrieveUinByVid = service.retrieveUinByVid("12345678");
		assertEquals(uin, String.valueOf(retrieveUinByVid.getResponse().getUin()));
	}

	@Test
	public void testRetrieveUinByVidExpired() {
		LocalDateTime currentTime = DateUtils.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone()))
				.toLocalDateTime();
		Vid vid = new Vid("18b67aa3-a25a-5cec-94c2-90644bf5b05b", "2015642902372691", "461_null",
				"461_7C9JlRD32RnFTzAmeTfIzg", "perpetual", currentTime, currentTime, "ACTIVATED", "IdRepo",
				currentTime, "IdRepo", currentTime, false, currentTime);
		Mockito.when(vidRepo.findByVid(Mockito.anyString())).thenReturn(vid);
		Mockito.when(vidRepo.retrieveUinByVid(Mockito.anyString())).thenReturn("1234567");
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		try {
			Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn("3920450236".getBytes());
			service.retrieveUinByVid("12345678");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_VID.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_VID.getErrorMessage(), "EXPIRED"),
					e.getErrorText());
		}
	}

	@Test
	public void testRetrieveUinHashNotMatching() {
		LocalDateTime currentTime = DateUtils.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone()))
				.toLocalDateTime();
		Vid vid = new Vid("18b67aa3-a25a-5cec-94c2-90644bf5b05b", "2015642902372691",
				"461_7329815461_7C9JlRD32RnFTzAmeTfIzg", "461_7C9JlRD32RnFTzAmeTfIzg", "perpetual",
				currentTime, currentTime, "ACTIVATED", "IdRepo", currentTime, "IdRepo", currentTime, false,
				currentTime);
		Mockito.when(vidRepo.findByVid(Mockito.anyString())).thenReturn(vid);
		Mockito.when(vidRepo.retrieveUinByVid(Mockito.anyString())).thenReturn("1234567");
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		try {
			Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn("3920450236".getBytes());
			service.retrieveUinByVid("12345678");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UIN_HASH_MISMATCH.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.UIN_HASH_MISMATCH.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRetrieveUinByVidBlocked() {
		LocalDateTime currentTime = DateUtils.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone()))
				.toLocalDateTime().plusDays(1);
		Vid vid = new Vid("18b67aa3-a25a-5cec-94c2-90644bf5b05b", "2015642902372691", "461_null",
				"461_7C9JlRD32RnFTzAmeTfIzg", "perpetual", currentTime, currentTime, "Blocked", "IdRepo",
				currentTime, "IdRepo", currentTime, false, currentTime);
		Mockito.when(vidRepo.findByVid(Mockito.anyString())).thenReturn(vid);
		Mockito.when(vidRepo.retrieveUinByVid(Mockito.anyString())).thenReturn("1234567");
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		try {
			Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn("3920450236".getBytes());
			service.retrieveUinByVid("12345678");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_VID.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_VID.getErrorMessage(), "Blocked"),
					e.getErrorText());
		}
	}

	@Test
	public void testRetrieveUinByVidInvalidNoRecordsFound() {
		Mockito.when(vidRepo.findByVid(Mockito.anyString())).thenReturn(null);
		Mockito.when(vidRepo.retrieveUinByVid(Mockito.anyString())).thenReturn("1234567");
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		try {
			service.retrieveUinByVid("12345678");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testUpdateVidvalid() throws IdRepoAppException {
		LocalDateTime currentTime = DateUtils.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone()))
				.toLocalDateTime().plusDays(1);
		Vid vid = new Vid("18b67aa3-a25a-5cec-94c2-90644bf5b05b", "2015642902372691", "461_3920450236",
				"461_7C9JlRD32RnFTzAmeTfIzg", "perpetual", currentTime, currentTime, "ACTIVE", "IdRepo",
				currentTime, "IdRepo", currentTime, false, currentTime);
		Mockito.when(vidRepo.findByVid(Mockito.anyString())).thenReturn(vid);
		Mockito.when(vidRepo.retrieveUinByVid(Mockito.anyString())).thenReturn("1234567");
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("3920450236".getBytes());
		Mockito.when(securityManager.hashwithSalt(Mockito.any(), Mockito.any())).thenReturn("3920450236");
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(1);
		policy.setAllowedTransactions(null);
		policy.setAutoRestoreAllowed(true);
		policy.setRestoreOnAction("REVOKE");
		policy.setValidForInMinutes(null);
		Mockito.when(vidPolicyProvider.getPolicy(Mockito.anyString())).thenReturn(policy);
		VidRequestDTO request = new VidRequestDTO();
		String vidStatus = "EXPIRED";
		request.setVidStatus(vidStatus);
		ResponseWrapper<VidResponseDTO> updateVid = service.updateVid("12345678", request);
		assertEquals(vidStatus, updateVid.getResponse().getVidStatus());
	}

	@Test
	public void testUpdateVidvalidREVOKE() throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		LocalDateTime currentTime = DateUtils.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone()))
				.toLocalDateTime().plusDays(1);
		Vid vid = new Vid("18b67aa3-a25a-5cec-94c2-90644bf5b05b", "2015642902372691", "461_null",
				"461_7C9JlRD32RnFTzAmeTfIzg", "perpetual", currentTime, currentTime, "ACTIVE", "IdRepo",
				currentTime, "IdRepo", currentTime, false, currentTime);
		Mockito.when(vidRepo.findByVid(Mockito.anyString())).thenReturn(vid);
		Mockito.when(vidRepo.retrieveUinByVid(Mockito.anyString())).thenReturn("1234567");
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(1);
		policy.setAllowedTransactions(null);
		policy.setAutoRestoreAllowed(true);
		policy.setRestoreOnAction("REVOKE");
		policy.setValidForInMinutes(null);
		Mockito.when(vidPolicyProvider.getPolicy(Mockito.anyString())).thenReturn(policy);
		RestRequestDTO restRequestDTO = new RestRequestDTO();
		IdResponseDTO idResponse = new IdResponseDTO();
		ResponseDTO resDTO = new ResponseDTO();
		resDTO.setStatus("ACTIVATED");
		idResponse.setResponse(resDTO);
		Mockito.when(restBuilder.buildRequest(RestServicesConstants.IDREPO_IDENTITY_SERVICE, null, IdResponseDTO.class))
				.thenReturn(restRequestDTO);
		Mockito.when(restHelper.requestSync(restRequestDTO)).thenReturn(idResponse);
		Mockito.when(vidRepo.save(Mockito.any())).thenReturn(vid);
		Mockito.when(securityManager.hash(Mockito.any()))
				.thenReturn("6B764AE0FF065490AEFAF796A039D6B4F251101A5F13DA93146B9DEB11087AFC");
		Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("3920450236".getBytes());
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("3920450236".getBytes());
		VidRequestDTO request = new VidRequestDTO();
		request.setVidStatus("REVOKE");
		RestRequestDTO restReq = new RestRequestDTO();
		when(restBuilder.buildRequest(RestServicesConstants.VID_GENERATOR_SERVICE, null, ResponseWrapper.class)).thenReturn(restReq);
		ResponseWrapper<Object> response = new ResponseWrapper<>();
		ObjectNode node = mapper.createObjectNode();
		node.put("vid", "12345");
		response.setResponse(mapper.readValue(node.toString(), Object.class));
		when(restHelper.requestSync(restReq)).thenReturn(response);
		service.updateVid("12345678", request);
	}

	@Test
	public void testUpdateVidInvalid() {
		Mockito.when(vidRepo.findByVid(Mockito.anyString())).thenReturn(null);
		Mockito.when(vidRepo.retrieveUinByVid(Mockito.anyString())).thenReturn("1234567");
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		VidRequestDTO request = new VidRequestDTO();
		request.setVidStatus("ACTIVE");
		try {
			service.updateVid("12345678", request);
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRegenerate_Valid() throws IdRepoAppException, JsonParseException, JsonMappingException, IOException {
		LocalDateTime currentTime = DateUtils.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone()))
				.toLocalDateTime().plusDays(1);
		String vidValue = "2015642902372691";
		Vid vid = new Vid("18b67aa3-a25a-5cec-94c2-90644bf5b05b", vidValue, "461_null",
				"461_7C9JlRD32RnFTzAmeTfIzg", "perpetual", currentTime, currentTime, "ACTIVE", "IdRepo",
				currentTime, "IdRepo", currentTime, false, currentTime);
		Mockito.when(vidRepo.findByVid(Mockito.anyString())).thenReturn(vid);
		Mockito.when(vidRepo.retrieveUinByVid(Mockito.anyString())).thenReturn("1234567");
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(1);
		policy.setAllowedTransactions(null);
		policy.setAutoRestoreAllowed(false);
		policy.setRestoreOnAction("REVOKE");
		policy.setValidForInMinutes(null);
		Mockito.when(vidPolicyProvider.getPolicy(Mockito.anyString())).thenReturn(policy);
		RestRequestDTO restRequestDTO = new RestRequestDTO();
		IdResponseDTO idResponse = new IdResponseDTO();
		ResponseDTO resDTO = new ResponseDTO();
		resDTO.setStatus("ACTIVATED");
		idResponse.setResponse(resDTO);
		Mockito.when(restBuilder.buildRequest(RestServicesConstants.IDREPO_IDENTITY_SERVICE, null, IdResponseDTO.class))
				.thenReturn(restRequestDTO);
		Mockito.when(restHelper.requestSync(restRequestDTO)).thenReturn(idResponse);
		Mockito.when(vidRepo.save(Mockito.any())).thenReturn(vid);
		Mockito.when(securityManager.hash(Mockito.any()))
				.thenReturn("6B764AE0FF065490AEFAF796A039D6B4F251101A5F13DA93146B9DEB11087AFC");
		Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("3920450236".getBytes());
		RestRequestDTO restReq = new RestRequestDTO();
		when(restBuilder.buildRequest(RestServicesConstants.VID_GENERATOR_SERVICE, null, ResponseWrapper.class)).thenReturn(restReq);
		ResponseWrapper<Object> responseWrapper = new ResponseWrapper<>();
		ObjectNode node = mapper.createObjectNode();
		node.put("vid", "12345");
		responseWrapper.setResponse(mapper.readValue(node.toString(), Object.class));
		when(restHelper.requestSync(restReq)).thenReturn(responseWrapper);
		ResponseWrapper<VidResponseDTO> regenerateVid = service.regenerateVid("12345678");
		assertEquals(vidValue, String.valueOf(regenerateVid.getResponse().getVid()));
		assertEquals("INVALIDATED", regenerateVid.getResponse().getVidStatus());
	}

	@Test
	public void testRegenerateVid_EmptyRecordsInDb() {
		Mockito.when(vidRepo.findByVid(Mockito.anyString())).thenReturn(null);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		try {
			service.regenerateVid("12345678");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRegenerateVid_InValidStatus() throws IdRepoAppException {
		LocalDateTime currentTime = DateUtils.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone()))
				.toLocalDateTime().plusDays(1);
		Vid vid = new Vid("18b67aa3-a25a-5cec-94c2-90644bf5b05b", "2015642902372691",
				"461_7329815461_7C9JlRD32RnFTzAmeTfIzg", "461_7C9JlRD32RnFTzAmeTfIzg", "perpetual",
				currentTime, currentTime, "INACTIVE", "IdRepo", currentTime, "IdRepo", currentTime, false, currentTime);
		Mockito.when(vidRepo.findByVid(Mockito.anyString())).thenReturn(vid);
		Mockito.when(vidRepo.retrieveUinByVid(Mockito.anyString())).thenReturn("1234567");
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(1);
		policy.setAllowedTransactions(null);
		policy.setAutoRestoreAllowed(false);
		policy.setRestoreOnAction("REVOKE");
		policy.setValidForInMinutes(null);
		Mockito.when(vidPolicyProvider.getPolicy(Mockito.anyString())).thenReturn(policy);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		try {
			service.regenerateVid("12345678");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.INVALID_VID.getErrorCode(), e.getErrorCode());
			assertEquals(String.format(IdRepoErrorConstants.INVALID_VID.getErrorMessage(), "INACTIVE"),
					e.getErrorText());
		}
	}

	@SuppressWarnings("serial")
	@Test
	public void testRegenerateVidTransactionFailed() throws RestServiceException, IdRepoDataValidationException {
		when(vidRepo.findByVid(Mockito.any())).thenThrow(new TransactionException("") {
		});
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		try {
			service.regenerateVid("123");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRegenerate_IdRepoAppUncheckedException() throws Throwable {
		LocalDateTime currentTime = DateUtils.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone()))
				.toLocalDateTime().plusDays(1);
		Vid vid = new Vid("18b67aa3-a25a-5cec-94c2-90644bf5b05b", "2015642902372691", "461_null",
				"461_7329815461_7C9JlRD32RnFTzAmeTfIzg", "perpetual", currentTime, currentTime, "ACTIVE", "IdRepo",
				currentTime, "IdRepo", currentTime, false, currentTime);
		Mockito.when(vidRepo.findByVid(Mockito.anyString())).thenReturn(vid);
		Mockito.when(vidRepo.retrieveUinByVid(Mockito.anyString())).thenReturn("1234567");
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(1);
		policy.setAllowedTransactions(null);
		policy.setAutoRestoreAllowed(false);
		policy.setRestoreOnAction("REVOKED");
		policy.setValidForInMinutes(null);
		Mockito.when(vidPolicyProvider.getPolicy(Mockito.anyString())).thenReturn(policy);
		RestRequestDTO restRequestDTO = new RestRequestDTO();
		IdResponseDTO idResponse = new IdResponseDTO();
		ResponseDTO resDTO = new ResponseDTO();
		resDTO.setStatus("ACTIVATED");
		idResponse.setResponse(resDTO);
		Mockito.when(restBuilder.buildRequest(RestServicesConstants.IDREPO_IDENTITY_SERVICE, null, IdResponseDTO.class))
				.thenReturn(restRequestDTO);
		Mockito.when(restHelper.requestSync(restRequestDTO)).thenReturn(idResponse);
		Mockito.when(vidRepo.save(Mockito.any())).thenReturn(vid);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt()))
				.thenThrow(new IdRepoAppUncheckedException(IdRepoErrorConstants.VID_GENERATION_FAILED));
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		try {
			when(securityManager.hash(Mockito.any()))
			.thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
			Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn("3920450236".getBytes());
			service.regenerateVid("123");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.VID_GENERATION_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.VID_GENERATION_FAILED.getErrorMessage(), e.getErrorText());
		}
	}

	@Test
	public void testRegenerate_AutoRestoreNotAllowed() throws Throwable {
		LocalDateTime currentTime = DateUtils.getUTCCurrentDateTime()
				.atZone(ZoneId.of(EnvUtil.getDatetimeTimezone()))
				.toLocalDateTime().plusDays(1);
		Vid vid = new Vid("18b67aa3-a25a-5cec-94c2-90644bf5b05b", "2015642902372691",
				"461_7329815461_7C9JlRD32RnFTzAmeTfIzg", "461_7329815461_7C9JlRD32RnFTzAmeTfIzg", "perpetual",
				currentTime, currentTime, "ACTIVE", "IdRepo", currentTime, "IdRepo", currentTime, false, currentTime);
		Mockito.when(vidRepo.findByVid(Mockito.anyString())).thenReturn(vid);
		Mockito.when(vidRepo.retrieveUinByVid(Mockito.anyString())).thenReturn("1234567");
		VidPolicy policy = new VidPolicy();
		policy.setAllowedInstances(1);
		policy.setAllowedTransactions(null);
		policy.setAutoRestoreAllowed(true);
		policy.setRestoreOnAction("REVOKE");
		policy.setValidForInMinutes(null);
		Mockito.when(vidPolicyProvider.getPolicy(Mockito.anyString())).thenReturn(policy);
		RestRequestDTO restRequestDTO = new RestRequestDTO();
		IdResponseDTO idResponse = new IdResponseDTO();
		ResponseDTO resDTO = new ResponseDTO();
		resDTO.setStatus("ACTIVATED");
		idResponse.setResponse(resDTO);
		Mockito.when(restBuilder.buildRequest(RestServicesConstants.IDREPO_IDENTITY_SERVICE, null, IdResponseDTO.class))
				.thenReturn(restRequestDTO);
		Mockito.when(restHelper.requestSync(restRequestDTO)).thenReturn(idResponse);
		Mockito.when(vidRepo.save(Mockito.any())).thenReturn(vid);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("7C9JlRD32RnFTzAmeTfIzg");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("AG7JQI1HwFp_cI_DcdAQ9A");
		when(securityManager.hash(Mockito.any()))
				.thenReturn("6B764AE0FF065490AEFAF796A039D6B4F251101A5F13DA93146B9DEB11087AFC");
		try {
			Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any()))
					.thenReturn("3920450236".getBytes());
			service.regenerateVid("123");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.VID_POLICY_FAILED.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.VID_POLICY_FAILED.getErrorMessage(), e.getErrorText());
		}
	}
	
	@Test
	public void testDeactivateVID_valid() throws IdRepoAppException {
		RestRequestDTO restRequestDTO = new RestRequestDTO();
		when(securityManager.getSaltKeyForId(Mockito.any())).thenReturn(461);
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any()))
		.thenReturn("3920450236".getBytes());
		when(securityManager.hash(Mockito.any()))
				.thenReturn("6B764AE0FF065490AEFAF796A039D6B4F251101A5F13DA93146B9DEB11087AFC");
		Mockito.when(restBuilder.buildRequest(RestServicesConstants.IDREPO_IDENTITY_SERVICE, null, IdResponseDTO.class))
		.thenReturn(restRequestDTO);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		vid.setUinHash("6B764AE0FF065490AEFAF796A039D6B4F251101A5F13DA93146B9DEB11087AFC");
		vid.setUin("461_37C9JlRD32RnFTzAmeTfIzg");
		when(vidRepo.findByUinHashAndStatusCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(Collections.singletonList(vid));
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(new VidPolicy());
		ResponseWrapper<VidResponseDTO> regenerateVid = service.deactivateVIDsForUIN("12345461");
		assertEquals("DEACTIVATED", regenerateVid.getResponse().getVidStatus());
	}
	
	@Test
	public void testDeactivateVID_Invalid() throws Throwable {
		RestRequestDTO restRequestDTO = new RestRequestDTO();
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any()))
		.thenReturn("3920450236".getBytes());
		when(securityManager.hash(Mockito.any()))
				.thenReturn("6B764AE0FF065490AEFAF796A039D6B4F251101A5F13DA93146B9DEB11087AFC");
		Mockito.when(restBuilder.buildRequest(RestServicesConstants.IDREPO_IDENTITY_SERVICE, null, IdResponseDTO.class))
		.thenReturn(restRequestDTO);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		vid.setUin("461_7C9JlRD32RnFTzAmeTfIzg");
		when(vidRepo.findByUinHashAndStatusCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(Collections.emptyList());
		try {
			service.deactivateVIDsForUIN("12345461");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorCode(), e.getErrorCode());
			assertEquals(IdRepoErrorConstants.NO_RECORD_FOUND.getErrorMessage(), e.getErrorText());
		}
	}
	
	@Test
	public void testReactivateVID_valid() throws IdRepoAppException {
		RestRequestDTO restRequestDTO = new RestRequestDTO();
		when(uinEncryptSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		Mockito.when(securityManager.decryptWithSalt(Mockito.any(), Mockito.any(), Mockito.any()))
		.thenReturn("3920450236".getBytes());
		when(securityManager.getSaltKeyForId(Mockito.any())).thenReturn(461);
		when(securityManager.hashwithSalt(Mockito.any(), Mockito.any()))
				.thenReturn("6B764AE0FF065490AEFAF796A039D6B4F251101A5F13DA93146B9DEB11087AFC");
		Mockito.when(restBuilder.buildRequest(RestServicesConstants.IDREPO_IDENTITY_SERVICE, null, IdResponseDTO.class))
		.thenReturn(restRequestDTO);
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		vid.setUinHash("6B764AE0FF065490AEFAF796A039D6B4F251101A5F13DA93146B9DEB11087AFC");
		vid.setUin("461_7C9JlRD32RnFTzAmeTfIzg");
		when(vidRepo.findByUinHashAndStatusCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(Collections.singletonList(vid));
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(new VidPolicy());
		ResponseWrapper<VidResponseDTO> regenerateVid = service.reactivateVIDsForUIN("12345461");
		assertEquals("ACTIVE", regenerateVid.getResponse().getVidStatus());
	}
	
	@Test
	public void testRetrieveVidsByUin() throws IdRepoAppException {
		Vid vid = new Vid();
		vid.setVid("123");
		vid.setStatusCode("");
		vid.setUinHash("6B764AE0FF065490AEFAF796A039D6B4F251101A5F13DA93146B9DEB11087AFC");
		vid.setUin("461_7C9JlRD32RnFTzAmeTfIzg");
		vid.setVidTypeCode("Perpetual");
		when(vidPolicyProvider.getPolicy(Mockito.any())).thenReturn(new VidPolicy());
		when(securityManager.hashwithSalt(Mockito.any(), Mockito.any()))
				.thenReturn("6B764AE0FF065490AEFAF796A039D6B4F251101A5F13DA93146B9DEB11087AFC");
		when(uinHashSaltRepo.retrieveSaltById(Mockito.anyInt())).thenReturn("YWJjZA==");
		when(securityManager.getSaltKeyForId(Mockito.any())).thenReturn(461);
		when(vidRepo.findByUinHashAndStatusCodeAndExpiryDTimesAfter(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(List.of(vid));
		VidsInfosDTO response = service.retrieveVidsByUin("");
		assertEquals("Perpetual", response.getResponse().get(0).getVidType());
	}
	
	@Test
	public void testRetrieveVidsByUinException() throws IdRepoAppException {
		when(securityManager.getSaltKeyForId(Mockito.any())).thenThrow(new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR));
		try {
			service.retrieveVidsByUin("");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage(), e.getErrorText());
		}
	}
	
	@Test
	public void testRetrieveVidsByUinDataAccessException() throws IdRepoAppException {
		when(securityManager.getSaltKeyForId(Mockito.any())).thenThrow(new DataAccessException(IdRepoErrorConstants.UNKNOWN_ERROR.getErrorMessage()) {
		});
		try {
			service.retrieveVidsByUin("");
		} catch (IdRepoAppException e) {
			assertEquals(IdRepoErrorConstants.DATABASE_ACCESS_ERROR.getErrorMessage(), e.getErrorText());
		}
	}
	
	@Test
	public void testCredentialRequestResponseConsumer() {
		CredentialIssueRequestWrapperDto req = new CredentialIssueRequestWrapperDto();
		Map<String, Object> res = Map.of();
		ArgumentCaptor<EventModel> argCapture = ArgumentCaptor.forClass(EventModel.class);
		service.credentialRequestResponseConsumer(req, res);
		verify(websubHelper).publishEvent(argCapture.capture());
		assertEquals(req, argCapture.getValue().getEvent().getData().get("request"));
		assertEquals(res, argCapture.getValue().getEvent().getData().get("response"));
	}
	
	@Test
	public void testIdaEventConsumer() {
		ArgumentCaptor<EventModel> argCapture = ArgumentCaptor.forClass(EventModel.class);
		EventModel event = new EventModel();
		service.idaEventConsumer(event);
		verify(websubHelper).publishEvent(argCapture.capture());
		assertEquals(event, argCapture.getValue().getEvent().getData().get("idaEvent"));
	}
}
