package io.mosip.credential.request.generator.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import io.mosip.credential.request.generator.batch.config.CredentialItemProcessor;
import io.mosip.idrepository.core.dto.EventModel;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.core.websub.spi.SubscriptionClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.kernel.websub.api.model.SubscriptionChangeRequest;
import io.mosip.kernel.websub.api.model.SubscriptionChangeResponse;
import io.mosip.kernel.websub.api.model.UnsubscriptionRequest;

@Component
public class SubscribeEvent implements ApplicationListener<ApplicationReadyEvent> {

	@Autowired
	SubscriptionClient<SubscriptionChangeRequest, UnsubscriptionRequest, SubscriptionChangeResponse> sb;

	@Value("${WEBSUBSUBSCRIBEURL}")
	private String webSubHubUrl;

	@Value("${WEBSUBSECRET}")
	private String webSubSecret;

	@Value("${CALLBACKURL}")
	private String callBackUrl;

	/** The config server file storage URL. */
	@Value("${mosip.partnerhuburl}")
	private String partnerhuburl;

	@Autowired
	private PublisherClient<String, EventModel, HttpHeaders> pb;

	/** The Constant BIOMETRICS. */
	private static final String ONAPPLICATIONEVENT = "onApplicationEvent";

	/** The Constant ID_REPO_SERVICE_IMPL. */
	private static final String SUBSCIRBEEVENT = "SubscribeEvent";

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialItemProcessor.class);

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		registerTopic();
		try {
			SubscriptionChangeRequest subscriptionRequest = new SubscriptionChangeRequest();
			subscriptionRequest.setCallbackURL(callBackUrl);
			subscriptionRequest.setHubURL(webSubHubUrl);
			subscriptionRequest.setSecret(webSubSecret);
			subscriptionRequest.setTopic("CREDENTIAL_STATUS_UPDATE");
			sb.subscribe(subscriptionRequest);
		} catch (WebSubClientException e) {
			LOGGER.info(IdRepoSecurityManager.getUser(), SUBSCIRBEEVENT, ONAPPLICATIONEVENT,
					"websub subscription error");
		}

	}

	private void registerTopic() {
		try {
			pb.registerTopic("CREDENTIAL_STATUS_UPDATE", partnerhuburl);
		} catch (WebSubClientException e) {
			LOGGER.info(IdRepoSecurityManager.getUser(), SUBSCIRBEEVENT, ONAPPLICATIONEVENT,
					"topic already registered");
		}

	}
}
