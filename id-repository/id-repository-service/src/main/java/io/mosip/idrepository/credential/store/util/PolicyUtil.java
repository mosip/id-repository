package io.mosip.idrepository.credential.store.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.credential.store.constant.ApiName;
import io.mosip.idrepository.common.constant.LoggerFileConstant;
import io.mosip.idrepository.credential.store.dto.PartnerCredentialTypePolicyDto;
import io.mosip.idrepository.credential.store.dto.PartnerExtractorResponse;
import io.mosip.idrepository.credential.store.dto.PartnerExtractorResponseDto;
import io.mosip.idrepository.credential.store.dto.PolicyManagerResponseDto;
import io.mosip.idrepository.credential.store.exception.ApiNotAccessibleException;
import io.mosip.idrepository.credential.store.exception.PartnerException;
import io.mosip.idrepository.credential.store.exception.PolicyException;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;


/**
 * Partner Management Service (PMS) policy client with Spring Cache and in-memory fallback.
 * <p>
 * Fetches credential-type policies and biometric extractor definitions used during issuance
 * by {@link io.mosip.idrepository.credential.store.service.impl.CredentialStoreServiceImpl}.
 * Cache regions are evicted by partner policy refresh schedulers in the consolidated service.
 * </p>
 *
 * @see io.mosip.idrepository.credential.store.dto.PartnerCredentialTypePolicyDto
 */
@Component
public class PolicyUtil {

	private static final Logger LOGGER = IdRepoLogger.getLogger(PolicyUtil.class);

	/** Outbound REST client for PMS policy APIs. */
	@Autowired
	private CredentialStoreRestUtil restUtil;

	/** Deserializes PMS policy JSON responses. */
	@Autowired
	private ObjectMapper mapper;

	/** Shared credential-store helpers (unused directly; kept for parity with legacy wiring). */
	@Autowired
	private Utilities utilities;

	/** Clears Spring cache regions on partner policy refresh. */
	@Autowired
	private CacheManager cacheManager;

	/**
	 * Fetches partner credential-type policy from PMS (cached).
	 *
	 * @param credentialType credential type code (IdAuth, QRCode, etc.)
	 * @param subscriberId   partner id
	 * @param requestId      correlation id for logging
	 * @return parsed policy including sharable attributes and data-share rules
	 * @throws PolicyException           if PMS returns business errors
	 * @throws ApiNotAccessibleException if HTTP call fails
	 */
	@Cacheable(cacheNames = IdRepoConstants.CACHE_DATASHARE_POLICIES, key = "{ #credentialType, #subscriberId }")
	public PartnerCredentialTypePolicyDto getPolicyDetail(String credentialType, String subscriberId, String requestId)
			throws PolicyException, ApiNotAccessibleException {

		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					requestId,
					"started fetching the policy data");
			Map<String, String> pathsegments = new HashMap<>();
			pathsegments.put("partnerId", subscriberId);
			pathsegments.put("credentialType", credentialType);
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"PMS PARTNER_POLICY GET partnerId=" + subscriberId + ", credentialType=" + credentialType
							+ " — uses selfTokenRestTemplate (Bearer added by kernel-auth interceptor, not PolicyUtil)");
			String responseString = restUtil.getApi(ApiName.PARTNER_POLICY, pathsegments, String.class);

			PolicyManagerResponseDto responseObject = mapper.readValue(responseString,
					PolicyManagerResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new PolicyException(error.getMessage());
			}
			PartnerCredentialTypePolicyDto policyResponseDto = responseObject != null ? responseObject.getResponse() : null;
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					requestId,
					"Fetched policy details successfully");
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"ended fetching the policy data");
			return policyResponseDto;

		} catch (IOException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"error with error message" + ExceptionUtils.getStackTrace(e));
			throw new PolicyException(e);
		} catch (Exception e) {
			if (e.getCause() instanceof HttpClientErrorException httpClientException
					&& httpClientException.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
				LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"PMS returned 403 KER-ATH-403 — partnermanager rejected the service-account Bearer token "
								+ "(selfTokenRestTemplate / mosip-creser-client). Check IAM: client secret, "
								+ "auth.server.admin.allowed.audience on partnermanager, CREDENTIAL_ISSUANCE role, "
								+ "and PARTNER_POLICY host (prefer api-internal.dev2 for local). Body: "
								+ httpClientException.getResponseBodyAsString());
			}
			else {
				LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"error with error message" + ExceptionUtils.getStackTrace(e));
			}
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new PolicyException(e);
			}

		}

	}


	/**
	 * Fetches partner biometric extractor policy from PMS (cached).
	 *
	 * @param policyId     PMS policy id
	 * @param subscriberId partner id
	 * @param requestId    correlation id for logging
	 * @return extractor configuration or {@code null} when PMS returns {@code PMS_PRT_064}
	 * @throws ApiNotAccessibleException if HTTP call fails
	 * @throws PartnerException        if PMS returns other partner errors
	 */
	@Cacheable(cacheNames = IdRepoConstants.CACHE_PARTNER_EXTRACTOR_FORMATS, key = "{ #subscriberId, #policyId }")
	public PartnerExtractorResponse getPartnerExtractorFormat(String policyId, String subscriberId, String requestId)
			throws ApiNotAccessibleException, PartnerException {
		LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"PARTNER_EXTRACTOR_FORMATS cache lookup partnerId=" + subscriberId + ", policyId=" + policyId);
		LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"started fetching the partner extraction policy data");
		try {
			Map<String, String> pathsegments = new HashMap<>();

			pathsegments.put("partnerId", subscriberId);
			pathsegments.put("policyId", policyId);
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"PMS PARTNER_EXTRACTION_POLICY GET partnerId=" + subscriberId + ", policyId=" + policyId
							+ " — uses selfTokenRestTemplate (Bearer added by kernel-auth interceptor, not PolicyUtil)");
			String responseString = restUtil.getApi(ApiName.PARTNER_EXTRACTION_POLICY, pathsegments, String.class);
			mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
			PartnerExtractorResponseDto responseObject = mapper.readValue(responseString,
					PartnerExtractorResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				if (error.getErrorCode().equalsIgnoreCase("PMS_PRT_064")) {
					LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
							"No partner bioextractors for partner=" + subscriberId + ", policy=" + policyId
									+ " (PMS_PRT_064)");
					return null;
				} else {
					LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
							"PMS partner extraction policy error: " + error.getErrorCode() + " — " + error.getMessage());
					throw new PartnerException(error.getMessage());
				}

			}

			PartnerExtractorResponse partnerExtractorResponse = responseObject != null ? responseObject.getResponse() : null;
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Fetched partner extraction policy details successfully");

			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"ended fetching the policy data");
			return partnerExtractorResponse;
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"error with error message" + ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				if (httpClientException.getStatusCode() == HttpStatus.NOT_FOUND) {
					LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
							"No partner bioextractors configured for partner=" + subscriberId + ", policy=" + policyId);
					return null;
				}
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new PartnerException(e);
			}

		}

	}
	
	/**
	 * Evicts the {@link #DATASHARE_POLICIES} Spring cache region.
	 */
	public void clearDataSharePoliciesCache() {
		Cache cache = cacheManager.getCache(IdRepoConstants.CACHE_DATASHARE_POLICIES);
		if (cache != null)
			cache.clear();
		LOGGER.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "clearDataSharePoliciesCache",
				IdRepoConstants.CACHE_DATASHARE_POLICIES + " cache cleared");
	}

	/**
	 * Evicts the partner extractor formats Spring cache region.
	 */
	public void clearPartnerExtractorFormatsCache() {
		Cache cache = cacheManager.getCache(IdRepoConstants.CACHE_PARTNER_EXTRACTOR_FORMATS);
		if (cache != null)
			cache.clear();
		LOGGER.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(),
				"clearPartnerExtractorFormatsCache", IdRepoConstants.CACHE_PARTNER_EXTRACTOR_FORMATS + " cache cleared");
	}
	
}
