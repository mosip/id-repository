package io.mosip.idrepository.credential.request.init;

import java.util.Date;

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
 * Application-startup listener that performs the initial WebSub subscription
 * for credential-status callbacks.
 * <p>
 * After a configurable delay ({@link IdRepoConstants#CREDENTIAL_SUBSCRIPTION_DELAY_SECS}),
 * subscribes the credential-request callback URL. Publish topic registration is handled at
 * startup by {@link io.mosip.idrepository.core.helper.IdRepoWebSubHelper#registerPublishTopicsAtStartup()}.
 * Complements {@link CredentialInstializer}, which handles periodic re-subscription.
 * </p>
 *
 * @see WebSubSubscriptionHelper
 */
@Component
public class SubscribeEvent implements ApplicationListener<ApplicationReadyEvent> {

	/**
	 * Scheduler for deferred one-shot subscription on startup.
	 */
	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	/**
	 * Milliseconds to wait after application ready before first subscribe attempt.
	 * Property: {@link IdRepoConstants#CREDENTIAL_SUBSCRIPTION_DELAY_SECS} (default {@code 60000}).
	 */
	@Value("${" + IdRepoConstants.CREDENTIAL_SUBSCRIPTION_DELAY_SECS + ":60000}")
	private int taskSubsctiptionDelay;

	/**
	 * Performs WebSub topic registration and hub subscription.
	 */
	@Autowired
	private WebSubSubscriptionHelper webSubSubscriptionHelper;

	private static final String ONAPPLICATIONEVENT = "onApplicationEvent";

	private static final String SUBSCIRBEEVENT = "SubscribeEvent";

	private static final Logger LOGGER = IdRepoLogger.getLogger(SubscribeEvent.class);

	/** Guards against duplicate one-shot subscription scheduling. */
	private static boolean isSubscriptionStarted = false;

	/**
	 * On application ready, schedules deferred topic registration and subscription.
	 *
	 * @param event Spring Boot application-ready event
	 */
	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		LOGGER.info(IdRepoSecurityManager.getUser(), SUBSCIRBEEVENT, ONAPPLICATIONEVENT,
				"Scheduling event subscriptions after (milliseconds): " + taskSubsctiptionDelay);

		taskScheduler.schedule(this::initSubsriptions, new Date(System.currentTimeMillis() + taskSubsctiptionDelay));
	}

	/**
	 * Schedules a one-shot subscription task after {@link #taskSubsctiptionDelay} milliseconds.
	 *
	 * @return {@link SubscriptionMessage#SUCCESS} when scheduled, or
	 *         {@link SubscriptionMessage#ALREADY_SUBSCRIBED} if already started
	 */
	public String scheduleSubscription() {
		if (!isSubscriptionStarted) {
			taskScheduler.schedule(this::initSubsriptions, new Date(System.currentTimeMillis() + taskSubsctiptionDelay));
			isSubscriptionStarted = true;
			return SubscriptionMessage.SUCCESS;
		} else
			return SubscriptionMessage.ALREADY_SUBSCRIBED;
	}

	private void initSubsriptions() {
		LOGGER.info(IdRepoSecurityManager.getUser(), SUBSCIRBEEVENT, ONAPPLICATIONEVENT,
				"Initializing subscribptions..");

		webSubSubscriptionHelper.initSubsriptions();
	}
}
