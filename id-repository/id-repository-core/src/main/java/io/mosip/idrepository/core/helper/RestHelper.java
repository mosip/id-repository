package io.mosip.idrepository.core.helper;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.CLIENT_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.CONNECTION_TIMED_OUT;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.SERVER_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UNKNOWN_ERROR;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.AuthenticationException;
import io.mosip.idrepository.core.exception.IdRepoRetryException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.RestUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.retry.WithRetry;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Reactive HTTP client helper for synchronous and asynchronous outbound REST calls.
 * <p>
 * Uses Spring {@link WebClient} to invoke MOSIP microservices (cryptomanager, audit-manager,
 * PMS, datashare, keymanager, etc.). Request descriptors are typically built by
 * {@link io.mosip.idrepository.core.builder.RestRequestBuilder} from
 * {@link io.mosip.idrepository.core.constant.RestServicesConstants} keys.
 * </p>
 *
 * <h2>WebClient selection</h2>
 * <p>
 * {@link #init()} prefers the {@code selfTokenWebClient} bean (IAM client-credentials) when
 * present; otherwise falls back to {@code webClient}. Buffer size is raised via
 * {@code mosip.idrepo.rest.client.max-in-memory-size} (default 20 MiB) for large crypto payloads.
 * </p>
 *
 * <h2>Retry and errors</h2>
 * <ul>
 *   <li>{@link #requestSync} is annotated with {@code @WithRetry} — timeouts, 403, and 5xx
 *       throw {@link IdRepoRetryException} so kernel retry can re-attempt</li>
 *   <li>401 becomes {@link AuthenticationException}; other 4xx become {@link RestServiceException}</li>
 *   <li>HTTP 200 bodies with a MOSIP {@code errors} array are treated as client errors</li>
 * </ul>
 *
 * <h2>Jackson 2 note</h2>
 * <p>
 * Response bodies are decoded with the injected Jackson 2 {@link ObjectMapper} rather than
 * Boot 4's default Jackson 3 WebClient codecs, so {@code ObjectNode} and other Jackson 2 types
 * deserialize correctly.
 * </p>
 *
 * @see io.mosip.idrepository.core.builder.RestRequestBuilder
 * @see RestServiceException
 * @see IdRepoRetryException
 * @see AuthenticationException
 * @author Manoj SP
 */
@NoArgsConstructor
public class RestHelper {

	private static final String CHECK_ERROR_RESPONSE = "checkErrorResponse";

	private static final String UNKNOWN_ERROR_LOG = "- UNKNOWN_ERROR - ";

	/** JSON key for MOSIP error arrays in response bodies. */
	private static final String ERRORS = "errors";

	/** Log method name for synchronous request handling. */
	private static final String METHOD_REQUEST_SYNC = "requestSync";

	/** Log method name for HTTP status error handling. */
	private static final String METHOD_HANDLE_STATUS_ERROR = "handleStatusError";

	/** Log prefix for outbound request URI logging. */
	private static final String PREFIX_REQUEST = "Request : ";

	/** Log method name for asynchronous request handling. */
	private static final String METHOD_REQUEST_ASYNC = "requestAsync";

	/** Log class name identifier for structured logging. */
	private static final String CLASS_REST_HELPER = "RestHelper";

	/** Log message prefix when throwing {@link RestServiceException}. */
	private static final String THROWING_REST_SERVICE_EXCEPTION = "Throwing RestServiceException";

	/** Log method name for runtime exceptions during sync requests. */
	private static final String REQUEST_SYNC_RUNTIME_EXCEPTION = "requestSync-RuntimeException";

	/** Log method name for HTTP request/response tracing. */
	private static final String LOG_HTTP_EXCHANGE = "httpExchange";

	/** MOSIP UTC timestamps must end with literal {@code Z} (audit, cryptomanager, etc.). */
	private static final Pattern MOSIP_UTC_MS_NO_Z = Pattern
			.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}");

	/** JSON mapper for parsing error response bodies. */
	@Autowired
	private ObjectMapper mapper;

	/** Spring context used to resolve {@code selfTokenWebClient} or {@code webClient} in {@link #init()}. */
	@Autowired
	private ApplicationContext ctx;

	/**
	 * WebClient in-memory buffer for large keymanager/cryptomanager payloads.
	 * Property: {@code mosip.idrepo.rest.client.max-in-memory-size} (default {@code 20971520} = 20 MiB).
	 */
	@Value("${mosip.idrepo.rest.client.max-in-memory-size:20971520}")
	private int maxInMemorySize;

	/** Structured logger for REST helper operations. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(RestHelper.class);

	/** Reactive HTTP client; resolved from context in {@link #init()} if not constructor-injected. */
	private WebClient webClient;

	/**
	 * Creates a RestHelper with an explicit {@link WebClient} instance.
	 *
	 * @param webClient configured WebClient bean for outbound HTTP calls
	 */
	public RestHelper(WebClient webClient) {
		this.webClient = webClient;
	}

	/**
	 * Resolves the {@link WebClient} bean from the application context when not set via constructor,
	 * then applies {@link #maxInMemorySize} exchange strategies.
	 * <p>
	 * Prefers {@code selfTokenWebClient} (outbound IAM token) over plain {@code webClient}.
	 * </p>
	 */
	@PostConstruct
	public void init() {
		if (Objects.isNull(webClient)) {
			if (ctx.containsBean("selfTokenWebClient")) {
				webClient = ctx.getBean("selfTokenWebClient", WebClient.class);
			} else {
				webClient = ctx.getBean("webClient", WebClient.class);
			}
		}
		webClient = webClient.mutate()
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
						.build())
				.build();
	}
	
	/**
	 * Sends an HTTP request synchronously and returns the deserialized response.
	 * <p>
	 * Annotated with {@code @WithRetry} — throws {@link IdRepoRetryException} on transient
	 * failures to trigger kernel retry. Validates MOSIP error arrays in non-String responses.
	 * </p>
	 *
	 * @param <T>     expected response type
	 * @param request REST request descriptor with URI, method, headers, and body
	 * @return deserialized response body
	 * @throws RestServiceException on client errors or unrecoverable failures
	 */
	@SuppressWarnings("unchecked")
	@WithRetry
	public <T> T requestSync(@Valid RestRequestDTO request) throws RestServiceException {
		Object response;
		try {
			mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_REQUEST_SYNC,
					request.getUri());
			if (request.getTimeout() != null) {
				// *.rest.timeout values in MOSIP config are historically seconds (e.g. 100 ≈ 100s),
				// despite older DTO comments saying ms. ofMillis breaks cryptomanager/audit calls.
				response = request(request).timeout(Duration.ofSeconds(request.getTimeout())).block();
			} else {
				response = request(request).block();
			}
			if(!String.class.equals(request.getResponseType()) && !byte[].class.equals(request.getResponseType())) {
				checkErrorResponse(response, request.getResponseType());
			}
			mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_REQUEST_SYNC,
					"Received valid response");
			return (T) response;
		} catch (WebClientResponseException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_REQUEST_SYNC,
					THROWING_REST_SERVICE_EXCEPTION + "- Http Status error - \n " + e.getMessage()
							+ " \n Response Body : \n" + e.getResponseBodyAsString());
			throw handleStatusError(e, request.getResponseType());
		} catch (RestServiceException e) {
			throw e;
		} catch (RuntimeException e) {
			RestServiceException restError = unwrapRestServiceException(e);
			if (restError != null) {
				throw restError;
			}
			if (e.getCause() != null && e.getCause().getClass().equals(TimeoutException.class)) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_REQUEST_SYNC,
						THROWING_REST_SERVICE_EXCEPTION + "- CONNECTION_TIMED_OUT - \n " + ExceptionUtils.getStackTrace(e));
				throw new IdRepoRetryException(new RestServiceException(CONNECTION_TIMED_OUT, e));
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, REQUEST_SYNC_RUNTIME_EXCEPTION,
						THROWING_REST_SERVICE_EXCEPTION + UNKNOWN_ERROR_LOG + ExceptionUtils.getStackTrace(e));
				throw new IdRepoRetryException(new RestServiceException(UNKNOWN_ERROR, e));
			}
		}
	}

	/**
	 * Sends an HTTP request asynchronously on the Spring {@code @Async} thread pool.
	 * <p>
	 * Delegates to {@link #requestSync} and wraps the result in a {@link CompletableFuture}.
	 * Failures are returned as {@code CompletableFuture.failedFuture(e)}.
	 * </p>
	 *
	 * @param request REST request descriptor
	 * @return future completing with the response, or failing with {@link RestServiceException}
	 */
	@Async
	public CompletableFuture<Object> requestAsync(@Valid RestRequestDTO request) {
		mosipLogger.debug(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_REQUEST_ASYNC,
				PREFIX_REQUEST + request.getUri());
		try {
			Object obj =  requestSync(request);
			return CompletableFuture.completedFuture(obj);
		} catch (RestServiceException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_REQUEST_ASYNC,
					ExceptionUtils.getStackTrace(e));
			return CompletableFuture.failedFuture(e);
		}
	}

	/**
	 * Builds and executes a reactive HTTP call for the given request descriptor.
	 * <p>
	 * Expands path variables and query params onto the URI, applies headers, serializes the
	 * JSON body (with UTC {@code Z} normalization), and returns a {@link Mono} of the
	 * decoded response type.
	 * </p>
	 *
	 * @param request REST request descriptor with URI, method, headers, and body
	 * @return reactive publisher that completes with the response body
	 */
	private Mono<?> request(RestRequestDTO request) {
		Mono<?> monoResponse;
		RequestBodySpec requestBodySpec;
		ResponseSpec exchange;
		
		if (request.getParams() != null && request.getPathVariables() == null) {
			request.setUri(UriComponentsBuilder
					.fromUriString(request.getUri())
					.queryParams(request.getParams())
					.toUriString());
		} else if (request.getParams() == null && request.getPathVariables() != null) {
			request.setUri(UriComponentsBuilder
					.fromUriString(request.getUri())
					.buildAndExpand(request.getPathVariables())
					.toUriString());
		} else if (request.getParams() != null && request.getPathVariables() != null) {
			request.setUri(UriComponentsBuilder
					.fromUriString(request.getUri())
					.queryParams(request.getParams())
					.buildAndExpand(request.getPathVariables())
					.toUriString());
		}
		
		requestBodySpec = webClient.method(request.getHttpMethod()).uri(request.getUri());

		if (request.getHeaders() != null) {
			requestBodySpec = requestBodySpec
					.headers(headers -> headers.addAll(request.getHeaders()));
		}

		mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, LOG_HTTP_EXCHANGE,
				request.getHttpMethod() + " " + request.getUri());

		if (request.getRequestBody() != null) {
			String requestJson = writeJsonBody(request.getRequestBody());
			mosipLogger.info(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, LOG_HTTP_EXCHANGE,
					"request body=" + summarizeRequestBody(requestJson));
			exchange = requestBodySpec.contentType(MediaType.APPLICATION_JSON).bodyValue(requestJson).retrieve();
		} else {
			exchange = requestBodySpec.retrieve();
		}

		monoResponse = decodeResponse(exchange, request.getResponseType());

		return monoResponse;
	}

	/**
	 * Decodes HTTP response bodies with the injected Jackson 2 {@link ObjectMapper}.
	 * <p>
	 * Spring Boot 4 WebClient defaults to Jackson 3 codecs, which cannot deserialize
	 * {@code com.fasterxml.jackson.databind.node.ObjectNode} and other Jackson 2 types.
	 * String and {@code byte[]} responses bypass JSON parsing.
	 * </p>
	 *
	 * @param exchange     WebClient response spec after {@code retrieve()}
	 * @param responseType expected Java type (may be {@code null}, treated as String)
	 * @return mono of the decoded body
	 */
	private Mono<?> decodeResponse(ResponseSpec exchange, Class<?> responseType) {
		if (responseType == null || String.class.equals(responseType)) {
			return exchange.bodyToMono(String.class);
		}
		if (byte[].class.equals(responseType)) {
			return exchange.bodyToMono(byte[].class);
		}
		return exchange.bodyToMono(String.class).flatMap(body -> parseResponseBodyMono(body, responseType));
	}

	/**
	 * Parses a MOSIP JSON response: checks the raw {@code errors} array first, then deserializes.
	 *
	 * @param body         raw response body string
	 * @param responseType target type for successful deserialization
	 * @return mono of the typed body, or {@link Mono#error} with {@link RestServiceException}
	 */
	private Mono<Object> parseResponseBodyMono(String body, Class<?> responseType) {
		if (RestUtil.containsError(body, mapper)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, CHECK_ERROR_RESPONSE,
					"MOSIP error response body=" + body);
			return Mono.error(new RestServiceException(CLIENT_ERROR, body, readResponseBody(body, responseType)));
		}
		return Mono.just(readResponseBody(body, responseType));
	}

	/**
	 * Serializes the request body to JSON and normalizes MOSIP UTC timestamp fields.
	 *
	 * @param body request object (wrapper or DTO)
	 * @return JSON string
	 * @throws UncheckedIOException if Jackson serialization fails
	 */
	private String writeJsonBody(Object body) {
		try {
			JsonNode tree = mapper.valueToTree(body);
			if (tree.isObject()) {
				normalizeMosipUtcTimestamps((ObjectNode) tree);
			}
			return mapper.writeValueAsString(tree);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Appends literal {@code Z} to envelope/request timestamps missing the MOSIP UTC suffix.
	 * <p>
	 * Touches {@code requesttime} on the wrapper and {@code actionTimeStamp}/{@code timeStamp}
	 * on the nested {@code request} object when they match {@code yyyy-MM-dd'T'HH:mm:ss.SSS}.
	 * </p>
	 *
	 * @param node root JSON object of the outbound body
	 */
	private void normalizeMosipUtcTimestamps(ObjectNode node) {
		appendUtcZSuffix(node, "requesttime");
		if (node.has("request") && node.get("request").isObject()) {
			ObjectNode inner = (ObjectNode) node.get("request");
			appendUtcZSuffix(inner, "actionTimeStamp");
			appendUtcZSuffix(inner, "timeStamp");
		}
	}

	/**
	 * If {@code field} is a textual timestamp without trailing {@code Z}, appends {@code Z}.
	 *
	 * @param node  JSON object that may contain the field
	 * @param field property name to normalize
	 */
	private void appendUtcZSuffix(ObjectNode node, String field) {
		JsonNode value = node.get(field);
		if (value != null && value.isTextual()) {
			String text = value.asText();
			if (!text.endsWith("Z") && MOSIP_UTC_MS_NO_Z.matcher(text).matches()) {
				node.put(field, text + "Z");
			}
		}
	}

	/**
	 * Returns request JSON suitable for INFO logs with ciphertext fields shortened.
	 * <p>
	 * Redacts nested {@code request.data} and {@code request.salt} to length-only placeholders.
	 * </p>
	 *
	 * @param json full request JSON
	 * @return redacted JSON, or the original string if parsing fails
	 */
	private String summarizeRequestBody(String json) {
		try {
			ObjectNode node = mapper.readValue(json, ObjectNode.class);
			if (node.has("request") && node.get("request").isObject()) {
				ObjectNode inner = (ObjectNode) node.get("request");
				redactBinaryField(inner, "data");
				redactBinaryField(inner, "salt");
			}
			return mapper.writeValueAsString(node);
		} catch (IOException e) {
			return json;
		}
	}

	/**
	 * Replaces a textual field with {@code <redacted len=N>} for log safety.
	 *
	 * @param node  parent JSON object
	 * @param field field name to redact when present and textual
	 */
	private void redactBinaryField(ObjectNode node, String field) {
		if (node.has(field) && node.get(field).isTextual()) {
			node.put(field, "<redacted len=" + node.get(field).asText().length() + ">");
		}
	}

	/**
	 * Deserializes a JSON body to {@code responseType} using Jackson 2.
	 *
	 * @param body         raw JSON
	 * @param responseType target class
	 * @return deserialized object
	 * @throws UncheckedIOException if parsing fails
	 */
	private Object readResponseBody(String body, Class<?> responseType) {
		try {
			return mapper.readValue(body, responseType);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Walks the cause chain and returns the first {@link RestServiceException}, if any.
	 *
	 * @param error throwable from a blocked reactive call
	 * @return nested {@link RestServiceException}, or {@code null}
	 */
	private static RestServiceException unwrapRestServiceException(Throwable error) {
		for (Throwable current = error; current != null; current = current.getCause()) {
			if (current instanceof RestServiceException restServiceException) {
				return restServiceException;
			}
		}
		return null;
	}

	/**
	 * Validates that a deserialized MOSIP response does not contain a non-empty {@code errors} array.
	 * <p>
	 * Used after sync decode for non-String / non-{@code byte[]} response types. A null response
	 * or present {@code errors} array throws {@link RestServiceException} with {@code CLIENT_ERROR}.
	 * </p>
	 *
	 * @param response     deserialized response object
	 * @param responseType expected type (used when re-reading the error body)
	 * @throws RestServiceException if the body is null, contains MOSIP errors, or cannot be inspected
	 */
	private void checkErrorResponse(Object response, Class<?> responseType) throws RestServiceException {
		try {
			if (Objects.nonNull(response)) {
				ObjectNode responseNode = mapper.readValue(mapper.writeValueAsBytes(response), ObjectNode.class);
				if (responseNode.has(ERRORS) && !responseNode.get(ERRORS).isNull() && responseNode.get(ERRORS).isArray()
						&& responseNode.get(ERRORS).size() > 0) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, CHECK_ERROR_RESPONSE,
							THROWING_REST_SERVICE_EXCEPTION + UNKNOWN_ERROR_LOG
									+ responseNode.get(ERRORS).toString());
					throw new RestServiceException(CLIENT_ERROR, responseNode.toString(),
							mapper.readValue(responseNode.toString().getBytes(), responseType));
				}
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, CHECK_ERROR_RESPONSE,
						THROWING_REST_SERVICE_EXCEPTION + UNKNOWN_ERROR_LOG + "Response is null");
				throw new RestServiceException(CLIENT_ERROR);
			}
		} catch (IOException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, CHECK_ERROR_RESPONSE,
					THROWING_REST_SERVICE_EXCEPTION + UNKNOWN_ERROR_LOG + e.getMessage());
			throw new RestServiceException(UNKNOWN_ERROR, e);
		}
	}

	/**
	 * Maps HTTP 4xx/5xx {@link WebClientResponseException} to ID-Repository exceptions.
	 * <table>
	 *   <caption>Status to exception mapping</caption>
	 *   <tr><th>Status</th><th>Result</th></tr>
	 *   <tr><td>401</td><td>{@link AuthenticationException} (no retry)</td></tr>
	 *   <tr><td>403</td><td>{@link IdRepoRetryException} wrapping {@link AuthenticationException}</td></tr>
	 *   <tr><td>other 4xx</td><td>{@link RestServiceException} {@code CLIENT_ERROR}</td></tr>
	 *   <tr><td>5xx</td><td>{@link IdRepoRetryException} wrapping {@code SERVER_ERROR}</td></tr>
	 * </table>
	 *
	 * @param e            WebClient status exception with response body
	 * @param responseType type used when parsing the error body into a typed payload
	 * @return {@link RestServiceException} only when body parsing fails after status handling
	 * @throws RestServiceException       for non-retryable client errors
	 * @throws AuthenticationException    for HTTP 401
	 * @throws IdRepoRetryException       for 403 and 5xx (propagates as runtime for retry)
	 */
	private RestServiceException handleStatusError(WebClientResponseException e, Class<?> responseType)
			throws RestServiceException {
		try {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER,
					"request failed with status code :" + e.getStatusCode().value(), "\n\n" + e.getResponseBodyAsString());
			if (e.getStatusCode().is4xxClientError()) {
				if (e.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
					List<ServiceError> errorList = ExceptionUtils.getServiceErrorList(e.getResponseBodyAsString());
					throw new AuthenticationException(errorList.get(0).getErrorCode(), errorList.get(0).getMessage(),
							e.getStatusCode().value());
				} else if (e.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
					List<ServiceError> errorList = ExceptionUtils.getServiceErrorList(e.getResponseBodyAsString());
					throw new IdRepoRetryException(new AuthenticationException(errorList.get(0).getErrorCode(),
							errorList.get(0).getMessage(), e.getStatusCode().value()));
				} else {
					mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_HANDLE_STATUS_ERROR,
							"Status error - returning RestServiceException - CLIENT_ERROR ");
					throw new RestServiceException(CLIENT_ERROR, e.getResponseBodyAsString(),
							mapper.readValue(e.getResponseBodyAsString().getBytes(), responseType));
				}
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_HANDLE_STATUS_ERROR,
						"Status error - returning RestServiceException - SERVER_ERROR");
				throw new IdRepoRetryException(new RestServiceException(SERVER_ERROR, e.getResponseBodyAsString(),
						mapper.readValue(e.getResponseBodyAsString().getBytes(), responseType)));
			}
		} catch (IOException ex) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), CLASS_REST_HELPER, METHOD_HANDLE_STATUS_ERROR,
					ex.getMessage());
			return new RestServiceException(UNKNOWN_ERROR, ex);
		}
	}
}