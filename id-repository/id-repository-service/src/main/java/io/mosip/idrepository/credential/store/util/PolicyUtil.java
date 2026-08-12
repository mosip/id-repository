package io.mosip.idrepository.credential.store.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.common.constant.LoggerFileConstant;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.credential.store.constant.ApiName;
import io.mosip.idrepository.credential.store.dto.PartnerCredentialTypePolicyDto;
import io.mosip.idrepository.credential.store.dto.PartnerExtractorResponse;
import io.mosip.idrepository.credential.store.dto.PartnerExtractorResponseDto;
import io.mosip.idrepository.credential.store.dto.PolicyManagerResponseDto;
import io.mosip.idrepository.credential.store.exception.ApiNotAccessibleException;
import io.mosip.idrepository.credential.store.exception.PartnerException;
import io.mosip.idrepository.credential.store.exception.PolicyException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;
import jakarta.annotation.PostConstruct;

/**
 * Partner Management Service (PMS) policy client with Spring Cache.
 * <p>
 * When {@code mosip.idrepo.policy.local-source=true}, policies are read from classpath JSON
 * under {@code pms_policy/} (from GET {@code /v1/policymanager/policies} dump) instead of remote PMS.
 * </p>
 */
@Component
public class PolicyUtil {

	private static final Logger LOGGER = IdRepoLogger.getLogger(PolicyUtil.class);

	@Autowired
	private CredentialStoreRestUtil restUtil;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private Utilities utilities;

	@Autowired
	private CacheManager cacheManager;

	@Value("${mosip.idrepo.policy.local-source:false}")
	private boolean localPolicySource;

	@Value("${mosip.idrepo.policy.local-index:classpath:pms_policy/partner-credential-policy-index.json}")
	private Resource localPolicyIndex;

	@Value("${mosip.idrepo.policy.local-all:classpath:pms_policy/pms-all-policy.json}")
	private Resource localPolicyAll;

	private Map<String, JsonNode> partnerCredentialIndex = Map.of();
	private Map<String, JsonNode> policiesById = Map.of();

	@PostConstruct
	void loadLocalPolicyFiles() throws IOException {
		if (!localPolicySource) {
			return;
		}
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		if (localPolicyIndex != null && localPolicyIndex.exists()) {
			try (InputStream in = localPolicyIndex.getInputStream()) {
				partnerCredentialIndex = mapper.readValue(in, new TypeReference<Map<String, JsonNode>>() {
				});
			}
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.SESSIONID.toString(),
					"Loaded local partner-credential policy index entries=" + partnerCredentialIndex.size());
		}
		if (localPolicyAll != null && localPolicyAll.exists()) {
			try (InputStream in = localPolicyAll.getInputStream()) {
				policiesById = mapper.readValue(in, new TypeReference<Map<String, JsonNode>>() {
				});
			}
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.SESSIONID.toString(),
					LoggerFileConstant.SESSIONID.toString(),
					"Loaded local pms-all-policy entries=" + policiesById.size());
		}
	}

	@Cacheable(cacheNames = IdRepoConstants.CACHE_DATASHARE_POLICIES, key = "{ #credentialType, #subscriberId }")
	public PartnerCredentialTypePolicyDto getPolicyDetail(String credentialType, String subscriberId, String requestId)
			throws PolicyException, ApiNotAccessibleException {

		if (localPolicySource) {
			return getLocalPolicyDetail(credentialType, subscriberId, requestId);
		}

		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"started fetching the policy data");
			Map<String, String> pathsegments = new HashMap<>();
			pathsegments.put("partnerId", subscriberId);
			pathsegments.put("credentialType", credentialType);
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"PMS PARTNER_POLICY GET partnerId=" + subscriberId + ", credentialType=" + credentialType
							+ " — uses selfTokenRestTemplate (Bearer added by kernel-auth interceptor, not PolicyUtil)");
			String responseString = restUtil.getApi(ApiName.PARTNER_POLICY, pathsegments, String.class);

			PolicyManagerResponseDto responseObject = mapper.readValue(responseString, PolicyManagerResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new PolicyException(error.getMessage());
			}
			PartnerCredentialTypePolicyDto policyResponseDto = responseObject != null ? responseObject.getResponse()
					: null;
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
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
						"PMS returned 403 KER-ATH-403 — partnermanager rejected the service-account Bearer token. Body: "
								+ httpClientException.getResponseBodyAsString());
			} else {
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

	private PartnerCredentialTypePolicyDto getLocalPolicyDetail(String credentialType, String subscriberId,
			String requestId) throws PolicyException {
		try {
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"LOCAL policy lookup partnerId=" + subscriberId + ", credentialType=" + credentialType);
			JsonNode node = partnerCredentialIndex.get(subscriberId + "|" + credentialType);
			if (node == null && credentialType != null) {
				node = policiesById.get("mpolicy-default-" + credentialType);
			}
			if (node == null && credentialType != null) {
				node = policiesById.get("mpolicy-default-" + credentialType.toLowerCase());
			}
			if (node == null) {
				throw new PolicyException("Local policy not found for partnerId=" + subscriberId + ", credentialType="
						+ credentialType);
			}
			PartnerCredentialTypePolicyDto dto = mapper.treeToValue(node, PartnerCredentialTypePolicyDto.class);
			if (dto.getPartnerId() == null) {
				dto.setPartnerId(subscriberId);
			}
			if (dto.getCredentialType() == null) {
				dto.setCredentialType(credentialType);
			}
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Fetched local policy details successfully policyId=" + dto.getPolicyId());
			return dto;
		} catch (PolicyException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"local policy error " + ExceptionUtils.getStackTrace(e));
			throw new PolicyException(e);
		}
	}

	@Cacheable(cacheNames = IdRepoConstants.CACHE_PARTNER_EXTRACTOR_FORMATS, key = "{ #subscriberId, #policyId }")
	public PartnerExtractorResponse getPartnerExtractorFormat(String policyId, String subscriberId, String requestId)
			throws ApiNotAccessibleException, PartnerException {
		LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"PARTNER_EXTRACTOR_FORMATS cache lookup partnerId=" + subscriberId + ", policyId=" + policyId);
		if (localPolicySource) {
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"LOCAL mode: no bioextractors configured — returning null");
			return null;
		}
		try {
			Map<String, String> pathsegments = new HashMap<>();
			pathsegments.put("partnerId", subscriberId);
			pathsegments.put("policyId", policyId);
			String responseString = restUtil.getApi(ApiName.PARTNER_EXTRACTION_POLICY, pathsegments, String.class);
			mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
			PartnerExtractorResponseDto responseObject = mapper.readValue(responseString,
					PartnerExtractorResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				if (error.getErrorCode().equalsIgnoreCase("PMS_PRT_064")) {
					return null;
				}
				throw new PartnerException(error.getMessage());
			}
			return responseObject != null ? responseObject.getResponse() : null;
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"error with error message" + ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				if (httpClientException.getStatusCode() == HttpStatus.NOT_FOUND) {
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

	public void clearDataSharePoliciesCache() {
		Cache cache = cacheManager.getCache(IdRepoConstants.CACHE_DATASHARE_POLICIES);
		if (cache != null)
			cache.clear();
		LOGGER.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "clearDataSharePoliciesCache",
				IdRepoConstants.CACHE_DATASHARE_POLICIES + " cache cleared");
	}

	public void clearPartnerExtractorFormatsCache() {
		Cache cache = cacheManager.getCache(IdRepoConstants.CACHE_PARTNER_EXTRACTOR_FORMATS);
		if (cache != null)
			cache.clear();
		LOGGER.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(),
				"clearPartnerExtractorFormatsCache",
				IdRepoConstants.CACHE_PARTNER_EXTRACTOR_FORMATS + " cache cleared");
	}
}
