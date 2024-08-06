package io.mosip.credentialstore.api.config;
import static io.mosip.idrepository.core.constant.IdRepoConstants.PARTNER_CACHE_UPDATE_DEFAULT_INTERVAL;
import static io.mosip.idrepository.core.constant.IdRepoConstants.PARTNER_CACHE_UPDATE_INTERVAL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import io.mosip.credentialstore.util.PolicyUtil;

/**
 * The Class PartnerCacheUpdatingSchedulerConfig.
 * 
 * @author Loganathan S
 */
@Configuration
@EnableCaching
public class PartnerCacheUpdatingSchedulerConfig {
	
	/** The partner service manager. */
	@Autowired
	private PolicyUtil partnerServiceManager;
	
	/**
	 * Clear partner data cache.
	 */
	@Scheduled(initialDelayString = "${" + PARTNER_CACHE_UPDATE_INTERVAL + ":" + PARTNER_CACHE_UPDATE_DEFAULT_INTERVAL + "}", fixedDelayString = "${" + PARTNER_CACHE_UPDATE_INTERVAL + ":" + PARTNER_CACHE_UPDATE_DEFAULT_INTERVAL + "}")
	public void clearPartnerDataCache() {
		partnerServiceManager.clearDataSharePoliciesCache();
		partnerServiceManager.clearPartnerExtractorFormatsCache();
	}
	
}
