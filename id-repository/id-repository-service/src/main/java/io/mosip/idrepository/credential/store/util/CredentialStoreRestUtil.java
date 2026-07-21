package io.mosip.idrepository.credential.store.util;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import io.mosip.idrepository.common.constant.LoggerFileConstant;
import io.mosip.idrepository.credential.store.constant.ApiName;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Outbound REST helper for credential-store integrations (PMS, cryptomanager, data-share, VID).
 * Mirrors legacy credential-service {@code io.mosip.credentialstore.util.RestUtil}.
 *
 * @see io.mosip.idrepository.credential.request.util.CredReqRestUtil
 */
public class CredentialStoreRestUtil {

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialStoreRestUtil.class);

	@Autowired
	private EnvUtil environment;

	@Autowired
	@Qualifier("selfTokenRestTemplate")
	RestTemplate restTemplate;

	@SuppressWarnings("unchecked")
	public <T> T postApi(ApiName apiName, List<String> pathsegments, String queryParamName, String queryParamValue,
			MediaType mediaType, Object requestType, Class<?> responseClass) throws Exception {
		T result = null;
		String apiHostIpPort = environment.getProperty(apiName.name());
		UriComponentsBuilder builder = null;
		if (apiHostIpPort != null) {
			builder = UriComponentsBuilder.fromUriString(apiHostIpPort);
		}
		if (builder != null) {
			if (!((pathsegments == null) || (pathsegments.isEmpty()))) {
				for (String segment : pathsegments) {
					if (!((segment == null) || (("").equals(segment)))) {
						builder.pathSegment(segment);
					}
				}
			}
			if (!((queryParamName == null) || (("").equals(queryParamName)))) {
				String[] queryParamNameArr = queryParamName.split(",");
				String[] queryParamValueArr = queryParamValue.split(",");
				for (int i = 0; i < queryParamNameArr.length; i++) {
					builder.queryParam(queryParamNameArr[i], queryParamValueArr[i]);
				}
			}
			try {
				result = (T) restTemplate.postForObject(builder.toUriString(),
						setRequestHeader(requestType, mediaType), responseClass);
			} catch (Exception e) {
				throw new Exception(e);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T> T getApi(ApiName apiName, List<String> pathsegments, String queryParamName, String queryParamValue,
			Class<?> responseType) throws Exception {
		String apiHostIpPort = environment.getProperty(apiName.name());
		T result = null;
		UriComponentsBuilder builder = null;
		UriComponents uriComponents = null;
		if (apiHostIpPort != null) {
			builder = UriComponentsBuilder.fromUriString(apiHostIpPort);
			if (!((pathsegments == null) || (pathsegments.isEmpty()))) {
				for (String segment : pathsegments) {
					if (!((segment == null) || (("").equals(segment)))) {
						builder.pathSegment(segment);
					}
				}
			}
			if (!((queryParamName == null) || (("").equals(queryParamName)))) {
				String[] queryParamNameArr = queryParamName.split(",");
				String[] queryParamValueArr = queryParamValue.split(",");
				for (int i = 0; i < queryParamNameArr.length; i++) {
					builder.queryParam(queryParamNameArr[i], queryParamValueArr[i]);
				}
			}
			uriComponents = builder.build(false).encode();
			IdRepoLogger.getLogger(CredentialStoreRestUtil.class).debug(uriComponents.toUri().toString());
			try {
				result = (T) restTemplate
						.exchange(uriComponents.toUri(), HttpMethod.GET, setRequestHeader(null, null), responseType)
						.getBody();
			} catch (Exception e) {
				throw new Exception(e);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T> T getApi(ApiName apiName, Map<String, String> pathsegments, Class<?> responseType) throws Exception {
		String apiHostIpPort = environment.getProperty(apiName.name());
		T result = null;
		UriComponentsBuilder builder = null;
		if (apiHostIpPort != null) {
			builder = UriComponentsBuilder.fromUriString(apiHostIpPort);
			URI urlWithPath = builder.build(pathsegments);
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.SESSIONID.toString(), apiName.name(),
					"GET " + apiName.name() + " urlTemplate=" + apiHostIpPort + " pathVars=" + pathsegments
							+ " resolvedUrl=" + urlWithPath);
			try {
				result = (T) restTemplate
						.exchange(urlWithPath, HttpMethod.GET, setRequestHeader(null, null), responseType).getBody();
			} catch (Exception e) {
				throw new Exception(e);
			}
		} else {
			LOGGER.warn(IdRepoSecurityManager.getUser(), LoggerFileConstant.SESSIONID.toString(), apiName.name(),
					"GET " + apiName.name() + " skipped — property not configured");
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T> T postApi(String url, List<String> pathsegments, String queryParamName, String queryParamValue,
			MediaType mediaType, Object requestType, Class<?> responseClass) throws Exception {
		T result = null;
		UriComponentsBuilder builder = null;
		if (url != null) {
			builder = UriComponentsBuilder.fromUriString(url);
		}
		if (builder != null) {
			if (!((pathsegments == null) || (pathsegments.isEmpty()))) {
				for (String segment : pathsegments) {
					if (!((segment == null) || (("").equals(segment)))) {
						builder.pathSegment(segment);
					}
				}
			}
			if (!((queryParamName == null) || (("").equals(queryParamName)))) {
				String[] queryParamNameArr = queryParamName.split(",");
				String[] queryParamValueArr = queryParamValue.split(",");
				for (int i = 0; i < queryParamNameArr.length; i++) {
					builder.queryParam(queryParamNameArr[i], queryParamValueArr[i]);
				}
			}
			try {
				result = (T) restTemplate.postForObject(builder.toUriString(),
						setRequestHeader(requestType, mediaType), responseClass);
			} catch (Exception e) {
				throw new Exception(e);
			}
		}
		return result;
	}

	private HttpEntity<Object> setRequestHeader(Object requestType, MediaType mediaType) throws IOException {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		if (mediaType != null) {
			headers.add("Content-Type", mediaType.toString());
		}
		if (requestType != null) {
			try {
				HttpEntity<Object> httpEntity = (HttpEntity<Object>) requestType;
				HttpHeaders httpHeader = httpEntity.getHeaders();
				for (String key : httpHeader.headerNames()) {
					String contentType = "Content-Type";
					if (!(headers.containsKey(contentType) && key.equals(contentType))) {
						headers.add(key, Objects.requireNonNull(httpHeader.get(key)).get(0));
					}
				}
				return new HttpEntity<>(httpEntity.getBody(), headers);
			} catch (ClassCastException e) {
				return new HttpEntity<>(requestType, headers);
			}
		} else {
			return new HttpEntity<>(headers);
		}
	}

}
