package io.mosip.idrepository.core.helper;

import io.mosip.kernel.core.util.DateUtils2;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREDENTIAL_STATUS_UPDATE_TOPIC;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDA_EVENT_TYPE_NAME;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDA_EVENT_TYPE_NAME_DEFAULT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDA_EVENT_TYPE_NAMESPACE;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDA_EVENT_TYPE_NAMESPACE_DEFAULT;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.constant.EventType;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.dto.AuthTypeStatusEventDTO;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.manager.partner.PartnerServiceManager;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
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
 * WebSub publisher and subscriber helper for ID Repository event notifications.
 * <p>
 * Manages topic registration at startup, kernel {@link EventModel} construction, and
 * publish/subscribe against the MOSIP WebSub hub. Publish methods honour
 * {@link IdRepoConstants#WEBSUB_PUBLISH_ASYNC_ENABLED}: when async (default), work
 * runs on {@code webSubHelperExecutor}; when sync, hub I/O blocks the caller.
 * </p>
 *
 * <h2>Primary consumers (id-repository-service)</h2>
 * <ul>
 *   <li>{@code io.mosip.idrepository.manager.CredentialServiceManager} — IDA credential
 *       issued / activate / deactivate / remove events</li>
 *   <li>Identity auth-type status APIs — {@link #publishAuthTypeStatusUpdateEvent}</li>
 *   <li>{@code io.mosip.idrepository.identity.config.IdentityWebsubConfig} — calls
 *       {@link #registerPublishTopicsAtStartup()} and {@link #subscribeForVidEvent()} at
 *       {@code ApplicationReadyEvent}</li>
 * </ul>
 *
 * <h2>Topic naming</h2>
 * <ul>
 *   <li>Partner-scoped IDA topics: {@code {partnerId}/{IDAEventType}} (for example
 *       {@code PARTNER/CREDENTIAL_ISSUED})</li>
 *   <li>Global topics: {@code IDENTITY_CREATED}, {@code IDENTITY_UPDATED}, credential status topic</li>
 *   <li>VID subscribe topic from {@code id-repo-websub-vid-topic} (and related callback/secret)</li>
 * </ul>
 *
 * <h2>Failure handling</h2>
 * <p>
 * Registration treats “already registered” as success. “Publisher is not authorized” is logged
 * as ERROR with ACL/IAM hints. Publish failures trigger one re-register + retry via
 * {@link #registerTopicOnFailure(String)}.
 * </p>
 *
 * @see io.mosip.kernel.core.websub.spi.PublisherClient
 * @see io.mosip.kernel.core.websub.spi.SubscriptionClient
 * @see EventType
 * @see IDAEventType
 * @author Manoj SP
 */
@Component
public class IdRepoWebSubHelper {

	/** Log method name for IDA publishes path. */
	private static final String SEND_EVENT_TO_IDA = "sendEventToIDA";

	/** Partner-prefixed topics id-repository publishes ({@code partner/EVENT}). */
	private static final Set<IDAEventType> PARTNER_SCOPED_PUBLISH_EVENTS = EnumSet.of(
			IDAEventType.CREDENTIAL_ISSUED,
			IDAEventType.REMOVE_ID,
			IDAEventType.DEACTIVATE_ID,
			IDAEventType.ACTIVATE_ID,
			IDAEventType.AUTH_TYPE_STATUS_UPDATE);

	/** Structured logger for WebSub operations. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(IdRepoWebSubHelper.class);

	/** WebSub publisher hub URL for topic registration and event publishing. */
	@Value("${" + WEB_SUB_PUBLISH_URL + "}")
	private String publisherURL;

	/** WebSub hub base URL for subscription management. */
	@Value("${" + WEB_SUB_HUB_URL + "}")
	private String hubURL;

	/** Topic name for VID lifecycle events consumed by this service. */
	@Value("${" + VID_EVENT_TOPIC + "}")
	private String vidEventTopic;

	/** Shared secret for VID event subscription callback verification. */
	@Value("${" + VID_EVENT_SECRET + "}")
	private String vidEventSecret;

	/** Callback URL registered for VID event WebSub delivery. */
	@Value("${" + VID_EVENT_CALLBACK_URL + "}")
	private String vidEventUrl;

	/** WebSub event type namespace for IDA events (default {@code mosip}). */
	@Value("${" + IDA_EVENT_TYPE_NAMESPACE + ":" + IDA_EVENT_TYPE_NAMESPACE_DEFAULT + "}")
	private String idaEventTypeNamespace;

	/** WebSub event type name for IDA events (default {@code ida}). */
	@Value("${" + IDA_EVENT_TYPE_NAME + ":" + IDA_EVENT_TYPE_NAME_DEFAULT + "}")
	private String idaEventTypeName;

	/** Topic for credential status update events published by identity. */
	@Value("${" + CREDENTIAL_STATUS_UPDATE_TOPIC + "}")
	private String credentialStatusUpdateTopic;

	/**
	 * When {@code true}, event publish runs off the caller thread.
	 * Property: {@link IdRepoConstants#WEBSUB_PUBLISH_ASYNC_ENABLED}.
	 */
	@Value("${" + IdRepoConstants.WEBSUB_PUBLISH_ASYNC_ENABLED + ":true}")
	private boolean publishAsyncEnabled;

	/** Executor for async publish. Optional so unit tests without the bean still run sync. */
	@Autowired(required = false)
	@Qualifier("webSubHelperExecutor")
	private Executor webSubHelperExecutor;

	/** Kernel WebSub publisher client for topic registration and publish. */
	@Autowired
	private PublisherClient<String, Object, HttpHeaders> publisher;

	/** Generates partner-scoped token IDs embedded in auth-type status events. */
	@Autowired
	private TokenIDGenerator tokenIdGenerator;

	/** Utility to skip publishing to dummy/test OLV partners. */
	@Autowired
	private DummyPartnerCheckUtil dummyCheck;

	/** Kernel WebSub subscription client for hub registration. */
	@Autowired
	protected SubscriptionClient<SubscriptionChangeRequest, UnsubscriptionRequest, SubscriptionChangeResponse> subscribe;

	/** Maps auth-type status DTOs into event data maps. */
	@Autowired
	private ObjectMapper mapper;

	/** Resolves online verification (OLV) partner ids for per-partner topic registration. */
	@Autowired
	private PartnerServiceManager partnerServiceManager;

	/** In-memory cache of already-registered WebSub topics to avoid duplicate registration calls. */
	private final Set<String> registeredTopicCache = new HashSet<>();

	/**
	 * Registers all publish-side WebSub topics once at application startup.
	 * <p>
	 * Covers credential status updates, generic identity events, and per-partner IDA topics
	 * (including {@code CREDENTIAL_ISSUED}). Publish paths re-register on failure via
	 * {@link #registerTopicOnFailure(String)}.
	 * </p>
	 */
	public void registerPublishTopicsAtStartup() {
		List<String> partnerIds = partnerServiceManager.getOLVPartnerIds();
		mosipLogger.debug(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(),
				"registerPublishTopicsAtStartup",
				"Registering WebSub publish topics: publisherUrl=" + publisherURL + ", hubUrl=" + hubURL
						+ ", credentialStatusTopic=" + credentialStatusUpdateTopic + ", olvPartnerCount="
						+ partnerIds.size());
		tryRegisteringTopic(credentialStatusUpdateTopic);
		tryRegisteringTopic(IDAEventType.IDENTITY_CREATED.name());
		tryRegisteringTopic(IDAEventType.IDENTITY_UPDATED.name());
		for (String partnerId : partnerIds) {
			for (IDAEventType eventType : PARTNER_SCOPED_PUBLISH_EVENTS) {
				tryRegisteringTopic(partnerId + "/" + eventType.name());
			}
		}
		mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(),
				"registerPublishTopicsAtStartup",
				"WebSub publish topic registration finished; cachedTopicCount=" + registeredTopicCache.size());
	}

	/**
	 * Re-registers a topic after a publish failure (clears the local cache entry first).
	 * <p>
	 * Used when startup registration is stale, the hub lost state, or a new partner topic
	 * was not yet present at application start.
	 * </p>
	 *
	 * @param topic WebSub topic name to re-register
	 */
	public void registerTopicOnFailure(String topic) {
		registeredTopicCache.remove(topic);
		tryRegisteringTopic(topic);
	}

	/**
	 * Registers a WebSub topic if not already present in the local cache.
	 * <p>
	 * Treats {@link WebSubClientErrorCode#REGISTER_ERROR} as idempotent success when the
	 * topic is already registered on the hub.
	 * </p>
	 *
	 * @param topic WebSub topic name (e.g. {@code {partnerId}/CREDENTIAL_ISSUED})
	 */
	public void tryRegisteringTopic(String topic) {
		if (!registeredTopicCache.contains(topic)) {
			mosipLogger.debug(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "tryRegisteringTopic",
					"Registering WebSub topic: topic=" + topic + ", publisherUrl=" + publisherURL);
			try {
				this.registerTopic(topic);
				registeredTopicCache.add(topic);
				mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "tryRegisteringTopic",
						"WebSub topic registered: topic=" + topic);
			} catch (WebSubClientException e) {
				if (isTopicAlreadyRegistered(e)) {
					registeredTopicCache.add(topic);
					mosipLogger.debug(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(),
							"tryRegisteringTopic", "Topic already registered: topic=" + topic);
				} else {
					logTopicRegistrationFailure(topic, e);
				}
			} catch (Exception e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(),
						"tryRegisteringTopic",
						"WebSub topic registration failed: topic=" + topic + ", publisherUrl=" + publisherURL + ", error="
								+ ExceptionUtils.getStackTrace(e));
			}
		}
	}

	/**
	 * Logs topic registration failure; elevates “publisher is not authorized” to ERROR with ACL hints.
	 *
	 * @param topic topic that failed registration
	 * @param e     WebSub client exception from the hub
	 */
	private void logTopicRegistrationFailure(String topic, WebSubClientException e) {
		String message = e.getMessage() != null ? e.getMessage() : e.getErrorCode();
		if (isPublisherNotAuthorized(message)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "tryRegisteringTopic",
					"WebSub publisher not authorized: topic=" + topic + ", publisherUrl=" + publisherURL
							+ ", hubUrl=" + hubURL + ", publisherId=" + ID_REPO + ", hubResponse=" + message
							+ ". Check websub hub publisher ACL and IAM client (mosip.iam.adapter.clientid/clientsecret).");
		} else {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "tryRegisteringTopic",
					"WebSub topic registration failed: topic=" + topic + ", publisherUrl=" + publisherURL + ", error="
							+ message);
		}
	}

	/**
	 * @param message hub error message (may be {@code null})
	 * @return {@code true} when the message indicates publisher ACL denial
	 */
	private static boolean isPublisherNotAuthorized(String message) {
		return message != null && message.toLowerCase().contains("publisher is not authorized");
	}

	/**
	 * @param e WebSub client exception
	 * @return {@code true} when the hub reports the topic is already registered (idempotent success)
	 */
	private static boolean isTopicAlreadyRegistered(WebSubClientException e) {
		String message = e.getMessage();
		return message != null && message.toLowerCase().contains("already registered");
	}

	/**
	 * Builds and publishes an auth-type status update event to the given WebSub topic.
	 * <p>
	 * Generates a partner-scoped token ID, wraps auth-type statuses in
	 * {@link AuthTypeStatusEventDTO}, and publishes via {@link #publishEvent(EventModel)}.
	 * </p>
	 *
	 * @param individualId       hashed or plain individual identifier
	 * @param authTypeStatusList list of authentication type status changes
	 * @param topic              WebSub topic to publish to
	 * @param partnerId          credential partner / IDA subscriber identifier
	 */
	@SuppressWarnings("unchecked")
	public void publishAuthTypeStatusUpdateEvent(String individualId, List<AuthtypeStatus> authTypeStatusList,
			String topic, String partnerId) {
		dispatchPublish(() -> {
			AuthTypeStatusEventDTO event = new AuthTypeStatusEventDTO();
			event.setTokenId(tokenIdGenerator.generateTokenID(individualId, partnerId));
			event.setAuthTypeStatusList(authTypeStatusList);
			Map<String, String> dataMap = mapper.convertValue(event, Map.class);
			EventModel eventModel = createEventModel(IDAEventType.AUTH_TYPE_STATUS_UPDATE, null, null, null, partnerId,
					null, dataMap);
			publishEventNow(eventModel.getTopic(), eventModel);
		});
	}
	
	/**
	 * Asynchronously builds a kernel {@link EventModel} for IDA WebSub delivery.
	 * <p>
	 * Topic is formatted as {@code {partner}/{eventType}}. Populates id hash, expiry,
	 * and transaction limit in the event data payload. Runs on {@code webSubHelperExecutor}.
	 * </p>
	 *
	 * @param eventType        IDA event type (activate, deactivate, remove, etc.)
	 * @param expiryTimestamp  ID or VID expiry time; current UTC time used for deactivation
	 * @param transactionLimit optional per-VID transaction cap
	 * @param transactionId    correlation transaction identifier
	 * @param partner          partner / subscriber identifier
	 * @param idHash           salted hash of the subject ID
	 * @return {@link CompletableFuture} completing with the constructed event model
	 */
	@Async("webSubHelperExecutor")
	public CompletableFuture<EventModel> createEventModel(EventType eventType, LocalDateTime expiryTimestamp,
			Integer transactionLimit, String transactionId, String partner, String idHash) {
		EventModel eventModel = createEventModel(eventType, expiryTimestamp, transactionLimit, transactionId, partner,
				idHash, null);
		return CompletableFuture.completedFuture(eventModel);
	}
	
	/**
	 * Builds a kernel {@link EventModel} for IDA WebSub delivery.
	 * <p>
	 * Topic is formatted as {@code {partner}/{eventType}}. Populates id hash, expiry,
	 * and transaction limit in the event data payload. For {@link IDAEventType#DEACTIVATE_ID},
	 * expiry is forced to current UTC regardless of {@code expiryTimestamp}.
	 * </p>
	 *
	 * @param eventType        IDA or identity event type
	 * @param expiryTimestamp  ID or VID expiry; ignored for deactivate (current UTC used)
	 * @param transactionLimit optional per-VID transaction cap
	 * @param transactionId    correlation transaction identifier
	 * @param partner          partner / subscriber identifier (topic prefix)
	 * @param idHash           salted hash of the subject ID
	 * @param dataMap          optional extra data fields (for example auth-type status map); may be {@code null}
	 * @return fully populated event model ready to publish
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
	 * Registers a topic on the WebSub hub and publishes an event to IDA.
	 * <p>
	 * Skips publishing for fake OLV partners. Invokes {@code idaEventModelConsumer}
	 * before publishing for local side effects (e.g. status table cleanup).
	 * </p>
	 *
	 * @param model                 fully constructed kernel event model
	 * @param idaEventModelConsumer optional callback invoked before publish (may be {@code null})
	 */
	public void sendEventToIDA(EventModel model, Consumer<EventModel> idaEventModelConsumer) {
		dispatchPublish(() -> {
			if (idaEventModelConsumer != null) {
				idaEventModelConsumer.accept(model);
			}

			String partnerId = model.getTopic().split("/")[0];
			if (!dummyCheck.isDummyOLVPartner(partnerId)) {
				mosipLogger.debug(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), SEND_EVENT_TO_IDA,
						"Publishing event to topic: " + model.getTopic());
				publishEventNow(model.getTopic(), model);
			}
		});
	}

	/**
	 * Subscribes this service to the configured VID event WebSub topic.
	 * <p>
	 * Uses {@link #vidEventTopic}, {@link #vidEventUrl}, {@link #vidEventSecret}, and
	 * {@link #hubURL} from configuration. Failures are logged as warnings.
	 * </p>
	 */
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
					"Error subscribing topic: topic=" + vidEventTopic + ", hubUrl=" + hubURL + ", callbackUrl="
							+ vidEventUrl + ", error=" + e.getMessage());
		}
	}

	/**
	 * Publishes a pre-built event model using its embedded topic name.
	 *
	 * @param event event model with topic and payload
	 */
	public void publishEvent(EventModel event) {
		this.publishEvent(event.getTopic(), event);
	}
	
	/**
	 * Registers a WebSub topic on the publisher hub.
	 *
	 * @param topic WebSub topic name to register
	 */
	public void registerTopic(String topic) {
		publisher.registerTopic(topic, publisherURL);
	}
	
	/**
	 * Publishes an event payload to the specified WebSub topic.
	 * <p>
	 * On {@link WebSubClientException}, re-registers the topic and retries publish once.
	 * Honours {@link IdRepoConstants#WEBSUB_PUBLISH_ASYNC_ENABLED}.
	 * </p>
	 *
	 * @param <U>        event payload type
	 * @param eventTopic destination topic name
	 * @param eventModel   serializable event payload (JSON-encoded)
	 */
	public <U> void publishEvent(String eventTopic, U eventModel) {
		dispatchPublish(() -> publishEventNow(eventTopic, eventModel));
	}

	private void dispatchPublish(Runnable task) {
		if (publishAsyncEnabled && webSubHelperExecutor != null) {
			webSubHelperExecutor.execute(task);
			return;
		}
		if (publishAsyncEnabled && webSubHelperExecutor == null) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "dispatchPublish",
					"mosip.idrepo.websub.publish.async-enabled=true but webSubHelperExecutor missing; running sync");
		}
		task.run();
	}

	private <U> void publishEventNow(String eventTopic, U eventModel) {
		try {
			publisher.publishUpdate(eventTopic, eventModel, MediaType.APPLICATION_JSON_VALUE, null, publisherURL);
		} catch (WebSubClientException e) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "publishEvent",
					"Publish failed for topic " + eventTopic + ", re-registering: " + e.getMessage());
			registerTopicOnFailure(eventTopic);
			publisher.publishUpdate(eventTopic, eventModel, MediaType.APPLICATION_JSON_VALUE, null, publisherURL);
		}
	}
}