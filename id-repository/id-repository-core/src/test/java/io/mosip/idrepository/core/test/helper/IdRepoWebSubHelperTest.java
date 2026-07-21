package io.mosip.idrepository.core.test.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.manager.partner.PartnerServiceManager;
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
	private PartnerServiceManager partnerServiceManager;

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
						WebSubClientErrorCode.REGISTER_ERROR.getErrorMessage()
								+ "Topic has already registered with the Hub"))
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
	public void testSendEventToIDAWithNonDummyPartner() {
		EventModel model = new EventModel();
		model.setTopic("partner123/someTopic");

		Mockito.when(dummyCheck.isDummyOLVPartner("partner123")).thenReturn(false);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "dummyCheck", dummyCheck);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publishAsyncEnabled", false);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisherURL", "URL");

		IdRepoWebSubHelper spyHelper = Mockito.spy(idRepoWebSubHelper);

		ReflectionTestUtils.setField(spyHelper, "publisher", publisher);
		ReflectionTestUtils.setField(spyHelper, "publisherURL", "URL");
		ReflectionTestUtils.setField(spyHelper, "publishAsyncEnabled", false);

		spyHelper.sendEventToIDA(model, null);

		Mockito.verify(dummyCheck).isDummyOLVPartner("partner123");
		Mockito.verify(spyHelper, Mockito.never()).tryRegisteringTopic(Mockito.anyString());
		Mockito.verify(publisher).publishUpdate(Mockito.eq("partner123/someTopic"), Mockito.eq(model),
				Mockito.anyString(), Mockito.isNull(), Mockito.eq("URL"));
	}

	@Test
	public void testSendEventToIDAWhenConsumerCalled() {
		EventModel model = new EventModel();
		model.setTopic("partner123/topic");

		Consumer<EventModel> consumer = Mockito.mock(Consumer.class);

		Mockito.when(dummyCheck.isDummyOLVPartner("partner123")).thenReturn(false);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "dummyCheck", dummyCheck);

		IdRepoWebSubHelper spyHelper = Mockito.spy(idRepoWebSubHelper);
		ReflectionTestUtils.setField(spyHelper, "publisher", publisher);
		ReflectionTestUtils.setField(spyHelper, "publisherURL", "URL");
		ReflectionTestUtils.setField(spyHelper, "publishAsyncEnabled", false);

		spyHelper.sendEventToIDA(model, consumer);

		Mockito.verify(consumer, Mockito.times(1)).accept(model);
		Mockito.verify(publisher).publishUpdate(Mockito.eq("partner123/topic"), Mockito.eq(model),
				Mockito.anyString(), Mockito.isNull(), Mockito.eq("URL"));
	}

	@Test
	public void publishEventReregistersTopicOnFailure() {
		String topic = "partner/topic";
		EventModel model = new EventModel();
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisher", publisher);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisherURL", "URL");
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publishAsyncEnabled", false);

		Mockito.doThrow(new WebSubClientException(WebSubClientErrorCode.PUBLISH_ERROR.getErrorCode(),
				WebSubClientErrorCode.PUBLISH_ERROR.getErrorMessage()))
				.doNothing()
				.when(publisher).publishUpdate(Mockito.eq(topic), Mockito.eq(model), Mockito.anyString(),
						Mockito.isNull(), Mockito.eq("URL"));

		IdRepoWebSubHelper spyHelper = Mockito.spy(idRepoWebSubHelper);
		ReflectionTestUtils.setField(spyHelper, "publisher", publisher);
		ReflectionTestUtils.setField(spyHelper, "publisherURL", "URL");
		ReflectionTestUtils.setField(spyHelper, "publishAsyncEnabled", false);

		spyHelper.publishEvent(topic, model);

		Mockito.verify(spyHelper).registerTopicOnFailure(topic);
		Mockito.verify(publisher, Mockito.times(2)).publishUpdate(Mockito.eq(topic), Mockito.eq(model),
				Mockito.anyString(), Mockito.isNull(), Mockito.eq("URL"));
	}

	@Test
	public void testRegisterPublishTopicsAtStartup() {
		ReflectionTestUtils.setField(idRepoWebSubHelper, "credentialStatusUpdateTopic", "cred-status");
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publisherURL", "URL");
		Mockito.when(partnerServiceManager.getOLVPartnerIds()).thenReturn(List.of("partner1"));
		idRepoWebSubHelper.registerPublishTopicsAtStartup();
		Mockito.verify(publisher, Mockito.atLeastOnce()).registerTopic(Mockito.anyString(), Mockito.eq("URL"));
	}

	@Test
	public void testTryRegisterWithNullMessageWebsubException() {
		IdRepoWebSubHelper helper = new IdRepoWebSubHelper();
		PublisherClient publisherClient = Mockito.mock(PublisherClient.class);
		ReflectionTestUtils.setField(helper, "publisher", publisherClient);
		ReflectionTestUtils.setField(helper, "publisherURL", "URL");
		Mockito.doThrow(new WebSubClientException(WebSubClientErrorCode.REGISTER_ERROR.getErrorCode(), null))
				.when(publisherClient).registerTopic(Mockito.any(), Mockito.anyString());
		helper.tryRegisteringTopic("topic-null-msg");
		Set<?> field = (Set<?>) ReflectionTestUtils.getField(helper, "registeredTopicCache");
		assertEquals(0, field.size());
	}

	@Test
	public void publishAuthTypeStatusUpdateEventWithDataMapTest() {
		Map<String, String> dataMap = new HashMap<>();
		dataMap.put("tokenId", "generated-token");
		Mockito.when(tokenIdGenerator.generateTokenID("123", "66")).thenReturn("generated-token");
		Mockito.when(mapper.convertValue(Mockito.any(), Mockito.eq(Map.class))).thenReturn(dataMap);
		IdRepoWebSubHelper spyHelper = Mockito.spy(idRepoWebSubHelper);
		ReflectionTestUtils.setField(spyHelper, "publishAsyncEnabled", false);
		ReflectionTestUtils.setField(spyHelper, "publisher", publisher);
		ReflectionTestUtils.setField(spyHelper, "publisherURL", "URL");
		spyHelper.publishAuthTypeStatusUpdateEvent("123", new ArrayList<>(), "topic", "66");
		Mockito.verify(publisher).publishUpdate(Mockito.anyString(), Mockito.any(EventModel.class),
				Mockito.anyString(), Mockito.isNull(), Mockito.eq("URL"));
	}

	@Test
	public void createEventModelWithExpiryAndDataMapTest() throws InterruptedException, ExecutionException {
		EventType eventType = IDAEventType.CREDENTIAL_ISSUED;
		LocalDateTime expiryTimestamp = LocalDateTime.of(2030, 6, 15, 10, 30);
		EventModel res = idRepoWebSubHelper
				.createEventModel(eventType, expiryTimestamp, 25, "tx-1", "partnerA", "hashVal").get();
		assertEquals("ID_REPO", res.getPublisher());
		assertTrue(res.getEvent().getData().containsKey(IdRepoConstants.EXPIRY_TIMESTAMP));
		assertEquals(25, res.getEvent().getData().get(IdRepoConstants.TRANSACTION_LIMIT));
	}

	@Test
	public void testSendEventToIDADummyPartnerSkipped() {
		EventModel model = new EventModel();
		model.setTopic("dummyPartner/topic");
		Mockito.when(dummyCheck.isDummyOLVPartner("dummyPartner")).thenReturn(true);
		ReflectionTestUtils.setField(idRepoWebSubHelper, "publishAsyncEnabled", false);
		idRepoWebSubHelper.sendEventToIDA(model, null);
		Mockito.verify(publisher, Mockito.never()).publishUpdate(Mockito.anyString(), Mockito.any(),
				Mockito.anyString(), Mockito.isNull(), Mockito.anyString());
	}

	@Test
	public void testSubscribeForVidEventFailure() {
		ReflectionTestUtils.setField(idRepoWebSubHelper, "vidEventTopic", "vid-topic");
		ReflectionTestUtils.setField(idRepoWebSubHelper, "vidEventUrl", "http://callback");
		ReflectionTestUtils.setField(idRepoWebSubHelper, "vidEventSecret", "secret");
		ReflectionTestUtils.setField(idRepoWebSubHelper, "hubURL", "http://hub");
		Mockito.doThrow(new RuntimeException("subscribe failed")).when(subscribe).subscribe(Mockito.any());
		idRepoWebSubHelper.subscribeForVidEvent();
		Mockito.verify(subscribe).subscribe(Mockito.any(SubscriptionChangeRequest.class));
	}

}
