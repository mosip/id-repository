package io.mosip.idrepository.identity.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;

/**
 * Registers WebSub subscriptions and publish topics when the application is ready.
 * <p>
 * Work is submitted to {@code webSubHelperExecutor} so hub registration and subscription
 * do not block the main application thread during startup.
 * </p>
 *
 * @see io.mosip.idrepository.core.helper.IdRepoWebSubHelper
 * @see IdRepoConfig
 */
@Configuration
public class IdentityWebsubConfig implements ApplicationListener<ApplicationReadyEvent> {

	@Autowired
	private IdRepoWebSubHelper websubHelper;

	@Autowired
	@Qualifier("webSubHelperExecutor")
	private Executor webSubHelperExecutor;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		webSubHelperExecutor.execute(() -> {
			websubHelper.registerPublishTopicsAtStartup();
			websubHelper.subscribeForVidEvent();
		});
	}

}
