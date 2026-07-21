package io.mosip.idrepository.credential.request.init;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.credential.request.constant.SubscriptionMessage;
import io.mosip.idrepository.credential.request.integration.WebSubSubscriptionHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Application-startup listener that periodically re-subscribes to the
 * {@code CREDENTIAL_STATUS_UPDATE} WebSub topic.
 * <p>
 * WebSub subscriptions can expire or become stale in long-running deployments.
 * When {@code mosip.credential.subscription.retry.count} and re-subscription delay
 * are configured, this component schedules fixed-rate retries via
 * {@link #scheduleRetrySubscriptions()} so credential-status callbacks remain active.
 * </p>
 *
 * @see SubscribeEvent
 * @see WebSubSubscriptionHelper
 */
@Component
public class CredentialInstializer implements ApplicationListener<ApplicationReadyEvent> {

	/**
	 * Maximum number of subscription attempts per scheduled retry cycle.
	 * Property: {@link IdRepoConstants#CREDENTIAL_SUBSCRIPTION_RETRY_COUNT} (default {@code 3}).
	 */
	@Value("${" + IdRepoConstants.CREDENTIAL_SUBSCRIPTION_RETRY_COUNT + ":3}")
	private int retryCount;

	/**
	 * Interval in seconds between periodic re-subscription runs.
	 * Property: {@link IdRepoConstants#CREDENTIAL_RESUBSCRIPTION_DELAY_SECS} (default {@code 0}, disabled).
	 */
	@Value("${" + IdRepoConstants.CREDENTIAL_RESUBSCRIPTION_DELAY_SECS + ":0}")
	private int reSubscriptionDelaySecs;

	/**
	 * Delegates WebSub subscribe and topic registration to the integration helper.
	 */
	@Autowired
	private WebSubSubscriptionHelper webSubSubscriptionHelper;

	/**
	 * Scheduler used for fixed-rate re-subscription tasks.
	 */
	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	private static final String ONAPPLICATIONEVENT = "onApplicationEvent";

	private static final String CREDENTIALINSTIALIZER = "CredentialInstializer";

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialInstializer.class);

	/** Guards against registering duplicate fixed-rate subscription tasks. */
	private static boolean isSubscriptionStarted = false;

	/**
	 * On application ready, starts periodic re-subscription when delay is configured.
	 *
	 * @param event Spring Boot application-ready event
	 */
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

	/**
	 * Schedules a fixed-rate task that retries WebSub subscription up to {@link #retryCount} times per tick.
	 *
	 * @return {@link SubscriptionMessage#SUCCESS} when scheduling starts, or
	 *         {@link SubscriptionMessage#ALREADY_SUBSCRIBED} if already running
	 */
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
