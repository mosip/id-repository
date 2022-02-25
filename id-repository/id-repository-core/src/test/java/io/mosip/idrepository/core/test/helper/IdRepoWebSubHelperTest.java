package io.mosip.idrepository.core.test.helper;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

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
	protected SubscriptionClient<SubscriptionChangeRequest, UnsubscriptionRequest, SubscriptionChangeResponse> subscribe;

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

}
