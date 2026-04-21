package io.mosip.idrepository.identity.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.manager.CredentialStatusManager;

@Configuration
public class IdentityScheduleConfig {
	
	@Autowired
	private CredentialStatusManager credStatusManager;
	
	@Scheduled(fixedDelayString = "${" + IdRepoConstants.CREDENTIAL_STATUS_JOB_DELAY + ":1000}")
	public void credentialStatusHandlerJob() {
		credStatusManager.triggerEventNotifications();
	}

}
