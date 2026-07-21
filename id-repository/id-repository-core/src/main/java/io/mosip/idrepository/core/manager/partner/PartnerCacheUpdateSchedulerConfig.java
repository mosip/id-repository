package io.mosip.idrepository.core.manager.partner;

import static io.mosip.idrepository.core.constant.IdRepoConstants.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduled eviction of partner OLV (Online Verification) policy cache.
 * <p>
 * Imported via {@link io.mosip.idrepository.core.jobs.IdRepoSchedulerConfiguration}.
 * </p>
 *
 * @author Loganathan S
 * @see io.mosip.idrepository.core.jobs.IdRepoSchedulerConfiguration
 */
@Configuration
@EnableCaching
public class PartnerCacheUpdateSchedulerConfig {
	
	/** Partner service used to clear cached partner policies. */
	@Autowired
	private PartnerServiceManager partnerServiceManager;
	
	/**
	 * Clears the partner OLV cache on a fixed delay from
	 * {@code mosip.idrepo.cache.update.interval}.
	 */
	@Scheduled(initialDelayString = "${" + IDREPO_CACHE_UPDATE_INTERVAL + ":" + CACHE_UPDATE_DEFAULT_INTERVAL + "}", fixedDelayString = "${" + IDREPO_CACHE_UPDATE_INTERVAL + ":" + CACHE_UPDATE_DEFAULT_INTERVAL + "}")
	public void clearPartnerDataCache() {
		partnerServiceManager.clearOLVPartnersCache();
	}
	
}