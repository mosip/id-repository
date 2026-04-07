package io.mosip.credentialstore.util;

import java.io.IOException;
import java.net.MalformedURLException;
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

import io.mosip.credentialstore.constants.ApiName;
import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.credentialstore.constants.LoggerFileConstant;
import io.mosip.credentialstore.dto.DataShare;
import io.mosip.credentialstore.dto.DataShareResponseDto;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.DataShareException;
import io.mosip.idrepository.core.dto.ErrorDTO;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;

@Component
public class DataShareUtil {
	@Autowired
	RestUtil restUtil;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private EnvUtil env;

	private static final Logger LOGGER = IdRepoLogger.getLogger(DataShareUtil.class);

	private static final String CREDENTIALFILE = "credentialfile";

	/** The Constant PROTOCOL. */
	public static final String PROTOCOL = "https";

	@Value("${mosip.data.share.protocol:https}")
	private String httpProtocol;

	@Value("${mosip.data.share.internal.domain.name}")
	private String internalDomainName;

	// Cached base URL for DataShare (major performance win)
	private volatile URL dataShareBaseUrl;

	/**
	 * Lazily initializes and caches the base DataShare URL.
	 * Avoids creating URL object on every request.
	 */
	private URL getDataShareBaseUrl() throws MalformedURLException {
		if (dataShareBaseUrl == null) {
			synchronized (this) {
				if (dataShareBaseUrl == null) {
					String protocol = (httpProtocol != null && !httpProtocol.isEmpty()) ? httpProtocol : "https";
					String path = env.getProperty(ApiName.CREATEDATASHARE.name());
					dataShareBaseUrl = new URL(protocol, internalDomainName, path);
				}
			}
		}
		return dataShareBaseUrl;
	}

	/**
	 * Creates a reusable HttpHeaders for multipart requests.
	 */
	private HttpHeaders createMultipartHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		return headers;
	}

	@Retryable(value = {DataShareException.class, ApiNotAccessibleException.class},
			maxAttemptsExpression = "${mosip.credential.service.retry.maxAttempts}",
			backoff = @Backoff(delayExpression = "${mosip.credential.service.retry.maxDelay}"))
	public DataShare getDataShare(byte[] data, String policyId, String partnerId, String requestId)
			throws ApiNotAccessibleException, DataShareException {

		long fileSizeInBytes = data != null ? data.length : 0;

		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Creating data share entry. Size: {} bytes", fileSizeInBytes);

			// Build multipart request
			LinkedMultiValueMap<String, Object> multipartMap = buildMultipartMap(data);

			HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity =
					new HttpEntity<>(multipartMap, createMultipartHeaders());

			// Build URL with path segments
			List<String> pathSegments = new ArrayList<>(2);
			pathSegments.add(policyId != null ? policyId : partnerId);
			pathSegments.add(partnerId);

			String fullUrl = buildDataShareUrl();

			// Make the call
			String responseString = restUtil.postApi(fullUrl, pathSegments, "", "",
					MediaType.MULTIPART_FORM_DATA, requestEntity, String.class);

			// Parse response
			DataShareResponseDto responseObject = mapper.readValue(responseString, DataShareResponseDto.class);

			if (responseObject == null || hasErrors(responseObject)) {
				LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
						"DataShare failed. File size: {} bytes", fileSizeInBytes);
				throw new DataShareException(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage());
			}

			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Data share created successfully");
			return responseObject.getDataShare();

		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"DataShare failed for size: {} bytes", fileSizeInBytes, e);

			if (e.getCause() instanceof HttpClientErrorException hce) {
				throw new ApiNotAccessibleException(hce.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException hse) {
				throw new ApiNotAccessibleException(hse.getResponseBodyAsString());
			}

			throw new DataShareException(e);
		}
	}

	/**
	 * Builds the multipart map with file content.
	 */
	private LinkedMultiValueMap<String, Object> buildMultipartMap(byte[] data) {
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("name", CREDENTIALFILE);
		map.add("filename", CREDENTIALFILE);

		ByteArrayResource resource = new ByteArrayResource(data) {
			@Override
			public String getFilename() {
				return CREDENTIALFILE;
			}
		};
		map.add("file", resource);
		return map;
	}

	/**
	 * Builds the full DataShare URL using cached base URL.
	 */
	private String buildDataShareUrl() throws MalformedURLException {
		URL baseUrl = getDataShareBaseUrl();
		String urlStr = baseUrl.toString();
		return urlStr.replaceAll("[\\[\\]]", "");   // Remove any unwanted brackets
	}

	/**
	 * Checks if the response contains errors.
	 */
	private boolean hasErrors(DataShareResponseDto response) {
		return response.getErrors() != null && !response.getErrors().isEmpty();
	}
}
