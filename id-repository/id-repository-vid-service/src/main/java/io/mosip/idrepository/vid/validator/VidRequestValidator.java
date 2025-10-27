package io.mosip.idrepository.vid.validator;

import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.MISSING_INPUT_PARAMETER;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.dto.VidRequestDTO;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.validator.BaseIdRepoValidator;
import io.mosip.idrepository.vid.provider.VidPolicyProvider;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.idvalidator.spi.VidValidator;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Backward-compatible, optimized validator for VID requests.
 * Keeps original error codes/messages for test compatibility.
 */
@Component
@ConfigurationProperties("mosip.idrepo.vid")
public class VidRequestValidator extends BaseIdRepoValidator implements Validator {

	private static final String REACTIVATE = "reactivate";
	private static final String DEACTIVATE = "deactivate";
	private static final String CREATE = "create";
	private static final String UPDATE = "update";

	private static final String VID_TYPE = "vidType";
	private static final String STATUS_FIELD = "vidStatus";
	private static final String REQUEST = "request";
	private static final String UIN = "UIN";

	private final Logger mosipLogger = IdRepoLogger.getLogger(VidRequestValidator.class);

	@Autowired
	private VidPolicyProvider policyProvider;

	@Autowired
	private VidValidator<String> vidValidator;

	@Autowired
	private UinValidator<String> uinValidator;

	@Resource
	private List<String> allowedStatus;

	private volatile Set<String> cachedVidTypes;

	@PostConstruct
	public void initCache() {
		cachedVidTypes = policyProvider.getAllVidTypes();
	}

	public void refreshVidTypesCache() {
		cachedVidTypes = policyProvider.getAllVidTypes();
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return clazz.isAssignableFrom(RequestWrapper.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void validate(@NonNull Object target, Errors errors) {
		RequestWrapper<VidRequestDTO> requestWrapper = (RequestWrapper<VidRequestDTO>) target;

		validateReqTime(requestWrapper.getRequesttime(), errors);
		validateVersion(requestWrapper.getVersion(), errors);
		validateRequest(requestWrapper.getRequest(), errors);

		if (errors.hasErrors()) return;

		String requestId = requestWrapper.getId();
		VidRequestDTO request = requestWrapper.getRequest();
		String currentUser = IdRepoSecurityManager.getUser();

		if (Objects.isNull(requestId)) return;

		switch (requestId) {
			case CREATE:
				validateVidType(request.getVidType(), errors, currentUser);
				if (!errors.hasErrors()) {
					validateUin(request.getUin(), errors, currentUser);
				}
				break;

			case UPDATE:
				validateStatus(request.getVidStatus(), errors, currentUser);
				break;

			case DEACTIVATE:
			case REACTIVATE:
				validateUin(request.getUin(), errors, currentUser);
				break;

			default:
				mosipLogger.warn(currentUser, getClass().getSimpleName(), "validate",
						"Unknown requestId ignored: " + requestId);
				break;
		}
	}

	private void validateVidType(String vidType, Errors errors, String user) {
		if (Objects.isNull(vidType)) {
			// same error format as old version
			mosipLogger.error(user, getClass().getSimpleName(), "validateVidType", "vidType is null");
			errors.rejectValue(REQUEST, MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), VID_TYPE));
		} else if (!cachedVidTypes.contains(vidType.toUpperCase())) {
			mosipLogger.error(user, getClass().getSimpleName(), "validateVidType",
					"vidType is invalid - " + vidType);
			errors.rejectValue(REQUEST, INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), VID_TYPE));
		}
	}

	private void validateUin(String uin, Errors errors, String user) {
		if (Objects.isNull(uin)) {
			mosipLogger.error(user, getClass().getSimpleName(), "validateUin", "uin is null");
			errors.rejectValue(REQUEST, MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), UIN));
		} else {
			try {
				uinValidator.validateId(uin);
			} catch (InvalidIDException e) {
				mosipLogger.error(user, getClass().getSimpleName(), "validateUin", e.getMessage());
				errors.rejectValue(REQUEST, INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), UIN));
			}
		}
	}

	private void validateRequest(VidRequestDTO request, Errors errors) {
		if (Objects.isNull(request)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), getClass().getSimpleName(),
					"validateRequest", "request is null");
			errors.rejectValue(REQUEST, MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), REQUEST));
		}
	}

	private void validateStatus(String vidStatus, Errors errors, String user) {
		if (Objects.isNull(vidStatus)) {
			mosipLogger.error(user, getClass().getSimpleName(), "validateStatus", "Status is null");
			errors.rejectValue(REQUEST, MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), STATUS_FIELD));
		} else if (!allowedStatus.contains(vidStatus)) {
			mosipLogger.error(user, getClass().getSimpleName(), "validateStatus", "Status is invalid");
			errors.rejectValue(REQUEST, INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), STATUS_FIELD));
		}
	}

	public void validateVid(String vid) {
		vidValidator.validateId(vid);
	}
}
