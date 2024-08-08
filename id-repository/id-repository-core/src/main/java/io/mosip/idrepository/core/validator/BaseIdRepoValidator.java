package io.mosip.idrepository.core.validator;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.MISSING_INPUT_PARAMETER;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.beans.factory.annotation.Value;

import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;

/**
 * The Class BaseIdRepoValidator - base validator to validate common fields from
 * request in Identity and VID service.
 *
 * @author Manoj SP
 * @author Prem Kumar
 */
@Component
public abstract class BaseIdRepoValidator {

	/** The Constant BASE_ID_REPO_VALIDATOR. */
	private static final String BASE_ID_REPO_VALIDATOR = "BaseIdRepoValidator";

	/** The mosip logger. */
	Logger mosipLogger = IdRepoLogger.getLogger(BaseIdRepoValidator.class);

	/** The Constant TIMESTAMP. */
	private static final String REQUEST_TIME = "requesttime";

	/** The Constant VER. */
	private static final String VER = "version";

	/** The Constant ID. */
	protected static final String ID = "id";

	/** The id. */
	@Resource
	protected Map<String, String> id;

	@Value("${mosip.idrepo.identity.max-request-time-deviation-seconds:60}")
	private int maxRequestTimeDeviationSeconds;

	/**
	 * Validate request time.
	 *
	 * @param reqTime the timestamp
	 * @param errors  the errors
	 */
	protected void validateReqTime(LocalDateTime reqTime, Errors errors) {
		if (Objects.isNull(reqTime)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateReqTime",
					"requesttime is null");
			errors.rejectValue(REQUEST_TIME, MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), REQUEST_TIME));
		} else {
			LocalDateTime currentUtcTime = DateUtils.getUTCCurrentDateTime();
			LocalDateTime adjustedCurrentUtcTime = currentUtcTime.plusSeconds(EnvUtil.getDateTimeAdjustment());

			int maxDeviationSeconds = maxRequestTimeDeviationSeconds;

			if (DateUtils.after(reqTime, adjustedCurrentUtcTime.plusSeconds(maxDeviationSeconds))
					|| DateUtils.before(reqTime, adjustedCurrentUtcTime.minusSeconds(maxDeviationSeconds))) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateReqTime",
						"requesttime is outside the allowed deviation");
				mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateReqTime",
						"reqTime: " + reqTime.toString());
				mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateReqTime",
						"vmTime: " + adjustedCurrentUtcTime.toString());
				String deviationMessage = String.format(
						"%s - the timestamp value can be at most %d seconds before and after the current time.",
						REQUEST_TIME, maxDeviationSeconds);
				errors.rejectValue(REQUEST_TIME, INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), deviationMessage));
			}
		}

	}

	/**
	 * Validate version.
	 *
	 * @param ver    the ver
	 * @param errors the errors
	 */
	protected void validateVersion(String ver, Errors errors) {
		if (Objects.isNull(ver)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateVersion",
					"version is null");
			errors.rejectValue(VER, MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), VER));
		} else if ((!Pattern.compile(EnvUtil.getVersionPattern()).matcher(ver)
				.matches())) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateVersion",
					"version is InValid");
			errors.rejectValue(VER, INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), VER));
		}
	}

	/**
	 * This method will validate the id field in the request.
	 *
	 * @param id        the id
	 * @param operation the operation
	 * @throws IdRepoAppException the id repo app exception
	 */
	public void validateId(String id, String operation) throws IdRepoAppException {
		if (Objects.isNull(id)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateId", "id is null");
			throw new IdRepoAppException(MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), ID));
		} else if (!this.id.get(operation).equals(id)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), BASE_ID_REPO_VALIDATOR, "validateId", "id is invalid");
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), ID));
		}
	}
}
