package io.mosip.idrepository.core.validator;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;

/**
 * Builds descriptive ID Repository validation error <em>detail</em> strings.
 *
 * <p>
 * MOSIP error messages from {@link IdRepoErrorConstants} typically contain a
 * {@code %s} placeholder for a field-specific explanation. This helper formats that
 * payload so operators see expected values, allow-lists, and clock-skew windows instead
 * of bare field names. It does <strong>not</strong> choose error codes — callers still
 * use {@link IdRepoErrorConstants#MISSING_INPUT_PARAMETER},
 * {@link IdRepoErrorConstants#INVALID_INPUT_PARAMETER}, etc.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 * errors.rejectValue("version", INVALID_INPUT_PARAMETER.getErrorCode(),
 *     String.format(INVALID_INPUT_PARAMETER.getErrorMessage(),
 *         IdRepoValidationMessageHelper.invalidVersion(ver, pattern, expected)));
 *
 * throw new IdRepoAppException(MISSING_INPUT_PARAMETER.getErrorCode(),
 *     String.format(MISSING_INPUT_PARAMETER.getErrorMessage(),
 *         IdRepoValidationMessageHelper.missingId(operation, operationIds)));
 * </pre>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link BaseIdRepoValidator} — {@code id}, {@code version}, {@code requesttime}</li>
 *   <li>{@code IdRequestValidator} / {@code VidRequestValidator} — status and type
 *       allow-lists</li>
 *   <li>{@code VidServiceImpl} — UIN/VID invalid-detail messages</li>
 * </ul>
 *
 * <h2>Stability</h2>
 * <p>
 * Detail wording may change for clarity. Error <strong>codes</strong> must remain stable
 * for API clients and IDA. This class is a {@code final} utility with a private
 * constructor — all methods are {@code static}.
 * </p>
 *
 * @see BaseIdRepoValidator
 * @see IdRepoErrorConstants
 */
public final class IdRepoValidationMessageHelper {

	/**
	 * Prevents instantiation; use static factory methods only.
	 */
	private IdRepoValidationMessageHelper() {
	}

	/**
	 * Formats a collection of allowed values as a sorted JSON-like array string.
	 * <p>
	 * Null entries are dropped; duplicates are removed; values are wrapped in double
	 * quotes. Empty or {@code null} input yields {@code []}.
	 * </p>
	 * <p>
	 * Example: {@code ["ACTIVE", "INACTIVE"]}.
	 * </p>
	 *
	 * @param values allowed values; may be {@code null} or empty
	 * @return bracketed, comma-separated quoted list (never {@code null})
	 */
	public static String formatAllowedList(Collection<String> values) {
		if (values == null || values.isEmpty()) {
			return "[]";
		}
		return values.stream()
				.filter(Objects::nonNull)
				.distinct()
				.sorted()
				.map(value -> "\"" + value + "\"")
				.collect(Collectors.joining(", ", "[", "]"));
	}

	/**
	 * Detail text when the request {@code id} field is missing.
	 * <p>
	 * When {@code operation} is present in {@code operationIds}, includes the expected
	 * id for that operation and the full allow-list of configured ids. Otherwise lists
	 * only the allow-list (or {@code []} if the map is null/empty).
	 * </p>
	 *
	 * @param operation    operation key from config (e.g. {@code deactivate} for
	 *                     {@code mosip.idrepo.vid.id.deactivate})
	 * @param operationIds map of operation key to request {@code id} value from config
	 *                     server; may be {@code null}
	 * @return detail string for {@link IdRepoErrorConstants#MISSING_INPUT_PARAMETER}
	 */
	public static String missingId(String operation, Map<String, String> operationIds) {
		String expected = operationIds != null ? operationIds.get(operation) : null;
		String allowed = formatAllowedList(operationIds != null ? operationIds.values() : null);
		if (expected != null) {
			return String.format("id - missing; expected \"%s\" for \"%s\" operation; allowed values: %s",
					expected, operation, allowed);
		}
		return String.format("id - missing; allowed values: %s", allowed);
	}

	/**
	 * Detail text when the request {@code id} is present but wrong or the operation is
	 * not configured.
	 * <p>
	 * Includes the received value, expected value for {@code operation} when known, and
	 * the allow-list of all configured ids.
	 * </p>
	 *
	 * @param receivedId   {@code id} value from the request envelope
	 * @param operation    operation key used for lookup (e.g. {@code create})
	 * @param operationIds map of operation key to expected {@code id}; may be {@code null}
	 * @return detail string for {@link IdRepoErrorConstants#INVALID_INPUT_PARAMETER}
	 */
	public static String invalidId(String receivedId, String operation, Map<String, String> operationIds) {
		String expected = operationIds != null ? operationIds.get(operation) : null;
		String allowed = formatAllowedList(operationIds != null ? operationIds.values() : null);
		if (expected != null) {
			return String.format("id - received \"%s\"; expected \"%s\" for \"%s\" operation; allowed values: %s",
					receivedId, expected, operation, allowed);
		}
		return String.format("id - received \"%s\"; allowed values: %s", receivedId, allowed);
	}

	/**
	 * Detail text when the request {@code version} field is missing.
	 *
	 * @param pattern         version regex from {@code EnvUtil.getVersionPattern()}
	 * @param expectedVersion exact configured application version; blank or {@code null}
	 *                        omits the “expected …” clause
	 * @return detail string for {@link IdRepoErrorConstants#MISSING_INPUT_PARAMETER}
	 */
	public static String missingVersion(String pattern, String expectedVersion) {
		if (expectedVersion != null && !expectedVersion.isBlank()) {
			return String.format("version - missing; expected \"%s\"; must match pattern %s", expectedVersion,
					pattern);
		}
		return String.format("version - missing; must match pattern %s", pattern);
	}

	/**
	 * Detail text when the request {@code version} fails regex and/or exact-match checks.
	 *
	 * @param received        version string from the request
	 * @param pattern         version regex from configuration
	 * @param expectedVersion exact configured application version; blank or {@code null}
	 *                        omits the “expected …” clause
	 * @return detail string for {@link IdRepoErrorConstants#INVALID_INPUT_PARAMETER}
	 */
	public static String invalidVersion(String received, String pattern, String expectedVersion) {
		if (expectedVersion != null && !expectedVersion.isBlank()) {
			return String.format("version - received \"%s\"; expected \"%s\"; must match pattern %s", received,
					expectedVersion, pattern);
		}
		return String.format("version - received \"%s\"; must match pattern %s", received, pattern);
	}

	/**
	 * Generic “field missing” detail without an allow-list.
	 *
	 * @param fieldName DTO / JSON field name (e.g. {@code status})
	 * @return {@code "{fieldName} - missing"}
	 */
	public static String missingField(String fieldName) {
		return fieldName + " - missing";
	}

	/**
	 * Detail text when {@code requesttime} is null.
	 * <p>
	 * Mentions ISO-8601 UTC format with an example timestamp for API consumers.
	 * </p>
	 *
	 * @return detail string for {@link IdRepoErrorConstants#MISSING_INPUT_PARAMETER}
	 */
	public static String missingRequestTime() {
		return "requesttime - missing; provide the current UTC timestamp in ISO-8601 format (e.g. \"2026-07-06T14:07:54.716Z\")";
	}

	/**
	 * Detail text when {@code requesttime} is outside the allowed clock-skew window,
	 * including the received timestamp.
	 *
	 * @param received            request timestamp as string (typically
	 *                            {@link java.time.LocalDateTime#toString()})
	 * @param maxDeviationSeconds configured maximum absolute skew in seconds
	 * @return detail string for {@link IdRepoErrorConstants#INVALID_INPUT_PARAMETER}
	 */
	public static String invalidRequestTimeDeviation(String received, int maxDeviationSeconds) {
		return String.format(
				"requesttime - received \"%s\"; must be within ±%d seconds of the current UTC time",
				received, maxDeviationSeconds);
	}

	/**
	 * Detail text when {@code requesttime} is outside the allowed clock-skew window,
	 * without echoing the received value.
	 *
	 * @param maxDeviationSeconds configured maximum absolute skew in seconds
	 * @return detail string for {@link IdRepoErrorConstants#INVALID_INPUT_PARAMETER}
	 */
	public static String invalidRequestTimeDeviation(int maxDeviationSeconds) {
		return String.format("requesttime - must be within ±%d seconds of the current UTC time", maxDeviationSeconds);
	}

	/**
	 * Detail text when a field is missing and an allow-list of valid values is known.
	 *
	 * @param fieldName     DTO / JSON field name
	 * @param allowedValues permitted values (formatted via {@link #formatAllowedList})
	 * @return detail string for {@link IdRepoErrorConstants#MISSING_INPUT_PARAMETER}
	 */
	public static String missingWithAllowed(String fieldName, Collection<String> allowedValues) {
		return String.format("%s - missing; allowed values: %s", fieldName, formatAllowedList(allowedValues));
	}

	/**
	 * Detail text when a field value is not in the configured allow-list.
	 *
	 * @param fieldName     DTO / JSON field name
	 * @param received      value from the request
	 * @param allowedValues permitted values (formatted via {@link #formatAllowedList})
	 * @return detail string for {@link IdRepoErrorConstants#INVALID_INPUT_PARAMETER}
	 */
	public static String invalidWithAllowed(String fieldName, String received, Collection<String> allowedValues) {
		return String.format("%s - received \"%s\"; allowed values: %s", fieldName, received,
				formatAllowedList(allowedValues));
	}

	/**
	 * Detail text when UIN checksum / format validation fails.
	 *
	 * @return fixed detail string describing the 10-digit UIN checksum requirement
	 */
	public static String invalidUin() {
		return "UIN - must be a valid 10-digit UIN (checksum validation failed)";
	}

	/**
	 * Detail text when a UIN’s lifecycle status is not the configured “registered”
	 * (active) status required for an operation.
	 *
	 * @param receivedStatus           status found on the UIN record
	 * @param expectedRegisteredStatus configured registered status (e.g. {@code ACTIVATED})
	 * @return detail string comparing received vs expected status
	 */
	public static String invalidRegisteredUinStatus(String receivedStatus, String expectedRegisteredStatus) {
		return String.format("status \"%s\"; expected registered UIN status \"%s\"", receivedStatus,
				expectedRegisteredStatus);
	}
}