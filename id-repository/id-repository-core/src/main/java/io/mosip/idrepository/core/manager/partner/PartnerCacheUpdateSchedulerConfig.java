package io.mosip.idrepository.core.manager.partner;
import static io.mosip.idrepository.core.constant.IdRepoConstants.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * The Class PartnerCacheUpdateSchedulerConfig.
 * 
 * @author Loganathan S
 */
@Configuration
@EnableCaching
public class PartnerCacheUpdateSchedulerConfig {
	
	/** The partner service manager. */
	@Autowired
	private PartnerServiceManager partnerServiceManager;
	
	/**
	 * Clear partner data cache.
	 */
	@Scheduled(initialDelayString = "${" + IDREPO_CACHE_UPDATE_INTERVAL + ":" + CACHE_UPDATE_DEFAULT_INTERVAL + "}", fixedDelayString = "${" + IDREPO_CACHE_UPDATE_INTERVAL + ":" + CACHE_UPDATE_DEFAULT_INTERVAL + "}")
	public void clearPartnerDataCache() {
		partnerServiceManager.clearOLVPartnersCache();
	}
	
}
