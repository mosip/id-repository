package io.mosip.credentialstore.util;

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
	
	
	private static final Logger LOGGER = IdRepoLogger.getLogger(DataShareUtil.class);

	private static final String CREDENTIALFILE = "credentialfile";

	/** The Constant PROTOCOL. */
	public static final String PROTOCOL = "https";

	@Value("${mosip.data.share.protocol}")
	private String httpProtocol;

	@Value("${mosip.data.share.internal.domain.name}")
	private String internalDomainName;

	@Autowired
	private EnvUtil env;

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
		pathsegments.add(policyId);
		pathsegments.add(partnerId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(
				map, headers);
			URL dataShareUrl = null;
			String protocol = PROTOCOL;
			String url = null;

			if (httpProtocol != null && !httpProtocol.isEmpty()) {
				protocol = httpProtocol;
			}

			dataShareUrl = new URL(protocol, internalDomainName, env.getProperty(ApiName.CREATEDATASHARE.name()));
			url = dataShareUrl.toString();
			url = url.replaceAll("[\\[\\]]", "");
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
				throw new io.mosip.credentialstore.exception.ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new DataShareException(e);
			}

		}


	}


}
