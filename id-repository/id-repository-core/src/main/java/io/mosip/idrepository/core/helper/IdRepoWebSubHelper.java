package io.mosip.idrepository.core.helper;

import static io.mosip.idrepository.core.constant.IdRepoConstants.EXPIRY_TIMESTAMP;
import static io.mosip.idrepository.core.constant.IdRepoConstants.ID_HASH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.ID_REPO;
import static io.mosip.idrepository.core.constant.IdRepoConstants.TRANSACTION_LIMIT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_EVENT_CALLBACK_URL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_EVENT_SECRET;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_EVENT_TOPIC;
import static io.mosip.idrepository.core.constant.IdRepoConstants.WEB_SUB_HUB_URL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.WEB_SUB_PUBLISH_URL;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.constant.EventType;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.dto.AuthTypeStatusEventDTO;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.kernel.core.websub.model.Event;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.model.Type;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.core.websub.spi.SubscriptionClient;
import io.mosip.kernel.websub.api.constants.WebSubClientErrorCode;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.kernel.websub.api.model.SubscriptionChangeRequest;
import io.mosip.kernel.websub.api.model.SubscriptionChangeResponse;
import io.mosip.kernel.websub.api.model.UnsubscriptionRequest;

/**
 * @author Manoj SP
 *
 */
@Component
@Async("webSubHelperExecutor")
public class IdRepoWebSubHelper {

	private static final String SEND_EVENT_TO_IDA = "sendEventToIDA";

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

	/** The ida event type namespace. */
	@Value("${id-repo-ida-event-type-namespace:mosip}")
	private String idaEventTypeNamespace;

	/** The ida event type name. */
	@Value("${id-repo-ida-event-type-name:ida}")
	private String idaEventTypeName;
	
	@Autowired
	private PublisherClient<String, Object, HttpHeaders> publisher;

	@Autowired
	private TokenIDGenerator tokenIdGenerator;

	@Autowired
	private DummyPartnerCheckUtil dummyCheck;

	@Autowired
	protected SubscriptionClient<SubscriptionChangeRequest, UnsubscriptionRequest, SubscriptionChangeResponse> subscribe;
	
	@Autowired
	private ObjectMapper mapper;

	/**
	 * Topics for which registration has been confirmed at the WebSub hub
	 * (either we successfully registered them, or the hub told us they were
	 * already registered via {@link WebSubClientErrorCode#REGISTER_ERROR}).
	 *
	 * <p>Thread-safe because this helper is annotated {@code @Async} at the
	 * class level — multiple {@code webSubHelperExecutor} threads invoke
	 * {@link #tryRegisteringTopic(String)} concurrently and the original
	 * plain {@link java.util.HashSet} was unsafe under concurrent {@code add}.
	 */
	private final Set<String> registeredTopicCache = ConcurrentHashMap.newKeySet();

	/**
	 * Attempts to register a topic with the WebSub hub if it has not already
	 * been confirmed registered. Idempotent and safe to call from multiple
	 * threads.
	 *
	 * <p>Return semantics:
	 * <ul>
	 *   <li>{@code true} — registration is confirmed at the hub (either we
	 *       just registered it, the hub reported it was already registered,
	 *       or it was previously cached). Subsequent {@code publishUpdate}
	 *       calls can reasonably be expected to be authorized.</li>
	 *   <li>{@code false} — registration could not be confirmed (the hub was
	 *       unreachable, the publisher's credentials were rejected, or some
	 *       other unexpected error). Callers should NOT publish in this case
	 *       — doing so produces the {@code hub.mode=denied&hub.reason=
	 *       Publisher is not authorized} response that previously flooded the
	 *       logs.</li>
	 * </ul>
	 *
	 * <p>The method previously returned {@code void} and silently swallowed
	 * failures; callers therefore proceeded to publish even when registration
	 * had not happened at the hub. Returning a status code lets callers make
	 * an informed decision.
	 */
	public boolean tryRegisteringTopic(String topic) {
		if (registeredTopicCache.contains(topic)) {
			return true;
		}
		try {
			this.registerTopic(topic);
			registeredTopicCache.add(topic);
			return true;
		} catch (WebSubClientException e) {
			if (WebSubClientErrorCode.REGISTER_ERROR.getErrorCode().equals(e.getErrorCode())) {
				// Hub reports the topic is already registered — treat as success
				// and cache so we stop hammering the hub on every call.
				registeredTopicCache.add(topic);
				return true;
			}
			mosipLogger.warn(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(),
					"tryRegisteringTopic",
					"Topic registration FAILED — will NOT publish | topic=" + topic
							+ " | publisherURL=" + publisherURL
							+ " | errorCode=" + e.getErrorCode()
							+ " | error=" + e.getMessage());
			return false;
		} catch (Exception e) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(),
					"tryRegisteringTopic",
					"Topic registration FAILED (unexpected error) — will NOT publish | topic=" + topic
							+ " | publisherURL=" + publisherURL
							+ " | error=" + ExceptionUtils.getStackTrace(e));
			return false;
		}
	}

	/**
	 * Removes a topic from the local registered-topic cache so that the next
	 * publish attempt will re-attempt registration at the hub.
	 *
	 * <p>Called after a publish failure to make the helper self-healing: if
	 * registration was previously cached but the hub is now rejecting the
	 * publish as unauthorized (for example because the hub's authorization
	 * state has been reset, or because our cache was populated by a stale
	 * "already registered" response that the hub no longer honours), the
	 * cache is purged so the next call goes through registration again.
	 */
	void evictTopicRegistrationCache(String topic) {
		if (registeredTopicCache.remove(topic)) {
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(),
					"evictTopicRegistrationCache",
					"Evicted topic from registration cache after publish failure | topic=" + topic);
		}
	}

	@SuppressWarnings("unchecked")
	public void publishAuthTypeStatusUpdateEvent(String individualId, List<AuthtypeStatus> authTypeStatusList,
			String topic, String partnerId) {
		AuthTypeStatusEventDTO event = new AuthTypeStatusEventDTO();
		event.setTokenId(tokenIdGenerator.generateTokenID(individualId, partnerId));
		event.setAuthTypeStatusList(authTypeStatusList);
		Map<String, String> dataMap = mapper.convertValue(event, Map.class);
		EventModel eventModel = createEventModel(IDAEventType.AUTH_TYPE_STATUS_UPDATE, null, null, null, partnerId,
				null, dataMap);
		// Only publish if registration is confirmed at the hub. Publishing
		// without confirmed registration produces the noisy "Publisher is not
		// authorized" stream that previously dominated the error logs.
		if (this.tryRegisteringTopic(topic)) {
			this.publishEvent(eventModel);
		} else {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(),
					"publishAuthTypeStatusUpdateEvent",
					"Skipped publish — topic registration not confirmed | topic=" + topic);
		}
	}
	
	/**
	 * Creates the event model.
	 *
	 * @param eventType        the event type
	 * @param id               the id
	 * @param expiryTimestamp  the expiry timestamp
	 * @param transactionLimit the transaction limit
	 * @param transactionId    the transaction id
	 * @param partner          the partner
	 * @param idHash           the id hash
	 * @return the event model
	 */
	public Future<EventModel> createEventModel(EventType eventType, LocalDateTime expiryTimestamp, Integer transactionLimit,
			String transactionId, String partner, String idHash) {
		EventModel eventModel = createEventModel(eventType, expiryTimestamp, transactionLimit, transactionId, partner, idHash, null);
		return new AsyncResult<>(eventModel);
	}
	
	/**
	 * Creates the event model.
	 *
	 * @param eventType        the event type
	 * @param id               the id
	 * @param expiryTimestamp  the expiry timestamp
	 * @param transactionLimit the transaction limit
	 * @param transactionId    the transaction id
	 * @param partner          the partner
	 * @param idHash           the id hash
	 * @return the event model
	 */
	private EventModel createEventModel(EventType eventType, LocalDateTime expiryTimestamp, Integer transactionLimit,
			String transactionId, String partner, String idHash, Map<String, String> dataMap) {
		EventModel model = new EventModel();
		model.setPublisher(ID_REPO);
		String dateTime = DateUtils2.formatToISOString(DateUtils2.getUTCCurrentDateTime());
		model.setPublishedOn(dateTime);
		Event event = new Event();
		event.setTimestamp(dateTime);
		String eventId = UUID.randomUUID().toString();
		event.setId(eventId);
		event.setTransactionId(transactionId);
		Type type = new Type();
		type.setNamespace(idaEventTypeNamespace);
		type.setName(idaEventTypeName);
		event.setType(type);
		Map<String, Object> data = new HashMap<>();
		if(dataMap != null && !dataMap.isEmpty()) {
			data.putAll(dataMap);
		}
		data.put(ID_HASH, idHash);
		if (eventType.equals(IDAEventType.DEACTIVATE_ID)) {
			data.put(EXPIRY_TIMESTAMP, DateUtils2.formatToISOString(DateUtils2.getUTCCurrentDateTime()));
		} else {
			if (expiryTimestamp != null) {
				data.put(EXPIRY_TIMESTAMP, DateUtils2.formatToISOString(expiryTimestamp));
			}
		}
		if(transactionLimit != null) {
			data.put(TRANSACTION_LIMIT, transactionLimit);
		}
		event.setData(data);
		model.setEvent(event);
		model.setTopic(partner + "/" + eventType.toString());
		return model;
	}

	/**
	 * Send event to IDA.
	 *
	 * @param model the model
	 */
	public void sendEventToIDA(EventModel model, Consumer<EventModel> idaEventModelConsumer) {
		if (idaEventModelConsumer != null) {
			idaEventModelConsumer.accept(model);
		}

		String partnerId = model.getTopic().split("//")[0];
		if (!dummyCheck.isDummyOLVPartner(partnerId)) {
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), SEND_EVENT_TO_IDA,
					"Trying registering topic: " + model.getTopic());

			// tryRegisteringTopic never throws — its return value tells us
			// whether registration is actually confirmed at the hub. The old
			// try/catch around it was dead code, and publishing regardless of
			// the registration outcome is what was producing the "Publisher is
			// not authorized" log flood.
			boolean registered = this.tryRegisteringTopic(model.getTopic());
			if (!registered) {
				mosipLogger.warn(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(),
						SEND_EVENT_TO_IDA,
						"Skipped publish — topic registration not confirmed | topic=" + model.getTopic());
				return;
			}

			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), SEND_EVENT_TO_IDA,
					"Publising event to topic: " + model.getTopic());
			this.publishEvent(model);
		}
	}

	public void subscribeForVidEvent() {
		try {
			SubscriptionChangeRequest subscriptionRequest = new SubscriptionChangeRequest();
			subscriptionRequest.setCallbackURL(vidEventUrl);
			subscriptionRequest.setHubURL(hubURL);
			subscriptionRequest.setSecret(vidEventSecret);
			subscriptionRequest.setTopic(vidEventTopic);
			subscribe.subscribe(subscriptionRequest);
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "subscribeForVidEvent",
					"subscribed event topic: " + vidEventTopic);
		} catch (Exception e) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "subscribeForVidEvent",
					"Error subscribing topic: " + vidEventTopic + "\n" + e.getMessage());
		}
	}

	public void publishEvent(EventModel event) {
		this.publishEvent(event.getTopic(), event);
	}
	
	public void registerTopic(String topic) {
		publisher.registerTopic(topic, publisherURL);
	}
	
	public <U> void publishEvent(String eventTopic, U eventModel) {
		try {
			publisher.publishUpdate(eventTopic, eventModel, MediaType.APPLICATION_JSON_VALUE, null, publisherURL);
		} catch (WebSubClientException e) {
			/*
			 * Hub rejected the publish — the most common cause is the
			 * "Publisher is not authorized" response, which means our cached
			 * registration for this topic is no longer honoured by the hub
			 * (or never actually took effect — see tryRegisteringTopic).
			 *
			 * Evict the topic from the local cache so the very next call
			 * re-runs registration against the hub, making the helper
			 * self-healing once the underlying authorization issue is fixed.
			 *
			 * The exception is re-thrown so that Spring's async exception
			 * handler still records it (preserving existing telemetry) and
			 * any synchronous caller sees the failure.
			 */
			evictTopicRegistrationCache(eventTopic);
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(),
					"publishEvent",
					"Publish FAILED at hub | topic=" + eventTopic
							+ " | publisherURL=" + publisherURL
							+ " | errorCode=" + e.getErrorCode()
							+ " | error=" + e.getMessage());
			throw e;
		}
	}
}
