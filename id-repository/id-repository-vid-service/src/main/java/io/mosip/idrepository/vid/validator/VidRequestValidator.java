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
 * Validates VID request types: CREATE, UPDATE, DEACTIVATE, REACTIVATE.
 * Preserves backward compatible behavior & improves VID type lookup performance.
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

	/** Cached VID types for performance */
	private volatile Set<String> cachedVidTypes;

	@PostConstruct
	public void initCache() {
		cachedVidTypes = policyProvider.getAllVidTypes();
	}

	/** Manual refresh if policies change */
	public void refreshVidTypesCache() {
		cachedVidTypes = policyProvider.getAllVidTypes();
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return RequestWrapper.class.isAssignableFrom(clazz);
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

		if (requestId == null) return;

		switch (requestId) {

			case CREATE:
				validateVidType(request.getVidType(), errors, currentUser);

				// ✅ Backward Compatible: Always validate UIN
				// ✅ Performance: skip if 'request' field already has validation error
				if (!errors.hasFieldErrors(REQUEST)) {
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

	private void validateVidType(String vidType, Errors errors, String currentUser) {
		if (vidType == null) {
			logAndReject(errors, currentUser, "validateVidType", "vidType is null",
					MISSING_INPUT_PARAMETER, VID_TYPE);
			return;
		}
		if (!cachedVidTypes.contains(vidType.toUpperCase())) {
			logAndReject(errors, currentUser, "validateVidType",
					"Invalid vidType: " + vidType, INVALID_INPUT_PARAMETER, VID_TYPE);
		}
	}

	private void validateUin(String uin, Errors errors, String currentUser) {
		if (uin == null) {
			logAndReject(errors, currentUser, "validateUin", "uin is null",
					MISSING_INPUT_PARAMETER, UIN);
			return;
		}
		try {
			uinValidator.validateId(uin);
		} catch (InvalidIDException e) {
			logAndReject(errors, currentUser, "validateUin", e.getMessage(),
					INVALID_INPUT_PARAMETER, UIN);
		}
	}

	private void validateRequest(VidRequestDTO request, Errors errors) {
		if (request == null) {
			logAndReject(errors, IdRepoSecurityManager.getUser(), "validateRequest", "request is null",
					MISSING_INPUT_PARAMETER, REQUEST);
		}
	}

	private void validateStatus(String vidStatus, Errors errors, String currentUser) {
		if (vidStatus == null) {
			logAndReject(errors, currentUser, "validateStatus", "Status is null",
					MISSING_INPUT_PARAMETER, STATUS_FIELD);
		} else if (!allowedStatus.contains(vidStatus)) {
			logAndReject(errors, currentUser, "validateStatus", "Invalid vidStatus",
					INVALID_INPUT_PARAMETER, STATUS_FIELD);
		}
	}

	public void validateVid(String vid) {
		vidValidator.validateId(vid);
	}

	private void logAndReject(Errors errors, String user, String method, String message,
							  IdRepoErrorConstants errorCode, String fieldName) {
		mosipLogger.error(user, getClass().getSimpleName(), method, message);
		errors.rejectValue(REQUEST, errorCode.getErrorCode(),
				String.format(errorCode.getErrorMessage(), fieldName));
	}
}
