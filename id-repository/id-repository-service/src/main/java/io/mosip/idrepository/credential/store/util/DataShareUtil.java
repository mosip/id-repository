package io.mosip.idrepository.credential.store.util;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.credential.store.constant.ApiName;
import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.idrepository.common.constant.LoggerFileConstant;
import io.mosip.idrepository.credential.store.dto.DataShare;
import io.mosip.idrepository.credential.store.dto.DataShareResponseDto;
import io.mosip.idrepository.credential.store.exception.ApiNotAccessibleException;
import io.mosip.idrepository.credential.store.exception.DataShareException;
import io.mosip.idrepository.core.dto.ErrorDTO;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Uploads issued credential artifacts to MOSIP Data Share (multipart) and returns partner URLs.
 * <p>
 * Builds internal cluster URLs from {@link #internalDomainName} and posts encrypted credential bytes.
 * </p>
 *
 * @see io.mosip.idrepository.credential.store.service.impl.CredentialStoreServiceImpl
 */
@Component
public class DataShareUtil {

	private static final Logger LOGGER = IdRepoLogger.getLogger(DataShareUtil.class);

	private static final String CREDENTIALFILE = "credentialfile";

	// TODO(TESTING): Set to false and remove constants below after dev2 datashare testing.
	private static final boolean USE_DEV2_DATASHARE_BASE_URL = true;
	private static final String DEV2_DATASHARE_BASE_URL = "https://api-internal.dev2.mosip.net";

	/** Default protocol when {@link #httpProtocol} is unset. */
	public static final String PROTOCOL = "https";

	/** Outbound REST client for data-share multipart upload. */
	@Autowired
	private CredentialStoreRestUtil restUtil;

	/** Deserializes data-share service responses. */
	@Autowired
	private ObjectMapper mapper;

	/**
	 * Protocol for internal data-share URL construction.
	 * Config key: {@value io.mosip.idrepository.core.constant.IdRepoConstants#DATA_SHARE_PROTOCOL}.
	 */
	@Value("${" + IdRepoConstants.DATA_SHARE_PROTOCOL + "}")
	private String httpProtocol;

	/**
	 * Internal Kubernetes/service domain for data-share host.
	 * Config key: {@value io.mosip.idrepository.core.constant.IdRepoConstants#DATA_SHARE_INTERNAL_DOMAIN_NAME}.
	 */
	@Value("${" + IdRepoConstants.DATA_SHARE_INTERNAL_DOMAIN_NAME + "}")
	private String internalDomainName;

	/** Resolves data-share API path from {@link ApiName#CREATEDATASHARE}. */
	@Autowired
	private EnvUtil env;

	/**
	 * Uploads issued credential bytes to MOSIP Data Share and returns partner URL metadata.
	 * <p>
	 * Retries on transient failures per {@code mosip.credential.service.retry.*} properties.
	 * </p>
	 *
	 * @param data      credential payload bytes
	 * @param policyId  data-share policy id (falls back to partner id when null)
	 * @param partnerId partner/subscriber id
	 * @param requestId correlation id for logging
	 * @return data-share URL, TTL, and signature metadata
	 * @throws ApiNotAccessibleException if HTTP call fails
	 * @throws IOException               if response parsing fails
	 * @throws DataShareException        if data-share service returns errors
	 */
	@Retryable(value = { DataShareException.class,
			ApiNotAccessibleException.class }, maxAttemptsExpression = "${mosip.credential.service.retry.maxAttempts}", backoff = @Backoff(delayExpression = "${mosip.credential.service.retry.maxDelay}"))
	public DataShare getDataShare(byte[] data, String policyId, String partnerId, String requestId)
			throws ApiNotAccessibleException, IOException, DataShareException {
		long fileLengthInBytes=0;
		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
		
					"creating data share entry");
			LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("name", CREDENTIALFILE);
			map.add("filename", CREDENTIALFILE);

			ByteArrayResource contentsAsResource = new ByteArrayResource(data) {
				@Override
				public String getFilename() {
					return CREDENTIALFILE;
				}
			};
			map.add("file", contentsAsResource);
			fileLengthInBytes = contentsAsResource.contentLength();
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(policyId == null? partnerId : policyId);
		pathsegments.add(partnerId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(
				map, headers);
			String url;

			// TODO(TESTING): Remove if-branch and USE_DEV2_* constants after dev2 datashare testing.
			if (USE_DEV2_DATASHARE_BASE_URL) {
				url = DEV2_DATASHARE_BASE_URL + env.getProperty(ApiName.CREATEDATASHARE.name());
			} else {
				URL dataShareUrl = null;
				String protocol = PROTOCOL;

				if (httpProtocol != null && !httpProtocol.isEmpty()) {
					protocol = httpProtocol;
				}

				dataShareUrl = new URL(protocol, internalDomainName, env.getProperty(ApiName.CREATEDATASHARE.name()));
				url = dataShareUrl.toString();
				url = url.replaceAll("[\\[\\]]", "");
			}
			String responseString = restUtil.postApi(url, pathsegments, "", "",
					MediaType.MULTIPART_FORM_DATA, requestEntity, String.class);

		DataShareResponseDto responseObject = mapper.readValue(responseString, DataShareResponseDto.class);

		if (responseObject == null) {
				LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"File size" + " " + fileLengthInBytes);
				LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage());

			throw new DataShareException();
		}
		if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {

			ErrorDTO error = responseObject.getErrors().get(0);
				LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"File size" + " " + fileLengthInBytes);
				LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					error.getMessage());
			throw new DataShareException();

		} else {

				LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"data share created");
			return responseObject.getDataShare();

			}
		} catch (Exception e) {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"File size" + " " + fileLengthInBytes);
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new io.mosip.idrepository.credential.store.exception.ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new DataShareException(e);
			}

		}


	}


}
