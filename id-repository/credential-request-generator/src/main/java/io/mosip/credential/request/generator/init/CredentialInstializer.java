package io.mosip.credential.request.generator.init;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import io.mosip.credential.request.generator.constants.SubscriptionMessage;
import io.mosip.credential.request.generator.integration.WebSubSubscriptionHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class CredentialInstializer implements ApplicationListener<ApplicationReadyEvent> {
	@Value("${retry-count:3}")
	private int retryCount;

	@Value("${resubscription-delay-secs:0}")
	private int reSubscriptionDelaySecs;

	@Autowired
	private WebSubSubscriptionHelper webSubSubscriptionHelper;

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	/** The Constant BIOMETRICS. */
	private static final String ONAPPLICATIONEVENT = "onApplicationEvent";

	/** The Constant ID_REPO_SERVICE_IMPL. */
	private static final String CREDENTIALINSTIALIZER = "CredentialInstializer";

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialInstializer.class);
	
	private static boolean isSubscriptionStarted = false;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		if (reSubscriptionDelaySecs > 0) {
		LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIALINSTIALIZER, ONAPPLICATIONEVENT,
				"Work around for web-sub notification issue after some time.");

		scheduleRetrySubscriptions();
		}
		else {
			LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIALINSTIALIZER, ONAPPLICATIONEVENT,

					"Scheduling for re-subscription is Disabled as the re-subsctription delay value is: "
							+ reSubscriptionDelaySecs);

		}
	}

	public String scheduleRetrySubscriptions() {
		LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIALINSTIALIZER, ONAPPLICATIONEVENT,
				"Scheduling re-subscription every " + reSubscriptionDelaySecs + " seconds");

		if (!isSubscriptionStarted) {
			taskScheduler.scheduleAtFixedRate(this::retrySubscriptions, Instant.now().plusSeconds(reSubscriptionDelaySecs),
					Duration.ofSeconds(reSubscriptionDelaySecs));
			isSubscriptionStarted = true;
			return SubscriptionMessage.SUCCESS;
		}
		else {
			LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIALINSTIALIZER, ONAPPLICATIONEVENT,
					"Already instantiated");
			return SubscriptionMessage.ALREADY_SUBSCRIBED;
		}
	}

	private void retrySubscriptions() {
		// Call Init Subscriptions for the count until no error in the subscription
		for (int i = 0; i <= retryCount; i++) {
			if (initSubsriptions()) {
				return;
			}
		}
	}

	private boolean initSubsriptions() {
		try {
			LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIALINSTIALIZER, ONAPPLICATIONEVENT,
					"Initializing subscribptions..");
			webSubSubscriptionHelper.initSubsriptions();

			return true;
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIALINSTIALIZER, ONAPPLICATIONEVENT,
					"Initializing subscribptions failed: " + e.getMessage());

			return false;
		}
	}
}
