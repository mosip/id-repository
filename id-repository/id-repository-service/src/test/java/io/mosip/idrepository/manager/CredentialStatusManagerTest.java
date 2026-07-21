package io.mosip.idrepository.manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.constant.CredentialRequestStatusLifecycle;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.repository.CredentialRequestStatusRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.kernel.core.websub.model.Event;
import io.mosip.kernel.core.websub.model.EventModel;

@RunWith(MockitoJUnitRunner.class)
public class CredentialStatusManagerTest {

	private static final String ID_HASH = "id_hash";

	@InjectMocks
	private CredentialStatusManager credentialStatusManager = new CredentialStatusManager();

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

	@Mock
	private DummyPartnerCheckUtil dummyPartner;

	@Before
	public void before() {
		ReflectionTestUtils.setField(credentialStatusManager, "uinRefId", "123");
		ReflectionTestUtils.setField(credentialStatusManager, "credentailStatusUpdateTopic", "topic");
	}

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

	@Test
	public void credentialRequestResponseConsumerTest() throws IdRepoAppException {
		CredentialIssueRequestWrapperDto request = new CredentialIssueRequestWrapperDto();
		CredentialIssueRequestDto req = new CredentialIssueRequestDto();
		Map<String, Object> additionalData = new HashMap<>();
		additionalData.put("id_hash", "value1");
		req.setAdditionalData(additionalData);
		req.setIssuer("Test");
		req.setId("1");
		request.setId("1");
		request.setVersion("2.0");
		request.setRequesttime(LocalDateTime.now());
		request.setRequest(req);
		Map<String, Object> response = new HashMap<>();
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

	@Test
	public void idaEventConsumerTest() {
		EventModel eventModel = new EventModel();
		Map<String, Object> data = new HashMap<>();
		data.put(ID_HASH, "value");
		Event event = new Event();
		event.setData(null);
		eventModel.setTopic("Test");
		eventModel.setPublisher("Demo");
		eventModel.setEvent(event);
		credentialStatusManager.idaEventConsumer(eventModel);

		event.setData(data);
		List<CredentialRequestStatus> credStatusList = new ArrayList<>();
		credStatusList.add(new CredentialRequestStatus());
		Mockito.when(statusRepo.findByIndividualIdHash(any())).thenReturn(credStatusList);
		credentialStatusManager.idaEventConsumer(eventModel);
	}
}
