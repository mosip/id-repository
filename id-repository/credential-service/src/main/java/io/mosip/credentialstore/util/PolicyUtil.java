package io.mosip.credentialstore.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.dto.PartnerExtractorResponse;
import io.mosip.credentialstore.dto.PartnerExtractorResponseDto;
import io.mosip.credentialstore.dto.PolicyManagerResponseDto;
import io.mosip.credentialstore.dto.PolicyResponseDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.PartnerException;
import io.mosip.credentialstore.exception.PolicyException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;


@Component
public class PolicyUtil {


	/** The env. */
	@Autowired
	private Environment env;

	/** The rest template. */
	@Autowired
	RestUtil restUtil;



	private static final Logger LOGGER = IdRepoLogger.getLogger(EncryptionUtil.class);

	private static final String GETPOLICYDETAIL = "getPolicyDetail";

	private static final String POLICYUTIL = "PolicyUtil";

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	Utilities utilities;

	public PolicyResponseDto getPolicyDetail(String policyId, String subscriberId) throws PolicyException, ApiNotAccessibleException {

		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), POLICYUTIL, GETPOLICYDETAIL,
					"started fetching the policy data");
			Map<String, String> pathsegments = new HashMap<>();
			pathsegments.put("partnerId", subscriberId);
			pathsegments.put("policyId", policyId);
			String responseString = restUtil.getApi(ApiName.PARTNER_POLICY, pathsegments, String.class);

			PolicyManagerResponseDto responseObject = mapper.readValue(responseString,
					PolicyManagerResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				ServiceError error = responseObject.getErrors().get(0);
				throw new PolicyException(error.getMessage());
			}
			PolicyResponseDto policyResponseDto = responseObject.getResponse();
			LOGGER.info(IdRepoSecurityManager.getUser(), POLICYUTIL, GETPOLICYDETAIL,
					"Fetched policy details successfully");
			LOGGER.debug(IdRepoSecurityManager.getUser(), POLICYUTIL, GETPOLICYDETAIL,
					"ended fetching the policy data");
			return policyResponseDto;

		} catch (IOException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), POLICYUTIL, GETPOLICYDETAIL,
					"error with error message" + ExceptionUtils.getStackTrace(e));
			throw new PolicyException(e);
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(),  POLICYUTIL, GETPOLICYDETAIL,
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



	public PartnerExtractorResponse getPartnerExtractorFormat(String policyId, String subscriberId)
			throws ApiNotAccessibleException, PartnerException {
		LOGGER.debug(IdRepoSecurityManager.getUser(), POLICYUTIL, GETPOLICYDETAIL,
				"started fetching the partner extraction policy data");
		PartnerExtractorResponse partnerExtractorResponse = null;
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
				} else {
					throw new PartnerException(error.getMessage());
				}

			}

			partnerExtractorResponse = responseObject.getResponse();
			LOGGER.info(IdRepoSecurityManager.getUser(), POLICYUTIL, GETPOLICYDETAIL,
					"Fetched partner extraction policy details successfully");

			LOGGER.debug(IdRepoSecurityManager.getUser(), POLICYUTIL, GETPOLICYDETAIL,
					"ended fetching the policy data");
			return partnerExtractorResponse;
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), POLICYUTIL, GETPOLICYDETAIL,
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
