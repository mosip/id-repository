package io.mosip.idrepository.core.builder;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import lombok.NoArgsConstructor;

/**
 * A builder for creating and building RestRequest objects from properties
 * 
 * @author Manoj SP
 *
 */
@NoArgsConstructor
public class RestRequestBuilder {

	/** The Constant REST_TIMEOUT. */
	private static final String REST_TIMEOUT = ".rest.timeout";

	/** The Constant REST_HTTP_METHOD. */
	private static final String REST_HTTP_METHOD = ".rest.httpMethod";

	/** The Constant REST_URI. */
	private static final String REST_URI = ".rest.uri";

	/** The Constant REST_HEADERS_MEDIA_TYPE. */
	private static final String REST_HEADERS_MEDIA_TYPE = ".rest.headers.mediaType";

	/** The Constant METHOD_BUILD_REQUEST. */
	private static final String METHOD_BUILD_REQUEST = "buildRequest";

	/** The env. */
	@Autowired
	private Environment env;

	private static HashMap<String, HashMap<String, String>> mapBuilder = new HashMap<>();

	/** The logger. */
	private static Logger mosipLogger = IdRepoLogger.getLogger(RestRequestBuilder.class);

	private List<String> serviceNames = List.of();

	public RestRequestBuilder(List<String> serviceNames) {
		this.serviceNames = serviceNames;
	}

	@PostConstruct
	private void init() {
		for (String serviceName : serviceNames) {
			if (!mapBuilder.containsKey(serviceName)) {
				HashMap<String, String> propertiesMap = new HashMap<>();
				propertiesMap.put(REST_TIMEOUT, env.getProperty(serviceName.concat(REST_TIMEOUT)));
				propertiesMap.put(REST_HTTP_METHOD, env.getProperty(serviceName.concat(REST_HTTP_METHOD)));
				propertiesMap.put(REST_URI, env.getProperty(serviceName.concat(REST_URI)));
				propertiesMap.put(REST_HEADERS_MEDIA_TYPE,
						env.getProperty(serviceName.concat(REST_HEADERS_MEDIA_TYPE)));
				mapBuilder.put(serviceName, propertiesMap);
			}
		}
	}

	/**
	 * Builds the rest request based on the rest service provided using
	 * {@code RestServicesConstants}.
	 *
	 * @param restService the rest service
	 * @param requestBody the request body
	 * @param returnType  the return type
	 * @return the rest request DTO
	 * @throws IdRepoDataValidationException the ID data validation exception
	 */
	
	public RestRequestDTO buildRequest(RestServicesConstants restService, Object requestBody, Class<?> returnType)
			throws IdRepoDataValidationException {
		return buildRequest(restService,Map.of(),requestBody,returnType);
		
	}
	
	public RestRequestDTO buildRequest(RestServicesConstants restService, Map<String, String> pathVariables,
			Object requestBody, Class<?> returnType) throws IdRepoDataValidationException {
		RestRequestDTO request = new RestRequestDTO();
		MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
		String serviceName = restService.getServiceName();
		String uri = getProperty(serviceName, REST_URI);
		String httpMethod = getProperty(serviceName, REST_HTTP_METHOD);
		String timeout = getProperty(serviceName, REST_TIMEOUT);
		HttpHeaders headers = constructHttpHeaders(serviceName);

		checkUri(request, uri);

		checkHttpMethod(request, httpMethod);

		if (requestBody != null) {
			if (!Objects.requireNonNull(headers.getContentType()).includes(MediaType.MULTIPART_FORM_DATA)) {
				request.setRequestBody(requestBody);
			} else {
				if (requestBody instanceof MultiValueMap) {
					request.setRequestBody(requestBody);
				} else {
					throw new IdRepoDataValidationException(INVALID_INPUT_PARAMETER.getErrorCode(),
							String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "requestBody"));
				}
			}
		}

		checkReturnType(returnType, request);

		request.setHeaders(headers);

		if (!paramMap.isEmpty()) {
			request.setParams(paramMap);
		}

		if (!pathVariables.isEmpty()) {
			request.setPathVariables(pathVariables);
		}

		if (!StringUtils.isEmpty(timeout)) {
			request.setTimeout(Integer.parseInt(timeout));
		}

		return request;
	}

	/**
	 * Construct http headers.
	 *
	 * @param serviceName the service name
	 * @return the http headers
	 * @throws IdRepoDataValidationException the id repo data validation exception
	 */
	private HttpHeaders constructHttpHeaders(String serviceName) throws IdRepoDataValidationException {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.valueOf(getProperty(serviceName, REST_HEADERS_MEDIA_TYPE)));
			return headers;
		} catch (InvalidMediaTypeException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), METHOD_BUILD_REQUEST, "returnType",
					"throwing IDDataValidationException - INVALID_INPUT_PARAMETER"
							+ getProperty(serviceName, REST_HEADERS_MEDIA_TYPE));
			throw new IdRepoDataValidationException(INVALID_INPUT_PARAMETER.getErrorCode(), String.format(
					INVALID_INPUT_PARAMETER.getErrorMessage(), getProperty(serviceName, REST_HEADERS_MEDIA_TYPE)));
		}
	}

	/**
	 * Check return type is null or not. If null, exception is thrown.
	 *
	 * @param returnType the return type
	 * @param request    the request
	 * @throws IdRepoDataValidationException the ID data validation exception
	 */
	private void checkReturnType(Class<?> returnType, RestRequestDTO request) throws IdRepoDataValidationException {
		if (returnType != null) {
			request.setResponseType(returnType);
		} else {

			mosipLogger.error(IdRepoSecurityManager.getUser(), METHOD_BUILD_REQUEST, "returnType",
					"throwing IDDataValidationException - INVALID_RETURN_TYPE");
			throw new IdRepoDataValidationException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "returnType"));
		}
	}

	/**
	 * Check http method is null or empty. If so, exception is thrown.
	 *
	 * @param request    the request
	 * @param httpMethod the http method
	 * @throws IdRepoDataValidationException the ID data validation exception
	 */
	private void checkHttpMethod(RestRequestDTO request, String httpMethod) throws IdRepoDataValidationException {
		if (!StringUtils.isEmpty(httpMethod)) {
			request.setHttpMethod(HttpMethod.valueOf(httpMethod));
		} else {
			mosipLogger.error(IdRepoSecurityManager.getUser(), METHOD_BUILD_REQUEST, "httpMethod",
					"throwing IDDataValidationException - INVALID_HTTP_METHOD" + httpMethod);
			throw new IdRepoDataValidationException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "httpMethod"));
		}
	}

	/**
	 * Check uri is null or empty. If so, exception is thrown.
	 *
	 * @param request the request
	 * @param uri     the uri
	 * @throws IdRepoDataValidationException the ID data validation exception
	 */
	private void checkUri(RestRequestDTO request, String uri) throws IdRepoDataValidationException {
		if (!StringUtils.isEmpty(uri)) {
			request.setUri(uri);
		} else {
			mosipLogger.error(IdRepoSecurityManager.getUser(), METHOD_BUILD_REQUEST, "uri",
					"throwing IDDataValidationException - uri is empty or whitespace" + uri);
			throw new IdRepoDataValidationException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "uri"));
		}
	}

	/**
	 * Get Rest properties.
	 *
	 * @param serviceName the service name
	 * @param property    the rest property name
	 * @return the rest property
	 */
	private String getProperty(String serviceName, String property) {
		if (mapBuilder.containsKey(serviceName) && mapBuilder.get(serviceName).containsKey(property)) {
			return mapBuilder.get(serviceName).get(property);
		}
		return null;
	}

}
