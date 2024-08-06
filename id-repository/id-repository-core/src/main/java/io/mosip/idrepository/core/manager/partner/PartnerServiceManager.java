package io.mosip.idrepository.core.manager.partner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

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
 * The Class PartnerServiceManager.
 * 
 * @author Loganathan S
 */
@Component
public class PartnerServiceManager {

	/** The Constant GET_OLV_PARTNER_IDS. */
	private static final String GET_OLV_PARTNER_IDS = "getOLVPartnerIds";


	/** The Constant mosipLogger. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(PartnerServiceManager.class);
	

	/** The Constant RESPONSE. */
	private static final String RESPONSE = "response";

	/** The Constant PARTNER_ACTIVE_STATUS. */
	private static final String PARTNER_ACTIVE_STATUS = "Active";
	

	/** The rest helper. */
	private RestHelper restHelper;

	/** The rest builder. */
	@Autowired
	private RestRequestBuilder restBuilder;
	
	/** The dummy check. */
	@Autowired
	private DummyPartnerCheckUtil dummyCheck;
	
	/** The ctx. */
	@Autowired
	private ApplicationContext ctx;
	
	/**
	 * Inits the.
	 */
	@PostConstruct
	public void init() {
		if (Objects.isNull(restHelper))
			this.restHelper = ctx.getBean(RestHelper.class);
	}

	
	/**
	 * Gets the partner ids.
	 *
	 * @return the partner ids
	 */
	@SuppressWarnings("unchecked")
	@Cacheable(cacheNames = IdRepoConstants.ONLINE_VERIFICATION_PARTNERS_CACHE)
	public List<String> getOLVPartnerIds() {
		mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "clearMasterDataTitlesCache",
				"Caching " + IdRepoConstants.ONLINE_VERIFICATION_PARTNERS_CACHE);
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
}
