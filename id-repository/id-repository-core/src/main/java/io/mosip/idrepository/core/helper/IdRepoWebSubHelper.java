package io.mosip.idrepository.core.helper;

import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_EVENT_CALLBACK_URL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_EVENT_SECRET;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_EVENT_TOPIC;
import static io.mosip.idrepository.core.constant.IdRepoConstants.WEB_SUB_HUB_URL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.WEB_SUB_PUBLISH_URL;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.dto.IDAEventDTO;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.core.websub.spi.SubscriptionClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.kernel.websub.api.model.SubscriptionChangeRequest;
import io.mosip.kernel.websub.api.model.SubscriptionChangeResponse;
import io.mosip.kernel.websub.api.model.UnsubscriptionRequest;

/**
 * @author Manoj SP
 *
 */
@Component
public class IdRepoWebSubHelper {

	/** The mosip logger. */
	Logger mosipLogger = IdRepoLogger.getLogger(IdRepoWebSubHelper.class);

	@Value("${" + WEB_SUB_PUBLISH_URL + "}")
	private String publisherURL;

	@Value("${" + WEB_SUB_HUB_URL + "}")
	private String hubURL;

	@Value("${" + VID_EVENT_TOPIC + "}")
	private String vidEventTopic;

	@Value("${" + VID_EVENT_SECRET + "}")
	private String vidEventSecret;

	@Value("${" + VID_EVENT_CALLBACK_URL + "}")
	private String vidEventUrl;

	@Autowired
	private PublisherClient<String, IDAEventDTO, HttpHeaders> authTypePublisher;

	@Autowired
	private PublisherClient<String, EventModel, HttpHeaders> publisher;

	@Autowired
	private TokenIDGenerator tokenIdGenerator;

	@Autowired
	private DummyPartnerCheckUtil dummyCheck;

	@Autowired
	protected SubscriptionClient<SubscriptionChangeRequest, UnsubscriptionRequest, SubscriptionChangeResponse> subscribe;

	private Set<String> registeredTopicCache = new HashSet<>();

	/*
	 * Cacheable is added to execute topic registration only once per topic
	 */
	@Async
	public void tryRegisteringTopic(String topic) {
		synchronized (registeredTopicCache) {
			// Skip topic registration if already registered
			if (registeredTopicCache.contains(topic)) {
				return;
			} else {
				try {
					publisher.registerTopic(topic, publisherURL);
					registeredTopicCache.add(topic);
				} catch (WebSubClientException e) {
					mosipLogger.warn(IdRepoSecurityManager.getUser(), "IdRepoConfig", "init", e.getMessage().toUpperCase());
				} catch (IdRepoAppUncheckedException e) {
					mosipLogger.warn(IdRepoSecurityManager.getUser(), "IdRepoConfig", "init", e.getMessage().toUpperCase());
				}
			}
		}
	}

	@Async
	public void publishAuthTypeStatusUpdateEvent(String individualId, List<AuthtypeStatus> authTypeStatusList, String topic,
			String partnerId) {
		IDAEventDTO event = new IDAEventDTO();
		event.setTokenId(tokenIdGenerator.generateTokenID(individualId, partnerId));
		event.setAuthTypeStatusList(authTypeStatusList);
		authTypePublisher.publishUpdate(topic, event, MediaType.APPLICATION_JSON_UTF8_VALUE, null, publisherURL);
	}

	/**
	 * Send event to IDA.
	 *
	 * @param model the model
	 */
	@Async
	public void sendEventToIDA(EventModel model, Consumer<EventModel> idaEventModelConsumer) {
		if (idaEventModelConsumer != null) {
			idaEventModelConsumer.accept(model);
		}

		String partnerId = model.getTopic().split("//")[0];
		if (!dummyCheck.isDummyOLVPartner(partnerId)) {
			try {
				mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "sendEventToIDA",
						"Trying registering topic: " + model.getTopic());
				this.tryRegisteringTopic(model.getTopic());
			} catch (Exception e) {
				// Exception will be there if topic already registered. Ignore that
				mosipLogger.warn(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "sendEventToIDA",
						"Error in registering topic: " + model.getTopic() + " : " + e.getMessage());
			}
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "sendEventToIDA",
					"Publising event to topic: " + model.getTopic());
			publisher.publishUpdate(model.getTopic(), model, MediaType.APPLICATION_JSON_VALUE, null, publisherURL);
		}
	}

	public void subscribeForVidEvent() {
		try {
			this.tryRegisteringTopic(vidEventTopic);
			SubscriptionChangeRequest subscriptionRequest = new SubscriptionChangeRequest();
			subscriptionRequest.setCallbackURL(vidEventUrl);
			subscriptionRequest.setHubURL(hubURL);
			subscriptionRequest.setSecret(vidEventSecret);
			subscriptionRequest.setTopic(vidEventTopic);
			subscribe.subscribe(subscriptionRequest);
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "subscribeForVidEvent",
					"subscribed event topic: " + vidEventTopic);
		} catch (Exception e) {
			System.err.println(ExceptionUtils.getStackTrace(e));
			mosipLogger.warn(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "subscribeForVidEvent",
					"Error subscribing topic: " + vidEventTopic + "\n" + e.getMessage());
		}
	}

	@Async
	public void publishEvent(EventModel event) {
		this.tryRegisteringTopic(event.getTopic());
		publisher.publishUpdate(event.getTopic(), event, MediaType.APPLICATION_JSON_VALUE, null, publisherURL);
	}
}
