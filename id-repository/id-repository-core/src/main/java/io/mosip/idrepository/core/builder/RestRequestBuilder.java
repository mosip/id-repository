package io.mosip.idrepository.core.builder;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import lombok.NoArgsConstructor;

/**
 * Builds {@link RestRequestDTO} instances from Spring {@link Environment} properties.
 *
 * <p>
 * Service configuration (URI, HTTP method, timeout, content type) is parsed once at
 * startup and cached in {@link #serviceConfigs} to avoid repeated property lookups and
 * {@link MediaType} / {@link HttpMethod} parsing on every outbound call. Callers such as
 * {@link RestHelper} and {@link io.mosip.idrepository.core.helper.AuditHelper} use the
 * resulting DTO for synchronous or reactive HTTP.
 * </p>
 *
 * <h2>Property key pattern</h2>
 * <p>
 * For each {@link RestServicesConstants#getServiceName()} prefix (for example
 * {@code mosip.idrepo.audit}):
 * </p>
 * <ul>
 *   <li>{@code {serviceName}.rest.uri} — target URL (may include path placeholders)</li>
 *   <li>{@code {serviceName}.rest.httpMethod} — HTTP verb ({@code GET}, {@code POST}, …)</li>
 *   <li>{@code {serviceName}.rest.timeout} — timeout in milliseconds (optional)</li>
 *   <li>{@code {serviceName}.rest.headers.mediaType} — request {@code Content-Type}</li>
 * </ul>
 *
 * <h2>Validation</h2>
 * <p>
 * Missing or blank URI, HTTP method, media type, or {@code returnType} raises
 * {@link IdRepoDataValidationException} with
 * {@link io.mosip.idrepository.core.constant.IdRepoErrorConstants#INVALID_INPUT_PARAMETER}.
 * Invalid media types discovered at load time are deferred until
 * {@link RestServiceConfig#createHeaders()} so error handling stays consistent with
 * {@code buildRequest}.
 * </p>
 *
 * <h2>Multipart bodies</h2>
 * <p>
 * When the configured content type includes {@link MediaType#MULTIPART_FORM_DATA}, the
 * request body must be a {@link MultiValueMap}; otherwise
 * {@link IdRepoDataValidationException} is thrown.
 * </p>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * This class is part of the published {@code id-repository-core} API consumed by ID
 * Authentication. Do not rename public methods or change their signatures without an
 * IDA-coordinated release.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * RestRequestDTO request = restRequestBuilder.buildRequest(
 *     RestServicesConstants.AUDIT_MANAGER_SERVICE,
 *     Map.of("requestId", id),
 *     body,
 *     AuditResponseDTO.class);
 * </pre>
 *
 * @author Manoj SP
 * @see RestServicesConstants
 * @see RestRequestDTO
 * @see RestHelper
 */
@NoArgsConstructor
public class RestRequestBuilder {

	/** Suffix for the REST call timeout property (milliseconds). */
	private static final String REST_TIMEOUT = ".rest.timeout";

	/** Suffix for the REST HTTP method property. */
	private static final String REST_HTTP_METHOD = ".rest.httpMethod";

	/** Suffix for the REST URI property. */
	private static final String REST_URI = ".rest.uri";

	/** Suffix for the REST request content-type header property. */
	private static final String REST_HEADERS_MEDIA_TYPE = ".rest.headers.mediaType";

	/** Method name used in audit/error log entries for this builder. */
	private static final String METHOD_BUILD_REQUEST = "buildRequest";

	/** Structured logger for validation failures while building requests. */
	private static Logger mosipLogger = IdRepoLogger.getLogger(RestRequestBuilder.class);

	/** Spring environment supplying outbound service property values. */
	@Autowired
	private Environment env;

	/**
	 * Parsed REST configuration keyed by service name (e.g. {@code mosip.idrepo.audit}).
	 * Populated in {@link #init()}; lazily extended when an unknown service is requested.
	 */
	private final Map<String, RestServiceConfig> serviceConfigs = new HashMap<>();

	/**
	 * Service property prefixes to load at startup; typically all
	 * {@link RestServicesConstants#getServiceName()} values.
	 */
	private List<String> serviceNames = List.of();

	/**
	 * Creates a builder that will preload configuration for the given service names.
	 * <p>
	 * Prefer this constructor (or a Spring {@code @Bean} factory) so
	 * {@link #init()} caches every outbound service used by the application.
	 * </p>
	 *
	 * @param serviceNames list of property-prefix strings to cache at {@link PostConstruct};
	 *                     must not be {@code null} (empty list is allowed)
	 */
	public RestRequestBuilder(List<String> serviceNames) {
		this.serviceNames = serviceNames;
	}

	/**
	 * Loads and caches REST configuration for every entry in {@link #serviceNames}.
	 * <p>
	 * Invoked by Spring after dependency injection. Services not listed here are still
	 * loadable on first {@link #buildRequest} via lazy {@link #loadServiceConfig(String)}.
	 * </p>
	 */
	@PostConstruct
	private void init() {
		for (String serviceName : serviceNames) {
			serviceConfigs.put(serviceName, loadServiceConfig(serviceName));
		}
	}

	/**
	 * Reads and parses REST properties for a single service from {@link #env}.
	 * <p>
	 * Invalid media-type strings are stored as {@code contentType=null} with the raw
	 * value retained so {@link RestServiceConfig#createHeaders()} can throw a consistent
	 * {@link IdRepoDataValidationException} at build time.
	 * </p>
	 *
	 * @param serviceName property prefix (e.g. {@code mosip.idrepo.audit})
	 * @return immutable parsed configuration for the service
	 */
	private RestServiceConfig loadServiceConfig(String serviceName) {
		String uri = env.getProperty(serviceName.concat(REST_URI));
		String httpMethod = env.getProperty(serviceName.concat(REST_HTTP_METHOD));
		String timeout = env.getProperty(serviceName.concat(REST_TIMEOUT));
		String mediaType = env.getProperty(serviceName.concat(REST_HEADERS_MEDIA_TYPE));
		MediaType contentType = null;
		if (!StringUtils.isEmpty(mediaType)) {
			try {
				contentType = MediaType.valueOf(mediaType);
			} catch (InvalidMediaTypeException e) {
				// defer validation to buildRequest for consistent error handling
				contentType = null;
			}
		}
		Integer timeoutMs = StringUtils.isEmpty(timeout) ? null : Integer.valueOf(timeout);
		HttpMethod method = StringUtils.isEmpty(httpMethod) ? null : HttpMethod.valueOf(httpMethod);
		return new RestServiceConfig(uri, method, timeoutMs, contentType, mediaType);
	}

	/**
	 * Builds a REST request without path variables.
	 * <p>
	 * Equivalent to {@link #buildRequest(RestServicesConstants, Map, Object, Class)} with
	 * an empty path-variable map.
	 * </p>
	 *
	 * @param restService the target outbound service
	 * @param requestBody request payload; may be {@code null} for body-less methods
	 * @param returnType  expected response type for deserialization; must not be {@code null}
	 * @return configured {@link RestRequestDTO} ready for {@link RestHelper}
	 * @throws IdRepoDataValidationException if URI, HTTP method, media type, or return type
	 *                                       is missing or invalid
	 */
	public RestRequestDTO buildRequest(RestServicesConstants restService, Object requestBody, Class<?> returnType)
			throws IdRepoDataValidationException {
		return buildRequest(restService, Map.of(), requestBody, returnType);
	}

	/**
	 * Builds a REST request with optional URI path variable substitutions.
	 * <p>
	 * Path placeholders in the configured URI (for example {@code {uin}}) are supplied via
	 * {@code pathVariables} and later expanded by the HTTP client. When the content type
	 * is multipart, {@code requestBody} must be a {@link MultiValueMap}.
	 * </p>
	 *
	 * @param restService   the target outbound service
	 * @param pathVariables map of path placeholder names to values; use {@link Map#of()} when
	 *                      none are needed
	 * @param requestBody   request payload; for multipart services must be a
	 *                      {@link MultiValueMap}; may be {@code null}
	 * @param returnType    expected response type for deserialization; must not be {@code null}
	 * @return configured {@link RestRequestDTO} ready for {@link RestHelper}
	 * @throws IdRepoDataValidationException if URI, HTTP method, media type, return type, or
	 *                                       multipart body shape is invalid
	 */
	public RestRequestDTO buildRequest(RestServicesConstants restService, Map<String, String> pathVariables,
			Object requestBody, Class<?> returnType) throws IdRepoDataValidationException {
		RestRequestDTO request = new RestRequestDTO();
		String serviceName = restService.getServiceName();
		RestServiceConfig config = serviceConfigs.get(serviceName);
		if (config == null) {
			config = loadServiceConfig(serviceName);
			serviceConfigs.put(serviceName, config);
		}

		checkUri(request, config.uri());

		checkHttpMethod(request, config.httpMethod());

		HttpHeaders headers = config.createHeaders();

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

		if (!pathVariables.isEmpty()) {
			request.setPathVariables(pathVariables);
		}

		if (config.timeoutMs() != null) {
			request.setTimeout(config.timeoutMs());
		}

		return request;
	}

	/**
	 * Validates and assigns the response type on the request DTO.
	 *
	 * @param returnType expected response class; must not be {@code null}
	 * @param request    DTO being populated
	 * @throws IdRepoDataValidationException if {@code returnType} is {@code null}
	 */
	private void checkReturnType(Class<?> returnType, RestRequestDTO request) throws IdRepoDataValidationException {
		if (returnType != null) {
			request.setResponseType(returnType);
		} else {
			mosipLogger.error(IdRepoSecurityManager.getUser(),
					METHOD_BUILD_REQUEST, "returnType",
					"throwing IDDataValidationException - INVALID_RETURN_TYPE");
			throw new IdRepoDataValidationException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "returnType"));
		}
	}

	/**
	 * Validates and assigns the HTTP method on the request DTO.
	 *
	 * @param request    DTO being populated
	 * @param httpMethod parsed HTTP method from cached config; must not be {@code null}
	 * @throws IdRepoDataValidationException if {@code httpMethod} is {@code null}
	 */
	private void checkHttpMethod(RestRequestDTO request, HttpMethod httpMethod) throws IdRepoDataValidationException {
		if (httpMethod != null) {
			request.setHttpMethod(httpMethod);
		} else {
			mosipLogger.error(IdRepoSecurityManager.getUser(),
					METHOD_BUILD_REQUEST, "httpMethod",
					"throwing IDDataValidationException - INVALID_HTTP_METHOD");
			throw new IdRepoDataValidationException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "httpMethod"));
		}
	}

	/**
	 * Validates and assigns the target URI on the request DTO.
	 *
	 * @param request DTO being populated
	 * @param uri     service URI from cached config; must not be null or blank
	 * @throws IdRepoDataValidationException if {@code uri} is null or empty
	 */
	private void checkUri(RestRequestDTO request, String uri) throws IdRepoDataValidationException {
		if (!StringUtils.isEmpty(uri)) {
			request.setUri(uri);
		} else {
			mosipLogger.error(IdRepoSecurityManager.getUser(),
					METHOD_BUILD_REQUEST, "uri",
					"throwing IDDataValidationException - uri is empty or whitespace" + uri);
			throw new IdRepoDataValidationException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "uri"));
		}
	}

	/**
	 * Immutable, pre-parsed REST settings for one outbound service.
	 *
	 * @param uri          target URI (may contain path placeholders)
	 * @param httpMethod   HTTP verb; {@code null} if the property was missing
	 * @param timeoutMs    call timeout in milliseconds; {@code null} if not configured
	 * @param contentType  parsed media type; {@code null} when raw value is invalid
	 *                     (validated lazily in {@link #createHeaders()})
	 * @param rawMediaType original media-type property string used for error messages
	 */
	private record RestServiceConfig(String uri, HttpMethod httpMethod, Integer timeoutMs, MediaType contentType,
			String rawMediaType) {
		/**
		 * Creates request headers with the configured content type.
		 *
		 * @return new {@link HttpHeaders} with {@code Content-Type} set
		 * @throws IdRepoDataValidationException if the media type property is missing or invalid
		 */
		HttpHeaders createHeaders() throws IdRepoDataValidationException {
			try {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(contentType != null ? contentType : MediaType.valueOf(rawMediaType));
				return headers;
			} catch (InvalidMediaTypeException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(),
						METHOD_BUILD_REQUEST, "returnType",
						"throwing IDDataValidationException - INVALID_INPUT_PARAMETER" + rawMediaType);
				throw new IdRepoDataValidationException(INVALID_INPUT_PARAMETER.getErrorCode(), String.format(
						INVALID_INPUT_PARAMETER.getErrorMessage(), rawMediaType));
			}
		}
	}
}
