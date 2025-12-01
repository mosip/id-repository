package io.mosip.idrepository.core.test.helper;

import static org.junit.Assert.assertEquals;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.constant.EventType;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.core.websub.spi.SubscriptionClient;
import io.mosip.kernel.websub.api.constants.WebSubClientErrorCode;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.kernel.websub.api.model.SubscriptionChangeRequest;
import io.mosip.kernel.websub.api.model.SubscriptionChangeResponse;
import io.mosip.kernel.websub.api.model.UnsubscriptionRequest;
import java.util.function.Consumer;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * 
 * @author Loganathan S
 *
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({ "unchecked", "rawtypes" })
public class IdRepoWebSubHelperTest {

	@InjectMocks
	private IdRepoWebSubHelper idRepoWebSubHelper;

	@Mock
	private TokenIDGenerator tokenIdGenerator;

	@Mock
	private ObjectMapper mapper;

	@Mock
	private PublisherClient<String, Object, HttpHeaders> publisher;

	@Mock
	private DummyPartnerCheckUtil dummyCheck;

	@Mock
	protected SubscriptionClient<SubscriptionChangeRequest, UnsubscriptionRequest, SubscriptionChangeResponse> subscribe;

	@Mock
	private Logger mosipLogger;

	@Test
	public void testTryRegisterCache() {
		IdRepoWebSubHelper idRepoWebSubHelper = new IdRepoWebSubHelper();
		PublisherClient publisherClient = Mockito.mock(PublisherClient.class);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisher", publisherClient);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisherURL", "URL");

		idRepoWebSubHelper.tryRegisteringTopic("topic");
		// Throw the error for the second time to make sure if it is cached in first
		// call
		Mockito.lenient().doThrow(new RuntimeException()).when(publisherClient).registerTopic(Mockito.any(),
				Mockito.anyString());
		;
		idRepoWebSubHelper.tryRegisteringTopic("topic");
		Set field = (Set) ReflectionTestUtils.getField(idRepoWebSubHelper, "registeredTopicCache");
		assertEquals(1, field.size());
	}

	@Test
	public void testTryRegisterCache_withRegistrationException() {
		IdRepoWebSubHelper idRepoWebSubHelper = new IdRepoWebSubHelper();
		PublisherClient publisherClient = Mockito.mock(PublisherClient.class);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisher", publisherClient);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisherURL", "URL");

		// Throw the error for the second time to make sure if it is cached in first
		// call
		Mockito.lenient()
				.doThrow(new WebSubClientException(WebSubClientErrorCode.REGISTER_ERROR.getErrorCode(),
						WebSubClientErrorCode.REGISTER_ERROR.getErrorMessage()))
				.when(publisherClient).registerTopic(Mockito.any(), Mockito.anyString());
		;
		idRepoWebSubHelper.tryRegisteringTopic("topic");
		Set field = (Set) ReflectionTestUtils.getField(idRepoWebSubHelper, "registeredTopicCache");
		assertEquals(1, field.size());
	}

	@Test
	public void testTryRegisterCache_withNonRegistrationWebsubException() {
		IdRepoWebSubHelper idRepoWebSubHelper = new IdRepoWebSubHelper();
		PublisherClient publisherClient = Mockito.mock(PublisherClient.class);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisher", publisherClient);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisherURL", "URL");

		// Throw the error for the first time only to make sure if it is cached in
		// second call
		Mockito.lenient()
				.doThrow(new WebSubClientException(WebSubClientErrorCode.AUTHENTTICATED_CONTENT_ERROR.getErrorCode(),
						WebSubClientErrorCode.AUTHENTTICATED_CONTENT_ERROR.getErrorMessage()))
				.when(publisherClient).registerTopic(Mockito.any(), Mockito.anyString());
		idRepoWebSubHelper.tryRegisteringTopic("topic");
		Set field = (Set) ReflectionTestUtils.getField(idRepoWebSubHelper, "registeredTopicCache");
		assertEquals(0, field.size());

		Mockito.lenient().doNothing().when(publisherClient).registerTopic(Mockito.any(), Mockito.anyString());
		idRepoWebSubHelper.tryRegisteringTopic("topic");
		assertEquals(1, field.size());
	}

	@Test
	public void testTryRegisterCache_withNonWebsubException() {
		IdRepoWebSubHelper idRepoWebSubHelper = new IdRepoWebSubHelper();
		PublisherClient publisherClient = Mockito.mock(PublisherClient.class);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisher", publisherClient);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisherURL", "URL");

		// Throw the error for the first time only to make sure if it is cached in
		// second call
		Mockito.lenient().doThrow(new RuntimeException()).when(publisherClient).registerTopic(Mockito.any(),
				Mockito.anyString());
		idRepoWebSubHelper.tryRegisteringTopic("topic");
		Set field = (Set) ReflectionTestUtils.getField(idRepoWebSubHelper, "registeredTopicCache");
		assertEquals(0, field.size());

		Mockito.lenient().doNothing().when(publisherClient).registerTopic(Mockito.any(), Mockito.anyString());
		idRepoWebSubHelper.tryRegisteringTopic("topic");
		assertEquals(1, field.size());
	}

	@Test
	public void publishAuthTypeStatusUpdateEventTest() {
		String individualId = "123";
		List<AuthtypeStatus> authTypeStatusList = new ArrayList<AuthtypeStatus>();
		String topic = "Test";
		String partnerId = "66";
		idRepoWebSubHelper.publishAuthTypeStatusUpdateEvent(individualId, authTypeStatusList, topic, partnerId);
	}

	@Test
	public void createEventModelTest() throws InterruptedException, ExecutionException {
		EventType eventType = IDAEventType.DEACTIVATE_ID;
		LocalDateTime expiryTimestamp = LocalDateTime.now();
		Integer transactionLimit = 10;
		String transactionId = "12";
		String partner = "Test";
		String idHash = "Azsa";
		EventModel res = idRepoWebSubHelper.createEventModel(eventType, expiryTimestamp, transactionLimit,
				transactionId, partner, idHash).get();
		assertEquals("ID_REPO", res.getPublisher());
	}

	@Test
	public void subscribeForVidEventTest() {
		idRepoWebSubHelper.subscribeForVidEvent();
	}

	@Test
	public void testSendEventToIDA_nonDummyPartner() {
		// Prepare model
		EventModel model = new EventModel();
		model.setTopic("partner123//someTopic");

		// Mock dummyCheck
		Mockito.when(dummyCheck.isDummyOLVPartner("partner123")).thenReturn(false);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "dummyCheck", dummyCheck);

		// Spy IdRepoWebSubHelper to verify internal method calls
		IdRepoWebSubHelper spyHelper = Mockito.spy(idRepoWebSubHelper);

		// Inject spy to replace main instance
		ReflectionTestUtils.setField(spyHelper, "publisher", publisher);

		// Invoke method
		spyHelper.sendEventToIDA(model, null);

		// verify partner check
		Mockito.verify(dummyCheck).isDummyOLVPartner("partner123");

		// verify topic registration
		Mockito.verify(spyHelper).tryRegisteringTopic("partner123//someTopic");

		// verify publish event
		Mockito.verify(spyHelper).publishEvent(model);
	}

	@Test
	public void testSendEventToIDA_consumerCalled() {
		EventModel model = new EventModel();
		model.setTopic("partner123//topic");

		Consumer<EventModel> consumer = Mockito.mock(Consumer.class);

		Mockito.when(dummyCheck.isDummyOLVPartner("partner123")).thenReturn(false);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "dummyCheck", dummyCheck);

		IdRepoWebSubHelper spyHelper = Mockito.spy(idRepoWebSubHelper);
		ReflectionTestUtils.setField(spyHelper, "publisher", publisher);

		spyHelper.sendEventToIDA(model, consumer);

		Mockito.verify(consumer, Mockito.times(1)).accept(model);
	}

	@Test
	public void testSendEventToIDA_registrationExceptionIgnored() {
		EventModel model = new EventModel();
		model.setTopic("partnerABC//sample");

		Mockito.when(dummyCheck.isDummyOLVPartner("partnerABC")).thenReturn(false);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "dummyCheck", dummyCheck);

		IdRepoWebSubHelper spyHelper = Mockito.spy(idRepoWebSubHelper);

		// Force exception
		Mockito.doThrow(new RuntimeException("Already registered"))
				.when(spyHelper).tryRegisteringTopic("partnerABC//sample");

		spyHelper.sendEventToIDA(model, null);

		// publishEvent must still be called
		Mockito.verify(spyHelper).publishEvent(model);
	}

}
