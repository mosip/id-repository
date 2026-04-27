package io.mosip.idrepository.core.test.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.dto.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.dto.VidsInfosDTO;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.manager.partner.PartnerServiceManager;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.kernel.core.websub.model.EventModel;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest
@Import(EnvUtil.class)
@ActiveProfiles("test")
public class CredentialServiceManagerTest {

	@InjectMocks
	private CredentialServiceManager credentialServiceManager;

	@Mock
	private ObjectMapper mapper;

	@Mock
	private RestRequestBuilder restBuilder;

	/** The security manager. */
	@Mock
	private IdRepoSecurityManager securityManager;

	@Mock
	private TokenIDGenerator tokenIDGenerator;

	@Mock
	private IdRepoWebSubHelper websubHelper;

	@Mock
	private DummyPartnerCheckUtil dummyCheck;

	@Mock
	private ApplicationContext ctx;

	@Mock
	private PartnerServiceManager partnerServiceManager;

	@Mock
	private RestHelper restHelper;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		ReflectionTestUtils.setField(credentialServiceManager, "restHelper", restHelper);
	}

	@Test
	public void notifyUinCredentialForStatusUpdateTest() throws IdRepoDataValidationException, RestServiceException {
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restReq);
		VidsInfosDTO vidsInfosDTO = new VidsInfosDTO();
		vidsInfosDTO.setResponse(List.of());
		when(restHelper.requestSync(any())).thenReturn(vidsInfosDTO);
		EventModel eventModel = new EventModel();
		when(websubHelper.createEventModel(any(), any(), any(), any(), any(), any()))
				.thenReturn(new AsyncResult<>(eventModel));
		String uin = "123";
		LocalDateTime expiryTimestamp = LocalDateTime.now();
		String status = "ACTIVATED";
		boolean isUpdate = true;
		String txnId = "12";
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer = null;
		Consumer<EventModel> idaEventModelConsumer = null;
		List<String> partnerIds = new ArrayList<String>();
		partnerIds.add(txnId);
		String requestId = "123465";
		credentialServiceManager.notifyUinCredential(uin, expiryTimestamp, status, isUpdate, txnId,
				saltRetreivalFunction, credentialRequestResponseConsumer, idaEventModelConsumer, partnerIds, requestId);
	}

	@Test
	public void notifyVIDCredentialTest() throws IdRepoDataValidationException, RestServiceException {
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restReq);
		VidsInfosDTO vidsInfosDTO = new VidsInfosDTO();
		vidsInfosDTO.setResponse(List.of());
		when(restHelper.requestSync(any())).thenReturn(vidsInfosDTO);
		EventModel eventModel = new EventModel();
		when(websubHelper.createEventModel(any(), any(), any(), any(), any(), any()))
				.thenReturn(new AsyncResult<>(eventModel));
		String txnId = "12";
		List<String> partnerIds = new ArrayList<>();
		partnerIds.add(txnId);
		when(partnerServiceManager.getOLVPartnerIds()).thenReturn(partnerIds);
		String uin = "123";
		List<VidInfoDTO> vidInfoDtos = new ArrayList<>();
		String status = "ACTIVATED";
		boolean isUpdate = true;
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer = null;
		Consumer<EventModel> idaEventModelConsumer = null;
		credentialServiceManager.notifyVIDCredential(uin, status, vidInfoDtos, isUpdate,
				saltRetreivalFunction, credentialRequestResponseConsumer, idaEventModelConsumer);

	}

	@Test
	public void sendUinEventsToCredServiceTest_WithoutRequestId() throws IdRepoDataValidationException, RestServiceException {
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restReq);
		when(restHelper.requestSync(any())).thenReturn(Map.of());
		EventModel eventModel = new EventModel();
		when(websubHelper.createEventModel(any(), any(), any(), any(), any(), any()))
				.thenReturn(new AsyncResult<>(eventModel));
		String uin = "123";
		LocalDateTime expiryTimestamp = LocalDateTime.now();
		boolean isUpdate = true;
		String txnId = "12";
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer = null;
		List<VidInfoDTO> vidInfoDtos = new ArrayList<>();
		List<HandleInfoDTO> handleInfoDtos = new ArrayList<>();
		List<String> partnerIds = new ArrayList<String>();
		partnerIds.add(txnId);
		credentialServiceManager.sendUinEventsToCredService(uin, expiryTimestamp, isUpdate, vidInfoDtos,handleInfoDtos,partnerIds,
				saltRetreivalFunction, credentialRequestResponseConsumer);

	}

	@Test
	public void sendUinEventsToCredServiceTest() throws IdRepoDataValidationException, RestServiceException {
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restReq);
		when(restHelper.requestSync(any())).thenReturn(Map.of());
		EventModel eventModel = new EventModel();
		when(websubHelper.createEventModel(any(), any(), any(), any(), any(), any()))
				.thenReturn(new AsyncResult<>(eventModel));
		String uin = "123";
		LocalDateTime expiryTimestamp = LocalDateTime.now();
		boolean isUpdate = true;
		String txnId = "12";
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer = null;
		List<VidInfoDTO> vidInfoDtos = new ArrayList<>();
		List<HandleInfoDTO> handleInfoDtos = new ArrayList<>();
		List<String> partnerIds = new ArrayList<String>();
		partnerIds.add(txnId);
		String requestId = "123465";
		credentialServiceManager.sendUinEventsToCredService(uin, expiryTimestamp, isUpdate, vidInfoDtos,handleInfoDtos,partnerIds,
				saltRetreivalFunction, credentialRequestResponseConsumer, requestId);

	}

	@Test
	public void sendVidEventsToCredServiceTest() throws IdRepoDataValidationException, RestServiceException {
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restReq);
		VidsInfosDTO vidsInfosDTO = new VidsInfosDTO();
		vidsInfosDTO.setResponse(List.of());
		when(restHelper.requestSync(any())).thenReturn(vidsInfosDTO);
		EventModel eventModel = new EventModel();
		when(websubHelper.createEventModel(any(), any(), any(), any(), any(), any()))
				.thenReturn(new AsyncResult<>(eventModel));
		String uin = "123";
		String status = "ACTIVATED";
		boolean isUpdate = true;
		String txnId = "12";
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer = null;
		List<VidInfoDTO> vidInfoDtos = new ArrayList<>();
		List<String> partnerIds = new ArrayList<String>();
		partnerIds.add(txnId);
		credentialServiceManager.sendVidEventsToCredService(uin, status, vidInfoDtos,isUpdate, partnerIds,
				saltRetreivalFunction, credentialRequestResponseConsumer);
	}

	@Test
	public void sendVidEventsToCredServiceTest_WithoutUin() throws IdRepoDataValidationException, RestServiceException {
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restReq);
		VidsInfosDTO vidsInfosDTO = new VidsInfosDTO();
		vidsInfosDTO.setResponse(List.of());
		when(restHelper.requestSync(any())).thenReturn(vidsInfosDTO);
		EventModel eventModel = new EventModel();
		when(websubHelper.createEventModel(any(), any(), any(), any(), any(), any()))
				.thenReturn(new AsyncResult<>(eventModel));
		boolean isUpdate = true;
		List<CredentialIssueRequestDto> eventRequestList= new ArrayList<>();
		BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer = null;;
		credentialServiceManager.sendRequestToCredService(eventRequestList,isUpdate,credentialRequestResponseConsumer);
	}

	@Test
	public void createCredReqDtoTest_WithoutRequestId() {
		String id = "123";
		String partnerId = "456";
		LocalDateTime expiryTimestamp = LocalDateTime.now();
		Integer transactionLimit = 100;
		String token = "abc";
		Map<String, Object> idHashAttributes = new HashMap<>();
		idHashAttributes.put("attribute1", "value1");
		idHashAttributes.put("attribute2", "value2");
		String requestId = null;
		CredentialIssueRequestDto result = credentialServiceManager.createCredReqDto(id, partnerId, expiryTimestamp,
				transactionLimit, token, idHashAttributes);

		assertEquals(id, result.getId());
		assertEquals(requestId, result.getRequestId());
		assertEquals(partnerId, result.getIssuer());
		assertEquals(transactionLimit, result.getAdditionalData().get(IdRepoConstants.TRANSACTION_LIMIT));
		assertEquals(token, result.getAdditionalData().get(IdRepoConstants.TOKEN));
	}

	@Test
	public void createCredReqDto_WithRequestId() {
		String id = "123";
		String partnerId = "456";
		LocalDateTime expiryTimestamp = LocalDateTime.now();
		Integer transactionLimit = 100;
		String token = "abc";
		Map<String, Object> idHashAttributes = new HashMap<>();
		idHashAttributes.put("attribute1", "value1");
		idHashAttributes.put("attribute2", "value2");
		String requestId = "789";
		CredentialIssueRequestDto result = credentialServiceManager.createCredReqDto(id, partnerId, expiryTimestamp,
				transactionLimit, token, idHashAttributes, requestId);

		assertEquals(id, result.getId());
		assertEquals(requestId, result.getRequestId());
		assertEquals(partnerId, result.getIssuer());
		assertEquals(transactionLimit, result.getAdditionalData().get(IdRepoConstants.TRANSACTION_LIMIT));
		assertEquals(token, result.getAdditionalData().get(IdRepoConstants.TOKEN));
	}

}
