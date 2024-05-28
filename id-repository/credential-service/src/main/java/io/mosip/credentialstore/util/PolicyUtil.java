package io.mosip.credentialstore.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.constants.LoggerFileConstant;
import io.mosip.credentialstore.dto.PartnerCredentialTypePolicyDto;
import io.mosip.credentialstore.dto.PartnerExtractorResponse;
import io.mosip.credentialstore.dto.PartnerExtractorResponseDto;
import io.mosip.credentialstore.dto.PolicyManagerResponseDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.PartnerException;
import io.mosip.credentialstore.exception.PolicyException;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;


@Component
public class PolicyUtil {


	/** The rest template. */
	@Autowired
	RestUtil restUtil;

	private static final Logger LOGGER = IdRepoLogger.getLogger(PolicyUtil.class);

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	Utilities utilities;
	
	Map<String, PartnerCredentialTypePolicyDto> policyMap = new HashMap();
	
	Map<String, PartnerExtractorResponse> extractorMap = new HashMap();
	
	@Cacheable(cacheNames = IdRepoConstants.DATASHARE_POLICIES_CACHE, key="{ #credentialType, #subscriberId }")
	public PartnerCredentialTypePolicyDto getPolicyDetail(String credentialType, String subscriberId, String requestId)
			throws PolicyException, ApiNotAccessibleException {

		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					requestId,
					"started fetching the policy data");
			String policyMapKey = credentialType + " " + subscriberId;
            PartnerCredentialTypePolicyDto policyResponseDto = null;
			
			if (policyMap.get(policyMapKey) == null) {
			Map<String, String> pathsegments = new HashMap<>();
			pathsegments.put("partnerId", subscriberId);
			pathsegments.put("credentialType", credentialType);
			String responseString = restUtil.getApi(ApiName.PARTNER_POLICY, pathsegments, String.class);

			PolicyManagerResponseDto responseObject = mapper.readValue(responseString,
					PolicyManagerResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new PolicyException(error.getMessage());
			}
			if (responseObject != null) {
				policyResponseDto = responseObject.getResponse();
			   }
			// caching response object
			policyMap.put(policyMapKey, policyResponseDto);
			}else 
				policyResponseDto = policyMap.get(policyMapKey);
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
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"error with error message" + ExceptionUtils.getStackTrace(e));
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


	@Cacheable(cacheNames = IdRepoConstants.PARTNER_EXTRACTOR_FORMATS_CACHE, key="{ #subscriberId, #policyId }")
	public PartnerExtractorResponse getPartnerExtractorFormat(String policyId, String subscriberId, String requestId)
			throws ApiNotAccessibleException, PartnerException {
		LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"started fetching the partner extraction policy data");
		PartnerExtractorResponse partnerExtractorResponse = null;
		try {
			String extractorKey = policyId + " " + subscriberId;
			if (extractorMap.get(extractorKey) == null) {
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
				} else {
					LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
							error.getMessage());
					throw new PartnerException(error.getMessage());
				}

			}

			   if(responseObject!=null){
				partnerExtractorResponse = responseObject.getResponse();
	             }
			// caching response
			  extractorMap.put(extractorKey, partnerExtractorResponse);
			}
			else 
				partnerExtractorResponse = extractorMap.get(extractorKey);
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
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new PartnerException(e);
			}

		}

	}
	
}
