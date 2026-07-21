package io.mosip.idrepository.core.validator;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.MISSING_INPUT_PARAMETER;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DataValidationUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;

/**
 * Abstract base for MOSIP request-envelope validation shared by identity and VID APIs.
 *
 * <p>
 * Validates the common wrapper fields {@code requesttime}, {@code version}, and {@code id}
 * against configuration (operation-id maps, application version, clock-skew window, and
 * version regex). Subclasses in {@code id-repository-service} add domain-specific rules
 * (UIN/VID format, status allow-lists, identity schema fields) on top of these checks.
 * </p>
 *
 * <h2>Subclasses (service module)</h2>
 * <ul>
 *   <li>{@code io.mosip.idrepository.identity.validator.IdRequestValidator}</li>
 *   <li>{@code io.mosip.idrepository.vid.validator.VidRequestValidator}</li>
 *   <li>Other identity validators that reuse {@link #validateId(String, String)} before
 *       Spring binding</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li>{@link #setOperationIds(Map)} — operation key → expected request {@code id}
 *       (e.g. {@code mosip.idrepo.vid.id.deactivate} → {@code mosip.vid.deactivate})</li>
 *   <li>{@link #setExpectedApplicationVersion(String)} — exact version from
 *       {@code mosip.idrepo.vid.application.version} or identity equivalent</li>
 *   <li>{@code mosip.idrepo.identity.max-request-time-deviation-seconds} (default
 *       {@code 60}) — allowed clock skew for {@code requesttime}</li>
 *   <li>{@link EnvUtil#getVersionPattern()} — regex for version format</li>
 *   <li>{@link EnvUtil#getDateTimeAdjustment()} — seconds added to UTC “now” before
 *       skew comparison (config-server clock adjustment)</li>
 * </ul>
 *
 * <h2>Error reporting styles</h2>
 * <ul>
 *   <li>{@link #validateReqTime} / {@link #validateVersion} — accumulate into Spring
 *       {@link Errors}; callers typically convert via {@link DataValidationUtil}</li>
 *   <li>{@link #validateId} — throws {@link IdRepoAppException} immediately (used when
 *       controllers validate {@code id} before full binding)</li>
 * </ul>
 *
 * <h2>Error codes</h2>
 * <p>
 * Uses {@link IdRepoErrorConstants#MISSING_INPUT_PARAMETER} and
 * {@link IdRepoErrorConstants#INVALID_INPUT_PARAMETER}. Detail text is built by
 * {@link IdRepoValidationMessageHelper}; codes must remain stable for API clients.
 * </p>
 *
 * @author Manoj SP
 * @author Prem Kumar
 * @see IdRepoValidationMessageHelper
 * @see DataValidationUtil
 * @see EnvUtil
 */
@Component
public abstract class BaseIdRepoValidator {

	/** Logger category / method-context name for structured error logs. */
	private static final String BASE_ID_REPO_VALIDATOR = "BaseIdRepoValidator";

	/** Application logger for validation failures. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(BaseIdRepoValidator.class);

	/** JSON / DTO field name for the request timestamp. */
	private static final String REQUEST_TIME = "requesttime";

	/** JSON / DTO field name for the API version. */
	private static final String VER = "version";

	/**
	 * JSON / DTO field name for the operation identifier.
	 * <p>
	 * Exposed as {@code protected} so subclasses can reject the same field via
	 * {@link Errors#rejectValue(String, String, String)}.
	 * </p>
	 */
	protected static final String ID = "id";

	/**
	 * Map of operation name to expected {@code id} value from config server.
	 * <p>
	 * Example: key {@code deactivate} → value {@code mosip.vid.deactivate} from property
	 * {@code mosip.idrepo.vid.id.deactivate}. Bound by subclasses via
	 * {@link #setOperationIds(Map)}.
	 * </p>
	 */
	protected Map<String, String> id;

	/**
	 * Expected request {@code version} from config (e.g.
	 * {@code mosip.idrepo.vid.application.version}).
	 * <p>
	 * When non-blank, {@link #validateVersion(String, Errors)} requires an exact match in
	 * addition to the version regex from {@link EnvUtil#getVersionPattern()}.
	 * </p>
	 */
	protected String expectedApplicationVersion;

	/**
	 * Maximum allowed clock skew for {@code requesttime}, in seconds.
	 * <p>
	 * Property: {@link IdRepoConstants#MAX_REQUEST_TIME_DEVIATION_SECONDS}
	 * ({@code mosip.idrepo.identity.max-request-time-deviation-seconds}), default
	 * {@code 60}.
	 * </p>
	 */
	@Value("${" + IdRepoConstants.MAX_REQUEST_TIME_DEVIATION_SECONDS + ":60}")
	private int maxRequestTimeDeviationSeconds;

	/**
	 * Binds the operation-key → expected-{@code id} map used by {@link #validateId}.
	 * <p>
	 * Called from subclass {@code @PostConstruct} or constructor after reading the
	 * identity or VID id map from configuration.
	 * </p>
	 *
	 * @param operationIds map of operation key to request {@code id} value; may be
	 *                     {@code null} (then every {@link #validateId} call fails as
	 *                     unconfigured)
	 */
	protected void setOperationIds(Map<String, String> operationIds) {
		this.id = operationIds;
	}

	/**
	 * Sets the exact application version expected in the request envelope.
	 *
	 * @param expectedApplicationVersion configured version string (e.g. {@code v1});
	 *                                   blank or {@code null} disables exact-match check
	 *                                   (regex-only validation remains)
	 */
	protected void setExpectedApplicationVersion(String expectedApplicationVersion) {
		this.expectedApplicationVersion = expectedApplicationVersion;
	}

	/**
	 * Validates that {@code requesttime} is present and within the configured deviation
	 * window of adjusted UTC “now”.
	 * <p>
	 * Comparison baseline is {@link DateUtils2#getUTCCurrentDateTime()} plus
	 * {@link EnvUtil#getDateTimeAdjustment()} seconds. The request is rejected when it
	 * is more than {@link #maxRequestTimeDeviationSeconds} before or after that baseline.
	 * </p>
	 * <p>
	 * On failure, rejects field {@code requesttime} on {@code errors} with
	 * {@link IdRepoErrorConstants#MISSING_INPUT_PARAMETER} (null) or
	 * {@link IdRepoErrorConstants#INVALID_INPUT_PARAMETER} (out of window). Detail text
	 * comes from {@link IdRepoValidationMessageHelper#missingRequestTime()} or
	 * {@link IdRepoValidationMessageHelper#invalidRequestTimeDeviation(String, int)}.
	 * </p>
	 *
	 * @param reqTime parsed request timestamp from the envelope; may be {@code null}
	 * @param errors  Spring binding-errors collector; must not be {@code null}
	 */
	protected void validateReqTime(LocalDateTime reqTime, Errors errors) {
		if (Objects.isNull(reqTime)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateReqTime",
					"requesttime is null");
			errors.rejectValue(REQUEST_TIME, MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(),
							IdRepoValidationMessageHelper.missingRequestTime()));
		} else {
			LocalDateTime currentUtcTime = DateUtils2.getUTCCurrentDateTime();
			LocalDateTime adjustedCurrentUtcTime = currentUtcTime.plusSeconds(EnvUtil.getDateTimeAdjustment());

			int maxDeviationSeconds = maxRequestTimeDeviationSeconds;

			if (DateUtils2.after(reqTime, adjustedCurrentUtcTime.plusSeconds(maxDeviationSeconds))
					|| DateUtils2.before(reqTime, adjustedCurrentUtcTime.minusSeconds(maxDeviationSeconds))) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateReqTime",
						"requesttime is outside the allowed deviation");
				mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateReqTime",
						"reqTime: " + reqTime.toString());
				mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateReqTime",
						"vmTime: " + adjustedCurrentUtcTime.toString());
				errors.rejectValue(REQUEST_TIME, INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), IdRepoValidationMessageHelper
								.invalidRequestTimeDeviation(reqTime.toString(), maxDeviationSeconds)));
			}
		}

	}

	/**
	 * Validates that {@code version} is present, matches
	 * {@link EnvUtil#getVersionPattern()}, and (when configured) equals
	 * {@link #expectedApplicationVersion}.
	 * <p>
	 * On failure, rejects field {@code version} with
	 * {@link IdRepoErrorConstants#MISSING_INPUT_PARAMETER} or
	 * {@link IdRepoErrorConstants#INVALID_INPUT_PARAMETER}. Detail text from
	 * {@link IdRepoValidationMessageHelper#missingVersion(String, String)} or
	 * {@link IdRepoValidationMessageHelper#invalidVersion(String, String, String)}.
	 * </p>
	 *
	 * @param ver    API version string from the request envelope; may be {@code null}
	 * @param errors Spring binding-errors collector; must not be {@code null}
	 */
	protected void validateVersion(String ver, Errors errors) {
		String versionPattern = EnvUtil.getVersionPattern();
		if (Objects.isNull(ver)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateVersion",
					"version is null");
			errors.rejectValue(VER, MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(),
							IdRepoValidationMessageHelper.missingVersion(versionPattern, expectedApplicationVersion)));
		} else if (!Pattern.compile(versionPattern).matcher(ver).matches()
				|| (expectedApplicationVersion != null && !expectedApplicationVersion.isBlank()
						&& !expectedApplicationVersion.equals(ver))) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateVersion",
					"version is InValid");
			errors.rejectValue(VER, INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(),
							IdRepoValidationMessageHelper.invalidVersion(ver, versionPattern, expectedApplicationVersion)));
		}
	}

	/**
	 * Validates that {@code id} is present and equals the configured value for
	 * {@code operation} in {@link #id}.
	 * <p>
	 * Unlike {@link #validateReqTime} / {@link #validateVersion}, this method throws
	 * {@link IdRepoAppException} instead of accumulating Spring {@link Errors}. Controllers
	 * typically call it early to reject unknown operation identifiers before full DTO
	 * binding.
	 * </p>
	 * <p>
	 * Failure cases:
	 * </p>
	 * <ul>
	 *   <li>{@code id} is {@code null} → {@link IdRepoErrorConstants#MISSING_INPUT_PARAMETER}</li>
	 *   <li>operation not in map, or value mismatch →
	 *       {@link IdRepoErrorConstants#INVALID_INPUT_PARAMETER}</li>
	 * </ul>
	 *
	 * @param id        operation identifier from the request envelope
	 * @param operation operation key used to look up the expected id (e.g. {@code create},
	 *                  {@code deactivate})
	 * @throws IdRepoAppException if {@code id} is null, the operation is not configured, or
	 *                            the value does not match the configured expected id
	 */
	public void validateId(String id, String operation) throws IdRepoAppException {
		if (Objects.isNull(id)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateId", "id is null");
			throw new IdRepoAppException(MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(),
							IdRepoValidationMessageHelper.missingId(operation, this.id)));
		}
		String expectedId = this.id != null ? this.id.get(operation) : null;
		if (expectedId == null) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateId",
					"operation id not configured: " + operation);
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(),
							IdRepoValidationMessageHelper.invalidId(id, operation, this.id)));
		}
		if (!expectedId.equals(id)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateId", "id is invalid");
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(),
							IdRepoValidationMessageHelper.invalidId(id, operation, this.id)));
		}
	}
}
