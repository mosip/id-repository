package io.mosip.biosdk.client.utils;

import static io.mosip.biosdk.client.constant.AppConstants.LOGGER_IDTYPE;
import static io.mosip.biosdk.client.constant.AppConstants.LOGGER_SESSIONID;

import java.util.Base64;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.mosip.biosdk.client.config.LoggerConfig;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Spring Framework 7 / Boot 4 replacement for {@code biosdk-client} {@code Util}.
 * <p>
 * Loaded from {@code id-repository-service} before {@code biosdk-client.jar}. The published
 * {@code 1.4.0-SNAPSHOT} artifact still calls {@code HttpEntity(Object, MultiValueMap)} which
 * breaks on Spring 7 even though <a href="https://github.com/mosip/biosdk-client/blob/develop/biosdk-client/src/main/java/io/mosip/biosdk/client/utils/Util.java">develop</a> is fixed.
 * </p>
 */
public final class Util {

	private static final Logger UTIL_LOGGER = LoggerConfig.logConfig(Util.class);

	private static final String DEBUG_REQUEST_RESPONSE = resolveDebugFlag();

	private static final RestTemplate REST_TEMPLATE = new RestTemplate();

	private static ObjectMapper mapper;

	private Util() {
	}

	public static ObjectMapper getObjectMapper() {
		if (mapper == null) {
			mapper = new ObjectMapper();
			mapper.registerModule(new AfterburnerModule());
			mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
			mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		}
		return mapper;
	}

	public static ResponseEntity<?> restRequest(String url, HttpMethod method, MediaType mediaType, Object body,
			Map<String, String> headersMap, Class<?> responseType) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(mediaType);
			if (headersMap != null) {
				headersMap.forEach(headers::add);
			}
			HttpEntity<?> entity = body != null ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);

			if ("y".equalsIgnoreCase(DEBUG_REQUEST_RESPONSE)) {
				UTIL_LOGGER.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, "Request: ",
						getObjectMapper().writeValueAsString(entity.getBody()));
			}

			ResponseEntity<?> response = REST_TEMPLATE.exchange(url, method, entity, responseType);

			if ("y".equalsIgnoreCase(DEBUG_REQUEST_RESPONSE)) {
				UTIL_LOGGER.debug(LOGGER_SESSIONID, LOGGER_IDTYPE, "Response: ",
						getObjectMapper().writeValueAsString(response.getBody()));
			}
			return response;
		} catch (Exception ex) {
			UTIL_LOGGER.error(LOGGER_SESSIONID, LOGGER_IDTYPE, "error ", ex);
			throw new RestClientException("rest call failed", ex);
		}
	}

	public static String base64Encode(String data) {
		return Base64.getEncoder().encodeToString(data.getBytes());
	}

	public static String getDebugRequestResponse() {
		return DEBUG_REQUEST_RESPONSE;
	}

	private static String resolveDebugFlag() {
		String property = System.getProperty("mosip_biosdk_request_response_debug");
		return property != null ? property : System.getenv("mosip_biosdk_request_response_debug");
	}
}
