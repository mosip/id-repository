package io.mosip.idrepository.core.manager;

import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_STATUS_UPDATE_TOPIC;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UIN_REFID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.time.LocalDateTime;
import java.util.*;

import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.constant.CredentialRequestStatusLifecycle;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.repository.CredentialRequestStatusRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.model.Event;
import io.mosip.kernel.core.websub.model.EventModel;

@ContextConfiguration(classes = { TestContext.class, WebApplicationContext.class })
@RunWith(SpringRunner.class)
@WebMvcTest
@Import(EnvUtil.class)
public class CredentialStatusManagerTest {

	@InjectMocks
	private CredentialStatusManager credentialStatusManager = new CredentialStatusManager();

	private static final String TRANSACTION_LIMIT = "transaction_limit";

	private static final String ID_HASH = "id_hash";

	Logger mosipLogger = IdRepoLogger.getLogger(CredentialStatusManager.class);

	@Mock
	private CredentialRequestStatusRepo statusRepo;

	@Mock
	private CredentialServiceManager credManager;

	@Mock
	private ObjectMapper mapper;

	@Mock
	private UinHashSaltRepo uinHashSaltRepo;

	@Mock
	private UinEncryptSaltRepo uinEncryptSaltRepo;

	@Mock
	private IdRepoSecurityManager securityManager;

	@Value("${" + UIN_REFID + "}")
	private String uinRefId;

	@Value("${" + CREDENTIAL_STATUS_UPDATE_TOPIC + "}")
	private String credentailStatusUpdateTopic;

	@Mock
	private DummyPartnerCheckUtil dummyPartner;

	private static String uinActiveStatus;

	@Mock
	private EnvUtil env;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		ReflectionTestUtils.setField(env, "uinActiveStatus", "true");
		ReflectionTestUtils.setField(credentialStatusManager, "uinRefId", "123");
	}

	/**
	 * This class tests the triggerEventNotifications method
	 * @throws IdRepoAppException
	 */
	@Test
	public void triggerEventNotificationsTest() throws IdRepoAppException {
		List<CredentialRequestStatus> deletedIssueRequestList = new ArrayList<CredentialRequestStatus>();
		CredentialRequestStatus req = new CredentialRequestStatus();
		req.setIndividualId("12");
		req.setPartnerId("33");
		req.setStatus("Success");
		req.setTokenId("132");
		req.setRequestId("22");
		deletedIssueRequestList.add(req);
		Mockito.when(statusRepo.findByStatus(CredentialRequestStatusLifecycle.DELETED.toString()))
				.thenReturn(deletedIssueRequestList);
		credentialStatusManager.triggerEventNotifications();

		CredentialStatusManager credentialStatusManagerSpy = Mockito.spy(credentialStatusManager);
		Mockito.doReturn("1122").when(credentialStatusManagerSpy).decryptId(any());
		ReflectionTestUtils.invokeMethod(credentialStatusManagerSpy, "handleDeletedRequests");

		Mockito.doReturn("1122").when(credentialStatusManagerSpy).decryptId(any());
		credentialStatusManagerSpy.handleNewOrUpdatedRequests();

		List<CredentialRequestStatus> expiredIssueRequestList = new ArrayList<>();
		CredentialRequestStatus credentialRequestStatus = new CredentialRequestStatus();
		expiredIssueRequestList.add(credentialRequestStatus);
		Mockito.when(statusRepo.findByIdExpiryTimestampBefore(any())).thenReturn(expiredIssueRequestList);
		credentialStatusManager.triggerEventNotifications();

		Mockito.doThrow(Exception.class).when(credentialStatusManagerSpy);
		ReflectionTestUtils.invokeMethod(credentialStatusManagerSpy, "handleExpiredRequests");
	}

	@Test
	public void handleNewOrUpdatedRequests_NotifyCredentialManagerForNewRequests() throws IdRepoAppException {
		List<CredentialRequestStatus> deletedIssueRequestList = new ArrayList<CredentialRequestStatus>();
		CredentialRequestStatus req = new CredentialRequestStatus();
		req.setIndividualId("12");
		req.setPartnerId("33");
		req.setStatus("Success");
		req.setTokenId("132");
		req.setRequestId("22");
		deletedIssueRequestList.add(req);
		Mockito.when(statusRepo.findByStatus(any(), anyInt()))
				.thenReturn(deletedIssueRequestList);
		credentialStatusManager.triggerEventNotifications();

		CredentialStatusManager credentialStatusManagerSpy = Mockito.spy(credentialStatusManager);
		Mockito.doReturn("1122").when(credentialStatusManagerSpy).decryptId(any());
		ReflectionTestUtils.invokeMethod(credentialStatusManagerSpy, "handleDeletedRequests");

		Mockito.doReturn("1122").when(credentialStatusManagerSpy).decryptId(any());
		credentialStatusManagerSpy.handleNewOrUpdatedRequests();

		List<CredentialRequestStatus> expiredIssueRequestList = new ArrayList<>();
		CredentialRequestStatus credentialRequestStatus = new CredentialRequestStatus();
		expiredIssueRequestList.add(credentialRequestStatus);
		Mockito.when(statusRepo.findByIdExpiryTimestampBefore(any())).thenReturn(expiredIssueRequestList);
		credentialStatusManager.triggerEventNotifications();

		Mockito.doThrow(Exception.class).when(credentialStatusManagerSpy);
		ReflectionTestUtils.invokeMethod(credentialStatusManagerSpy, "handleExpiredRequests");
		Mockito.verify(statusRepo, Mockito.times(3)).findByStatus(any(), anyInt());
	}

	/**
	 * This class tests the handleNewOrUpdatedRequests method
	 * @throws IdRepoAppException
	 */
	@Test
	public void handleNewOrUpdatedRequestsTest() throws IdRepoAppException {
		List<CredentialRequestStatus> newIssueRequestList = new ArrayList<CredentialRequestStatus>();
		CredentialRequestStatus req = new CredentialRequestStatus();
		req.setIndividualId("12");
		req.setPartnerId("33");
		req.setStatus("Success");
		req.setTokenId("132");
		req.setRequestId("22");
		newIssueRequestList.add(req);

		Mockito.when(statusRepo.findByStatus(CredentialRequestStatusLifecycle.NEW.toString()))
				.thenReturn(newIssueRequestList);
		credentialStatusManager.handleNewOrUpdatedRequests();

		CredentialStatusManager credentialStatusManagerSpy = Mockito.spy(credentialStatusManager);
		Mockito.doReturn("1122").when(credentialStatusManagerSpy).decryptId(any());
		credentialStatusManagerSpy.handleNewOrUpdatedRequests();
	}

	/**
	 * This class tests the deleteDummyPartner method
	 * @throws IdRepoAppException
	 */
	@Test
	public void deleteDummyPartnerTest() {
		CredentialRequestStatus credentialRequestStatus = new CredentialRequestStatus();
		credentialRequestStatus.setPartnerId("123");
		credentialStatusManager.deleteDummyPartner(credentialRequestStatus);

		credentialRequestStatus.setStatus(CredentialRequestStatusLifecycle.NEW.toString());
		Optional<CredentialRequestStatus> idWithDummyPartnerOptional = Optional.of(credentialRequestStatus);
		Mockito.when(statusRepo.findByIndividualIdHashAndPartnerId(any(), any())).thenReturn(idWithDummyPartnerOptional);
		credentialStatusManager.deleteDummyPartner(credentialRequestStatus);

		credentialRequestStatus.setStatus(CredentialRequestStatusLifecycle.FAILED.toString());
        idWithDummyPartnerOptional = Optional.of(credentialRequestStatus);
		Mockito.when(statusRepo.findByIndividualIdHashAndPartnerId(any(), any())).thenReturn(idWithDummyPartnerOptional);
		credentialStatusManager.deleteDummyPartner(credentialRequestStatus);
	}

	/**
	 * This class tests the credentialRequestResponseConsumer method
	 * @throws IdRepoAppException
	 */
	@Test
	public void credentialRequestResponseConsumerTest() throws IdRepoAppException {
		CredentialIssueRequestWrapperDto request = new CredentialIssueRequestWrapperDto();
		CredentialIssueRequestDto req = new CredentialIssueRequestDto();
		Map<String, Object> additionalData = new HashMap<String, Object>();
		additionalData.put("id_hash", "value1");
		req.setAdditionalData(additionalData);
		req.setIssuer("Test");
		req.setId("1");
		request.setId("1");
		request.setVersion("2.0");
		request.setRequesttime(LocalDateTime.now());
		request.setRequest(req);
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("response", "value1");
		credentialStatusManager.credentialRequestResponseConsumer(request, response);

		CredentialStatusManager credentialStatusManagerSpy = Mockito.spy(credentialStatusManager);
		Mockito.doReturn("value1").when(credentialStatusManagerSpy).encryptId(any());
		credentialStatusManagerSpy.credentialRequestResponseConsumer(request, response);

		CredentialIssueResponse credResponse = new CredentialIssueResponse();
		Mockito.when(mapper.convertValue((Object) any(), (Class<Object>) any())).thenReturn(credResponse);
		credentialStatusManagerSpy.credentialRequestResponseConsumer(request, response);

		additionalData.put("transaction_limit", 3);
		additionalData.put("expiry_timestamp", LocalDateTime.now());
		req.setAdditionalData(additionalData);
		request.setRequest(req);
		credentialStatusManagerSpy.credentialRequestResponseConsumer(request, response);

		CredentialRequestStatus credentialRequestStatus = new CredentialRequestStatus();
		Optional<CredentialRequestStatus> credStatusOptional = Optional.of(credentialRequestStatus);
		Mockito.when(statusRepo.findByIndividualIdHashAndPartnerId(any(), any())).thenReturn(credStatusOptional);
		credentialStatusManager.credentialRequestResponseConsumer(request, response);

		additionalData.remove("transaction_limit", 3);
		req.setAdditionalData(additionalData);
		request.setRequest(req);
		credentialStatusManagerSpy.credentialRequestResponseConsumer(request, response);
	}

	/**
	 * This class tests the idaEventConsumer method
	 */
	@Test
	public void idaEventConsumerTest() {
		EventModel eventModel = new EventModel();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(ID_HASH, "value");
		Event event = new Event();
		event.setData(null);
		eventModel.setTopic("Test");
		eventModel.setPublisher("Demo");
		eventModel.setEvent(event);
		credentialStatusManager.idaEventConsumer(eventModel);

		event.setData(data);
		List<CredentialRequestStatus> credStatusList = new ArrayList<>();
		CredentialRequestStatus credentialRequestStatus = new CredentialRequestStatus();
		credStatusList.add(credentialRequestStatus);
		Mockito.when(statusRepo.findByIndividualIdHash(any())).thenReturn(credStatusList);
		credentialStatusManager.idaEventConsumer(eventModel);
	}

	/**
	 * This class tests the handleDeletedRequests method
	 * @throws IdRepoAppException
	 */
	@Test
	public void handleDeletedRequestsTest() throws IdRepoAppException {
		List<CredentialRequestStatus> deletedIssueRequestList = new ArrayList<CredentialRequestStatus>();
		CredentialRequestStatus req = new CredentialRequestStatus();
		req.setIndividualId("12");
		req.setPartnerId("33");
		req.setStatus("Success");
		req.setTokenId("132");
		req.setRequestId("22");
		deletedIssueRequestList.add(req);
		Mockito.when(statusRepo.findByStatus(CredentialRequestStatusLifecycle.DELETED.toString()))
				.thenReturn(deletedIssueRequestList);

		CredentialStatusManager credentialStatusManagerSpy = Mockito.spy(credentialStatusManager);
		Mockito.doReturn("1122").when(credentialStatusManagerSpy).decryptId(any());
		ReflectionTestUtils.invokeMethod(credentialStatusManagerSpy, "handleDeletedRequests");
	}

}
