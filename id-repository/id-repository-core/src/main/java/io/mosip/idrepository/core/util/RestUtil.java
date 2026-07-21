package io.mosip.idrepository.core.util;

import static io.mosip.idrepository.core.constant.IdRepoConstants.ERRORS;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Stateless helpers for parsing MOSIP-style REST error payloads from raw JSON response
 * bodies.
 *
 * <p>
 * Outbound calls in ID Repository expect error responses shaped as
 * {@code {"errors": [...]}} or, in some legacy paths, a single {@code errors} map with
 * {@code errorCode} and {@code message}. These utilities centralise Jackson parsing so
 * callers such as {@link RestHelper} can detect failures without duplicating logic.
 * </p>
 *
 * <h2>Supported shapes</h2>
 * <ul>
 *   <li>Standard kernel list: {@code "errors": [ { "errorCode", "message" }, ... ]}</li>
 *   <li>Legacy map: {@code "errors": { "errorCode", "message" }}</li>
 * </ul>
 *
 * <h2>Failure policy</h2>
 * <p>
 * Malformed JSON is logged (body abbreviated) and treated as “no errors” /
 * empty list — callers must not assume parse failure means a successful remote call.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * if (RestUtil.containsError(body, mapper)) {
 *     List&lt;ServiceError&gt; errors = RestUtil.getErrorList(body, mapper);
 *     // map to RestServiceException / IdRepoAppException
 * }
 * </pre>
 *
 * <h2>IDA compatibility</h2>
 * <p>
 * Listed as an IDA-facing utility in the core module guide. Keep public method signatures
 * stable across releases.
 * </p>
 *
 * @see RestHelper
 * @see IdRepoConstants#ERRORS
 * @see ServiceError
 * @see ExceptionUtils#getServiceErrorList(String)
 */
public final class RestUtil {

	/** Logger scoped to REST response parsing utilities. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(RestUtil.class);

	/**
	 * Prevents instantiation; use static helpers only.
	 */
	private RestUtil() {
	}

	/**
	 * Parses a JSON response and returns the {@code errors} entry when it is a non-empty
	 * list.
	 * <p>
	 * Malformed JSON or responses without a populated {@code errors} array yield
	 * {@link Optional#empty()}; parse failures are logged and treated as non-error
	 * responses.
	 * </p>
	 *
	 * @param response raw HTTP response body as a string; may be {@code null} (parse fails)
	 * @param mapper   Jackson {@link ObjectMapper} used to deserialise the body
	 * @return optional map entry whose key is {@link IdRepoConstants#ERRORS} and whose
	 *         value is a non-empty {@link List}; empty when absent or on parse failure
	 */
	public static Optional<Entry<String, Object>> getError(String response, ObjectMapper mapper) {
		try {
			Map<String, Object> readValue = mapper.readValue(response.getBytes(), Map.class);
			return readValue.entrySet().stream()
						.filter(entry -> entry.getKey().equals(ERRORS)
											&& !Objects.isNull(entry.getValue()) 
											&& (entry.getValue() instanceof List && !((List<?>)entry.getValue()).isEmpty()))
						.findAny();
		} catch (IOException e) {
			mosipLogger.warn("Failed to parse error response (body="
					+ abbreviate(response, 500) + "): " + ExceptionUtils.getStackTrace(e));
			return Optional.empty();
		}
	}

	/**
	 * Extracts a list of {@link ServiceError} instances from a MOSIP error response body.
	 * <p>
	 * If {@code errors} is a map with {@code errorCode} / {@code message}, returns a
	 * single-element list. Otherwise delegates to
	 * {@link ExceptionUtils#getServiceErrorList(String)} for the standard list format.
	 * JSON parse failures log a warning and return an empty list.
	 * </p>
	 *
	 * @param responseBodyAsString raw HTTP response body
	 * @param mapper               Jackson {@link ObjectMapper} for initial map parsing
	 * @return list of {@link ServiceError}; never {@code null}, may be empty on parse
	 *         failure or when no errors are present
	 * @see ExceptionUtils#getServiceErrorList(String)
	 */
	@SuppressWarnings("unchecked")
	public static List<ServiceError> getErrorList(String responseBodyAsString, ObjectMapper mapper) {
		try {
			Map<String, Object> responseMap = mapper.readValue(responseBodyAsString.getBytes(), Map.class);
			Object errors = responseMap.get("errors");
			if(errors instanceof Map) {
				Map<String, Object> errorMap = (Map<String, Object>) errors;
				return List.of(new ServiceError((String)errorMap.get("errorCode"), (String)errorMap.get("message")));
			}
		} catch (IOException e) {
			mosipLogger.warn("Failed to parse error response (body="
					+ abbreviate(responseBodyAsString, 500) + "): " + ExceptionUtils.getStackTrace(e));
			return Collections.emptyList();
		}
		
		return ExceptionUtils.getServiceErrorList(responseBodyAsString);
	}

	/**
	 * Returns {@code true} when the JSON response body contains a non-empty {@code errors}
	 * list.
	 * <p>
	 * Equivalent to {@code getError(response, mapper).isPresent()} but avoids allocating an
	 * {@link Optional} when only a boolean check is needed.
	 * </p>
	 *
	 * @param response raw HTTP response body as a string
	 * @param mapper   Jackson {@link ObjectMapper} used to deserialise the body
	 * @return {@code true} if the body parses as JSON and contains a non-empty
	 *         {@code errors} list; {@code false} on parse failure (logged) or when no
	 *         errors are present
	 * @see #getError(String, ObjectMapper)
	 * @see RestHelper
	 */
	@SuppressWarnings("unchecked")
	public static boolean containsError(String response, ObjectMapper mapper) {
		try {
			Map<String, Object> readValue = mapper.readValue(response.getBytes(), Map.class);
			return readValue.entrySet().stream()
						.anyMatch(entry -> entry.getKey().equals(ERRORS)
											&& !Objects.isNull(entry.getValue()) 
											&& (entry.getValue() instanceof List && !((List<?>)entry.getValue()).isEmpty()));
		} catch (IOException e) {
			mosipLogger.warn("Failed to parse error response (body="
					+ abbreviate(response, 500) + "): " + ExceptionUtils.getStackTrace(e));
			return false;
		}
	}

	/**
	 * Truncates {@code text} for safe logging when response bodies are large.
	 *
	 * @param text   raw text; may be {@code null}
	 * @param maxLen maximum characters to keep before appending a truncation marker
	 * @return abbreviated text, {@code "null"}, or the original string when short enough
	 */
	private static String abbreviate(String text, int maxLen) {
		if (text == null) {
			return "null";
		}
		if (text.length() <= maxLen) {
			return text;
		}
		return text.substring(0, maxLen) + "... <truncated len=" + text.length() + ">";
	}
}
