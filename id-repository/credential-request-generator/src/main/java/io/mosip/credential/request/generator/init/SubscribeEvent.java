package io.mosip.credential.request.generator.init;

import java.util.Date;

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
public class SubscribeEvent implements ApplicationListener<ApplicationReadyEvent> {

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	@Value("${subscription-delay-secs:60000}")
	private int taskSubsctiptionDelay;

	@Autowired
	private WebSubSubscriptionHelper webSubSubscriptionHelper;

	/** The Constant BIOMETRICS. */
	private static final String ONAPPLICATIONEVENT = "onApplicationEvent";

	/** The Constant ID_REPO_SERVICE_IMPL. */
	private static final String SUBSCIRBEEVENT = "SubscribeEvent";

	private static final Logger LOGGER = IdRepoLogger.getLogger(SubscribeEvent.class);
	
	private static boolean isSubscriptionStarted = false;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		LOGGER.info(IdRepoSecurityManager.getUser(), SUBSCIRBEEVENT, ONAPPLICATIONEVENT,
				"Scheduling event subscriptions after (milliseconds): " + taskSubsctiptionDelay);

		taskScheduler.schedule(() -> {
			webSubSubscriptionHelper.registerTopic();
			initSubsriptions();
		}, new Date(System.currentTimeMillis() + taskSubsctiptionDelay));

	}
	
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
