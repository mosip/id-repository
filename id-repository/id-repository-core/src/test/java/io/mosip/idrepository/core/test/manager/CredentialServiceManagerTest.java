package io.mosip.idrepository.core.test.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.dto.*;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.entity.Handle;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.repository.HandleRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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

	@Mock
	HandleRepo handleRepo;

	@Mock
	UinHashSaltRepo uinHashSaltRepo;

	@Mock
	UinEncryptSaltRepo uinEncryptSaltRepo;

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
	public void notifyUinCredential_shouldLogError_whenExceptionOccurs() throws IdRepoDataValidationException, RestServiceException {

		when(restBuilder.buildRequest(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenThrow(new RuntimeException("Runtime Exception"));
		VidsInfosDTO vidsInfosDTO = new VidsInfosDTO();
		vidsInfosDTO.setResponse(List.of());
		when(restHelper.requestSync(any())).thenReturn(vidsInfosDTO);
		EventModel eventModel = new EventModel();
		when(websubHelper.createEventModel(any(), any(), any(), any(), any(), any()))
				.thenReturn(new AsyncResult<>(eventModel));
		LocalDateTime expiryTimestamp = LocalDateTime.now();
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		List<String> partnerIds = new ArrayList<String>();
		partnerIds.add("12");
		credentialServiceManager.notifyUinCredential("123", expiryTimestamp, null, false, "12",
				saltRetreivalFunction, null, null, partnerIds, "123465");
		verify(restBuilder, never()).buildRequest(any(), any(), any());
	}

	@Test
	public void notifyUinCredential_WhenPartnerIdIsNull() throws IdRepoDataValidationException, RestServiceException {
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restReq);
		VidsInfosDTO vidsInfosDTO = new VidsInfosDTO();
		vidsInfosDTO.setResponse(List.of());
		when(restHelper.requestSync(any())).thenReturn(vidsInfosDTO);
		EventModel eventModel = new EventModel();
		when(websubHelper.createEventModel(any(), any(), any(), any(), any(), any()))
				.thenReturn(new AsyncResult<>(eventModel));
		LocalDateTime expiryTimestamp = LocalDateTime.now();
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		credentialServiceManager.notifyUinCredential("123", expiryTimestamp, null, false, "12",
				saltRetreivalFunction, null, null, new ArrayList<>(), "123465");
		verify(partnerServiceManager, times(1)).getOLVPartnerIds();
	}

	@Test
	public void notifyUinCredential_WhenUINBasedRequestIsEnabled() throws IdRepoAppException {

		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restReq);

		VidsInfosDTO vidsInfosDTO = getVidsInfosDTO();
		when(restHelper.requestSync(any())).thenReturn(vidsInfosDTO);

		EventModel eventModel = new EventModel();
		when(websubHelper.createEventModel(any(), any(), any(), any(), any(), any()))
				.thenReturn(new AsyncResult<>(eventModel));
		LocalDateTime expiryTimestamp = LocalDateTime.now();
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		List<String> partnerIds = new ArrayList<String>();
		partnerIds.add("12");
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hash");
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(123);
		when(securityManager.hashwithSalt(any(), any())).thenReturn("uinHash");

		List<Handle> list = new ArrayList<>();
		Handle handle = new Handle();
		handle.setId("1");
		handle.setHandle("6666");
		list.add(handle);
		when(handleRepo.findByUinHash(anyString())).thenReturn(list);

		when(securityManager.decryptWithSalt(any(), any(), any())).thenReturn(new byte[5]);

		when(restHelper.requestSync(any())).thenReturn(vidsInfosDTO);
		credentialServiceManager.notifyUinCredential("123", expiryTimestamp, null, true, "12",
				saltRetreivalFunction, null, null, partnerIds, "123465");
		verify(handleRepo, times(1)).findByUinHash(anyString());
	}

	VidsInfosDTO getVidsInfosDTO() {
		VidInfoDTO vidInfoDTO = new VidInfoDTO();
		vidInfoDTO.setVid("456");
		vidInfoDTO.setTransactionLimit(10000);
		Map<String, String> hashAttributes = new HashMap<>();
		hashAttributes.put("key", "value");
		vidInfoDTO.setHashAttributes(hashAttributes);
		VidsInfosDTO vidsInfosDTO = new VidsInfosDTO();
		vidsInfosDTO.setResponse(List.of(vidInfoDTO));
		return vidsInfosDTO;
	}

	@Test
	public void shouldHandleDecryptionFailure_WhenNotifyingUinCredential() throws IdRepoAppException {

		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restReq);

		VidsInfosDTO vidsInfosDTO = getVidsInfosDTO();
		when(restHelper.requestSync(any())).thenReturn(vidsInfosDTO);

		EventModel eventModel = new EventModel();
		when(websubHelper.createEventModel(any(), any(), any(), any(), any(), any()))
				.thenReturn(new AsyncResult<>(eventModel));
		LocalDateTime expiryTimestamp = LocalDateTime.now();
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		List<String> partnerIds = new ArrayList<String>();
		partnerIds.add("12");
		when(uinHashSaltRepo.retrieveSaltById(anyInt())).thenReturn("hash");
		when(securityManager.getSaltKeyForId(anyString())).thenReturn(123);
		when(securityManager.hashwithSalt(any(), any())).thenReturn("uinHash");

		List<Handle> list = new ArrayList<>();
		Handle handle = new Handle();
		handle.setId("1");
		handle.setHandle("6666");
		list.add(handle);
		when(handleRepo.findByUinHash(anyString())).thenReturn(list);

		when(securityManager.decryptWithSalt(any(), any(), any()))
				.thenThrow(new IdRepoAppException(IdRepoErrorConstants.ENCRYPTION_DECRYPTION_FAILED));

		when(restHelper.requestSync(any())).thenReturn(vidsInfosDTO);
		credentialServiceManager.notifyUinCredential("123", expiryTimestamp, null, true, "12",
				saltRetreivalFunction, null, null, partnerIds, "123465");
		verify(securityManager, times(1)).decryptWithSalt(any(), any(), any());
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
	public void notifyVIDCredential_WhenPartnerIdsAreRetrieved() throws IdRepoDataValidationException, RestServiceException {
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
		List<VidInfoDTO> vidInfoDtos = new ArrayList<>();
		VidInfoDTO vidInfoDTO = new VidInfoDTO();
		vidInfoDTO.setExpiryTimestamp(LocalDateTime.now());
		vidInfoDtos.add(vidInfoDTO);
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		credentialServiceManager.notifyVIDCredential("123", "ACTIVATED", vidInfoDtos, false,
				saltRetreivalFunction, null, null);
		verify(partnerServiceManager, times(1)).getOLVPartnerIds();

	}

	@Test
	public void notifyVIDCredential_WhenStatusIsActivated() throws IdRepoDataValidationException, RestServiceException {
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
		List<VidInfoDTO> vidInfoDtos = new ArrayList<>();
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		ReflectionTestUtils.setField(credentialServiceManager, "vidActiveStatus", "ACTIVATED");
		credentialServiceManager.notifyVIDCredential("123", "ACTIVATED", vidInfoDtos, true,
				saltRetreivalFunction, null, null);
		verify(partnerServiceManager, times(1)).getOLVPartnerIds();

	}

	@Test
	public void notifyVIDCredential_WhenStatusIsRevoked() throws IdRepoDataValidationException, RestServiceException {
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
		List<VidInfoDTO> vidInfoDtos = new ArrayList<>();
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		ReflectionTestUtils.setField(credentialServiceManager, "vidActiveStatus", "");
		credentialServiceManager.notifyVIDCredential("123", "REVOKED", vidInfoDtos, true,
				saltRetreivalFunction, null, null);
		verify(partnerServiceManager, times(1)).getOLVPartnerIds();

	}

	@Test
	public void notifyVIDCredential_WhenStatusIsDeactivate() throws IdRepoDataValidationException, RestServiceException {
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restReq);

		VidInfoDTO vidInfoDTO = new VidInfoDTO();
		vidInfoDTO.setVid("456");
		vidInfoDTO.setTransactionLimit(10000);
		Map<String, String> hashAttributes = new HashMap<>();
		hashAttributes.put("key", "value");
		vidInfoDTO.setHashAttributes(hashAttributes);
		VidsInfosDTO vidsInfosDTO = new VidsInfosDTO();
		vidsInfosDTO.setResponse(List.of(vidInfoDTO));

		List<VidInfoDTO> vidInfoDtos = new ArrayList<>();
		vidInfoDtos.add(vidInfoDTO);

		when(restHelper.requestSync(any())).thenReturn(vidsInfosDTO);
		EventModel eventModel = new EventModel();
		when(websubHelper.createEventModel(any(), any(), any(), any(), any(), any()))
				.thenReturn(new AsyncResult<>(eventModel));
		String txnId = "12";
		List<String> partnerIds = new ArrayList<>();
		partnerIds.add(txnId);
		when(partnerServiceManager.getOLVPartnerIds()).thenReturn(partnerIds);
		IntFunction<String> saltRetreivalFunction = a -> "Test";
		ReflectionTestUtils.setField(credentialServiceManager, "vidActiveStatus", "");
		credentialServiceManager.notifyVIDCredential("123", "Deactivate", vidInfoDtos, true,
				saltRetreivalFunction, null, null);
		verify(partnerServiceManager, times(1)).getOLVPartnerIds();

	}

	@Test
	public void sendUinEventsToCredServiceTest_WithoutRequestId() throws IdRepoDataValidationException, RestServiceException {
		RestRequestDTO restReq = new RestRequestDTO();
		restReq.setUri("{uin}");
		when(restBuilder.buildRequest(any(), any(), any())).thenReturn(restReq);
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
	public void sendVidEventsToCredService_RestServiceException() throws IdRepoDataValidationException, RestServiceException {
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
		VidInfoDTO vidInfoDTO = new VidInfoDTO();
		vidInfoDTO.setExpiryTimestamp(LocalDateTime.now());
		vidInfoDtos.add(vidInfoDTO);
		List<String> partnerIds = new ArrayList<String>();
		partnerIds.add(txnId);

		when(restHelper.requestSync(any())).thenThrow(new RestServiceException(IdRepoErrorConstants.AUTHENTICATION_FAILED));

		credentialServiceManager.sendVidEventsToCredService(uin, status, vidInfoDtos,isUpdate, partnerIds,
				saltRetreivalFunction, credentialRequestResponseConsumer);
		verify(tokenIDGenerator, times(1)).generateTokenID(anyString(), anyString());
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
	public void updateEventProcessingStatus(){
		credentialServiceManager.updateEventProcessingStatus("request", "active", "event");
		verify(tokenIDGenerator, never()).generateTokenID(anyString(), anyString());
	}

	@Test
	public void triggerEventNotifications(){
		IntFunction<String> saltRetreivalFunction = value -> "salt";
		credentialServiceManager.triggerEventNotifications("342221", LocalDateTime.now(), "Activated", true,
				"76643", saltRetreivalFunction, "request");
		verify(tokenIDGenerator, never()).generateTokenID(anyString(), anyString());
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
