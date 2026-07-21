package io.mosip.idrepository.credential.request.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.core.websub.spi.SubscriptionClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.kernel.websub.api.model.SubscriptionChangeRequest;
import io.mosip.kernel.websub.api.model.SubscriptionChangeResponse;
import io.mosip.kernel.websub.api.model.UnsubscriptionRequest;

/**
 * WebSub integration for the credential-request module.
 * <p>
 * Subscribes the credential-request callback so identity-service status updates
 * are delivered to the merged service. Publish topic registration is handled at startup
 * by {@link io.mosip.idrepository.core.helper.IdRepoWebSubHelper#registerPublishTopicsAtStartup()}.
 * Invoked at startup by {@link io.mosip.idrepository.credential.request.init.SubscribeEvent}
 * and on a schedule by {@link io.mosip.idrepository.credential.request.init.CredentialInstializer}.
 * </p>
 */
@Component
public class WebSubSubscriptionHelper {

	/**
	 * Kernel WebSub subscription client for hub subscribe/unsubscribe calls.
	 */
	@Autowired
	SubscriptionClient<SubscriptionChangeRequest, UnsubscriptionRequest, SubscriptionChangeResponse> sb;

	/**
	 * WebSub hub base URL for subscription requests.
	 * Property: {@link IdRepoConstants#WEB_SUB_HUB_URL}.
	 */
	@Value("${" + IdRepoConstants.WEB_SUB_HUB_URL + "}")
	private String webSubHubUrl;

	/**
	 * Shared secret presented to the hub when subscribing.
	 * Property: {@link IdRepoConstants#CREDREQ_WEBSUB_SECRET}.
	 */
	@Value("${" + IdRepoConstants.CREDREQ_WEBSUB_SECRET + "}")
	private String webSubSecret;

	/**
	 * Callback URL the hub invokes on {@code CREDENTIAL_STATUS_UPDATE} events.
	 * Property: {@link IdRepoConstants#CREDREQ_CALLBACK_URL}.
	 */
	@Value("${" + IdRepoConstants.CREDREQ_CALLBACK_URL + "}")
	private String callBackUrl;

	/**
	 * Partner WebSub publish hub URL used for topic registration.
	 * Property: {@link IdRepoConstants#WEB_SUB_PUBLISH_URL}.
	 */
	@Value("${" + IdRepoConstants.WEB_SUB_PUBLISH_URL + "}")
	private String partnerhuburl;

	/**
	 * Kernel WebSub publisher client for topic registration.
	 */
	@Autowired
	private PublisherClient<String, EventModel, HttpHeaders> pb;

	private static final String WEBSUBSUBSCRIPTIONHEPLER = "WebSubSubscriptionHelper";

	private static final String INITSUBSCRIPTION = "initSubsriptions";

	private static final Logger LOGGER = IdRepoLogger.getLogger(WebSubSubscriptionHelper.class);

	/**
	 * Subscribes the credential-request callback to {@code CREDENTIAL_STATUS_UPDATE}.
	 * <p>
	 * Logs and swallows {@link WebSubClientException} on failure so startup is not aborted;
	 * {@link io.mosip.idrepository.credential.request.init.CredentialInstializer} may retry later.
	 * </p>
	 */
	public void initSubsriptions() {
		LOGGER.info(IdRepoSecurityManager.getUser(), WEBSUBSUBSCRIPTIONHEPLER, INITSUBSCRIPTION,
				"Initializing subscribptions..");
		subscribeForPrintServiceEvents();
	}

	private void subscribeForPrintServiceEvents() {
		try {
			SubscriptionChangeRequest subscriptionRequest = new SubscriptionChangeRequest();
			subscriptionRequest.setCallbackURL(callBackUrl);
			subscriptionRequest.setHubURL(webSubHubUrl);
			subscriptionRequest.setSecret(webSubSecret);
			subscriptionRequest.setTopic("CREDENTIAL_STATUS_UPDATE");
			sb.subscribe(subscriptionRequest);
		} catch (WebSubClientException e) {
			LOGGER.info(IdRepoSecurityManager.getUser(), WEBSUBSUBSCRIPTIONHEPLER, INITSUBSCRIPTION,
					"websub subscription error");
		}
	}
}
