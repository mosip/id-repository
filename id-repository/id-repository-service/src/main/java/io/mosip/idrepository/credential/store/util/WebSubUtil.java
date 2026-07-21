package io.mosip.idrepository.credential.store.util;

import java.io.IOException;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.common.constant.LoggerFileConstant;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;

/**
 * Publishes credential-issued events to partner WebSub hubs.
 * <p>
 * Partner-specific topics follow the pattern {@code {partnerId}/CREDENTIAL_ISSUED}.
 * Topics are registered at application startup; on publish failure the topic is
 * re-registered via {@link #registerTopic(String, String)} before a single retry.
 * Further failures are retried per {@code mosip.credential.service.retry.*} properties.
 * </p>
 * <p>
 * Dispatch mode is controlled by {@link IdRepoConstants#WEBSUB_PUBLISH_ASYNC_ENABLED}
 * ({@code mosip.idrepo.websub.publish.async-enabled}). When async, publish runs on
 * {@code webSubHelperExecutor} and failures are logged only (credential issuance still
 * completes). When sync, exceptions propagate to the caller.
 * </p>
 */
@Component
public class WebSubUtil {

	/**
	 * Kernel WebSub publisher for topic registration and event publish.
	 */
	@Autowired
	private PublisherClient<String, EventModel, HttpHeaders> pb;

	/**
	 * Partner WebSub publish hub base URL.
	 * Property: {@link IdRepoConstants#WEB_SUB_PUBLISH_URL}.
	 */
	@Value("${" + IdRepoConstants.WEB_SUB_PUBLISH_URL + "}")
	private String partnerhuburl;

	/**
	 * When {@code true}, credential WebSub publish runs off the caller thread.
	 * Property: {@link IdRepoConstants#WEBSUB_PUBLISH_ASYNC_ENABLED}.
	 */
	@Value("${" + IdRepoConstants.WEBSUB_PUBLISH_ASYNC_ENABLED + ":true}")
	private boolean asyncEnabled;

	/** Executor for async WebSub publish. Optional for unit tests. */
	@Autowired(required = false)
	@Qualifier("webSubHelperExecutor")
	private Executor webSubHelperExecutor;

	private static final Logger LOGGER = IdRepoLogger.getLogger(WebSubUtil.class);

	/**
	 * Registers a WebSub topic with the hub.
	 * <p>
	 * Idempotent — duplicate registration is logged and ignored.
	 * </p>
	 *
	 * @param topic     partner topic to register
	 * @param requestId credential request id for audit logging
	 */
	public void registerTopic(String topic, String requestId) {
		try {
			pb.registerTopic(topic, partnerhuburl);
		} catch (WebSubClientException e) {
			if (isTopicAlreadyRegistered(e)) {
				LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"Topic already registered: " + topic);
			} else {
				LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"Topic registration failed: " + e.getMessage());
				throw e;
			}
		}
	}

	private static boolean isTopicAlreadyRegistered(WebSubClientException e) {
		String message = e.getMessage();
		return message != null && message.toLowerCase().contains("already registered");
	}

	/**
	 * Publishes a successful credential-issued event to the partner topic.
	 * <p>
	 * On {@link WebSubClientException}, re-registers the topic and retries publish once.
	 * Retries on {@link WebSubClientException} and {@link IOException} using
	 * {@code mosip.credential.service.retry.maxAttempts} and {@code maxDelay} when running sync
	 * (or inside the async worker).
	 * </p>
	 *
	 * @param topic      partner-specific WebSub topic (e.g. {@code partnerId/CREDENTIAL_ISSUED})
	 * @param eventModel event payload including transaction id and credential metadata
	 * @throws WebSubClientException when sync mode and the hub rejects the publish after retries
	 * @throws IOException           when sync mode and transport fails after retries
	 */
	public void publishSuccess(String topic, EventModel eventModel) throws WebSubClientException, IOException {
		if (asyncEnabled && webSubHelperExecutor != null) {
			webSubHelperExecutor.execute(() -> {
				try {
					publishSuccessNow(topic, eventModel);
				} catch (Exception e) {
					String requestId = eventModel.getEvent() != null ? eventModel.getEvent().getTransactionId() : "";
					LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
							"Async WebSub publish failed for topic=" + topic + ": " + e.getMessage());
				}
			});
			return;
		}
		if (asyncEnabled && webSubHelperExecutor == null) {
			LOGGER.warn(IdRepoSecurityManager.getUser(), LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.SESSIONID.toString(),
					"mosip.idrepo.websub.publish.async-enabled=true but webSubHelperExecutor missing; running sync");
		}
		publishSuccessNow(topic, eventModel);
	}

	/**
	 * Synchronous publish with Spring Retry. Invoked directly in sync mode, or from the async worker.
	 *
	 * @param topic      partner topic
	 * @param eventModel event payload
	 * @throws WebSubClientException when hub rejects publish after retries
	 * @throws IOException           on transport failure after retries
	 */
	@Retryable(value = { WebSubClientException.class,
			IOException.class }, maxAttemptsExpression = "${mosip.credential.service.retry.maxAttempts}", backoff = @Backoff(delayExpression = "${mosip.credential.service.retry.maxDelay}"))
	public void publishSuccessNow(String topic, EventModel eventModel) throws WebSubClientException, IOException {
		String requestId = eventModel.getEvent().getTransactionId();
		try {
			doPublish(topic, eventModel);
		} catch (WebSubClientException e) {
			LOGGER.warn(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Publish failed, re-registering topic: " + topic);
			registerTopic(topic, requestId);
			doPublish(topic, eventModel);
		}
		LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
				requestId,
				"Publish the update successfully");
	}

	private void doPublish(String topic, EventModel eventModel) throws WebSubClientException, IOException {
		HttpHeaders httpHeaders = new HttpHeaders();
		pb.publishUpdate(topic, eventModel, MediaType.APPLICATION_JSON_VALUE, httpHeaders, partnerhuburl);
	}

}
