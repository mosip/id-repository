package io.mosip.idrepository.credential.store.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import io.mosip.kernel.core.websub.model.Event;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.constants.WebSubClientErrorCode;
import io.mosip.kernel.websub.api.exception.WebSubClientException;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("rawtypes")
public class WebSubUtilTest {

	private static final String HUB_URL = "http://localhost/hub";
	private static final String TOPIC = "partner/CREDENTIAL_ISSUED";

	@InjectMocks
	private WebSubUtil webSubUtil;

	@Mock
	private PublisherClient<String, EventModel, HttpHeaders> publisherClient;

	@Before
	public void init() {
		ReflectionTestUtils.setField(webSubUtil, "partnerhuburl", HUB_URL);
		ReflectionTestUtils.setField(webSubUtil, "asyncEnabled", false);
	}

	@Test
	public void registerTopicSuccess() {
		doNothing().when(publisherClient).registerTopic(TOPIC, HUB_URL);
		webSubUtil.registerTopic(TOPIC, "req-1");
		verify(publisherClient).registerTopic(TOPIC, HUB_URL);
	}

	@Test
	public void registerTopicIgnoresAlreadyRegistered() {
		doThrow(new WebSubClientException(WebSubClientErrorCode.REGISTER_ERROR.getErrorCode(),
				"Topic already registered with hub")).when(publisherClient).registerTopic(TOPIC, HUB_URL);
		webSubUtil.registerTopic(TOPIC, "req-2");
		verify(publisherClient).registerTopic(TOPIC, HUB_URL);
	}

	@Test(expected = WebSubClientException.class)
	public void registerTopicRethrowsWhenMessageIsNull() {
		doThrow(new WebSubClientException(WebSubClientErrorCode.REGISTER_ERROR.getErrorCode(), null))
				.when(publisherClient).registerTopic(TOPIC, HUB_URL);
		webSubUtil.registerTopic(TOPIC, "req-null");
	}

	@Test(expected = WebSubClientException.class)
	public void registerTopicRethrowsOtherFailures() {
		doThrow(new WebSubClientException(WebSubClientErrorCode.REGISTER_ERROR.getErrorCode(), "hub unavailable"))
				.when(publisherClient).registerTopic(TOPIC, HUB_URL);
		webSubUtil.registerTopic(TOPIC, "req-3");
	}

	@Test
	public void publishSuccessOnFirstAttempt() throws Exception {
		EventModel eventModel = eventModel("req-4");
		doNothing().when(publisherClient).publishUpdate(eq(TOPIC), eq(eventModel),
				eq(MediaType.APPLICATION_JSON_VALUE), any(HttpHeaders.class), eq(HUB_URL));
		webSubUtil.publishSuccess(TOPIC, eventModel);
		verify(publisherClient).publishUpdate(eq(TOPIC), eq(eventModel),
				eq(MediaType.APPLICATION_JSON_VALUE), any(HttpHeaders.class), eq(HUB_URL));
	}

	@Test
	public void publishSuccessRetriesAfterRegisteringTopic() throws Exception {
		EventModel eventModel = eventModel("req-5");
		doThrow(new WebSubClientException(WebSubClientErrorCode.PUBLISH_ERROR.getErrorCode(), "publish failed"))
				.doNothing()
				.when(publisherClient).publishUpdate(eq(TOPIC), eq(eventModel),
						eq(MediaType.APPLICATION_JSON_VALUE), any(HttpHeaders.class), eq(HUB_URL));
		doNothing().when(publisherClient).registerTopic(TOPIC, HUB_URL);

		webSubUtil.publishSuccess(TOPIC, eventModel);

		verify(publisherClient, times(2)).publishUpdate(eq(TOPIC), eq(eventModel),
				eq(MediaType.APPLICATION_JSON_VALUE), any(HttpHeaders.class), eq(HUB_URL));
		verify(publisherClient).registerTopic(TOPIC, HUB_URL);
	}

	private static EventModel eventModel(String requestId) {
		Event event = new Event();
		event.setTransactionId(requestId);
		EventModel eventModel = new EventModel();
		eventModel.setEvent(event);
		return eventModel;
	}
}
