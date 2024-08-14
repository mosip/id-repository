package io.mosip.idrepository.identity.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;

@Configuration
public class IdentityWebsubConfig implements ApplicationListener<ApplicationReadyEvent> {
	
	@Autowired
	private IdRepoWebSubHelper websubHelper;
	


	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		websubHelper.subscribeForVidEvent();
	}

}
