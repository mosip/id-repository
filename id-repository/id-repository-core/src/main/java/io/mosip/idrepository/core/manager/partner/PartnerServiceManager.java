package io.mosip.idrepository.core.manager.partner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Manages Online Verification (OLV) partner ID resolution from Partner Management Service.
 * <p>
 * Fetches active credential partners from PMS via {@link RestHelper}, filters out dummy/test
 * partners, and caches results in the {@code Online_Verification_Partners} cache region.
 * Falls back to the configured dummy OLV partner when PMS returns no active partners.
 * </p>
 *
 * @see io.mosip.idrepository.manager.CredentialServiceManager
 * @see PartnerCacheUpdateSchedulerConfig
 * @see RestServicesConstants#PARTNER_SERVICE
 * @see DummyPartnerCheckUtil
 *
 * @author Loganathan S
 */
@Component
public class PartnerServiceManager {

	/** Spring cache region name for OLV partner ID list. */
	private static final String ONLINE_VERIFICATION_PARTNERS = IdRepoConstants.CACHE_ONLINE_VERIFICATION_PARTNERS;

	/** Structured log method name for partner ID retrieval. */
	private static final String GET_OLV_PARTNER_IDS = "getOLVPartnerIds";

	/** Structured logger for partner service operations. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(PartnerServiceManager.class);

	/** JSON response wrapper key for PMS partner list responses. */
	private static final String RESPONSE = "response";

	/** PMS partner status value indicating an active credential partner. */
	private static final String PARTNER_ACTIVE_STATUS = "Active";

	/** REST client for PMS calls; resolved in {@link #init()} if null. */
	private RestHelper restHelper;

	/** REST request builder for PMS service URL resolution. */
	@Autowired
	private RestRequestBuilder restBuilder;

	/** Utility to identify and exclude dummy/test OLV partners. */
	@Autowired
	private DummyPartnerCheckUtil dummyCheck;

	@Autowired
	private ApplicationContext ctx;

	/**
	 * Resolves the {@link RestHelper} bean from the application context when not constructor-injected.
	 */
	@PostConstruct
	public void init() {
		if (Objects.isNull(restHelper))
			this.restHelper = ctx.getBean(RestHelper.class);
	}

	/**
	 * Returns the list of active Online Verification partner IDs from PMS.
	 * <p>
	 * Results are cached in {@code Online_Verification_Partners}. Active partners are
	 * filtered by status {@code Active} and exclude dummy OLV partners. Returns a
	 * singleton list containing the dummy partner ID when PMS returns no partners.
	 * </p>
	 *
	 * @return non-empty list of partner IDs eligible for credential issuance
	 * @see #clearOLVPartnersCache()
	 */
	@SuppressWarnings("unchecked")
	@Cacheable(cacheNames = ONLINE_VERIFICATION_PARTNERS)
	public List<String> getOLVPartnerIds() {
		mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "clearMasterDataTitlesCache",
				"Caching " + ONLINE_VERIFICATION_PARTNERS);
		List<String> partners = Collections.emptyList();
		try {
			Map<String, Object> responseWrapperMap = restHelper
					.requestSync(restBuilder.buildRequest(RestServicesConstants.PARTNER_SERVICE, null, Map.class));
			Object response = responseWrapperMap.get(RESPONSE);
			if (response instanceof Map) {
				Object partnersObj = ((Map<String, ?>) response).get("partners");
				if (partnersObj instanceof List) {
					List<Map<String, Object>> partnersList = (List<Map<String, Object>>) partnersObj;
					partners = partnersList.stream()
							.filter(partner -> PARTNER_ACTIVE_STATUS.equalsIgnoreCase((String) partner.get("status")))
							.map(partner -> (String) partner.get("partnerID"))
							.filter(Predicate.not(dummyCheck::isDummyOLVPartner))
							.collect(Collectors.toList());
				}
			}
		} catch (RestServiceException | IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), GET_OLV_PARTNER_IDS,
					e.getMessage());
		}

		mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), GET_OLV_PARTNER_IDS,
				"PARTNERS_IDENTIFIED: " + partners.size());

		if (partners.isEmpty()) {
			return List.of(dummyCheck.getDummyOLVPartnerId());
		} else {
			return partners;
		}
	}

	/**
	 * Evicts the cached OLV partner ID list, forcing a fresh PMS fetch on next access.
	 * <p>
	 * Invoked by {@link PartnerCacheUpdateSchedulerConfig} on a configurable schedule.
	 * </p>
	 *
	 * @see #getOLVPartnerIds()
	 */
	@CacheEvict(value=ONLINE_VERIFICATION_PARTNERS)
	public void clearOLVPartnersCache() {
		mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "clearOLVPartnersCache",
				ONLINE_VERIFICATION_PARTNERS + " cache cleared");
	}
}