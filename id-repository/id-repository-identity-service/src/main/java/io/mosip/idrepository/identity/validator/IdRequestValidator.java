package io.mosip.idrepository.identity.validator;

import static io.mosip.idrepository.core.constant.IdRepoConstants.AUTH_TYPE_SEPERATOR;
import static io.mosip.idrepository.core.constant.IdRepoConstants.CREATE;
import static io.mosip.idrepository.core.constant.IdRepoConstants.ROOT_PATH;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UPDATE;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VERIFIED_ATTRIBUTES;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.ID_OBJECT_PROCESSING_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.MISSING_INPUT_PARAMETER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.idrepository.core.dto.IdRequestByIdDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.client.RestTemplate;

import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.AuthTypeStatusRequestDto;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.validator.BaseIdRepoValidator;
import io.mosip.idrepository.identity.helper.IdRepoServiceHelper;
import io.mosip.kernel.core.idobjectvalidator.constant.IdObjectValidatorErrorConstant;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectIOException;
import io.mosip.kernel.core.idobjectvalidator.exception.IdObjectValidationFailedException;
import io.mosip.kernel.core.idobjectvalidator.exception.InvalidIdSchemaException;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.idvalidator.exception.InvalidIDException;
import io.mosip.kernel.core.idvalidator.spi.UinValidator;
import io.mosip.kernel.core.idvalidator.spi.VidValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.idobjectvalidator.impl.IdObjectSchemaValidator;

/**
 * The Class IdRequestValidator - Validator for {@code IdRequestDTO}.
 *
 * @author Manoj SP
 */
@Component
public class IdRequestValidator extends BaseIdRepoValidator implements Validator {

	/** The Constant DOC_VALUE. */
	private static final String DOC_VALUE = "value";

	/** The Constant DOC_TYPE. */
	private static final String DOC_CAT = "category";

	/** The Constant DOCUMENTS. */
	private static final String DOCUMENTS = "documents";

	/** The Constant VALIDATE_REQUEST. */
	private static final String VALIDATE_REQUEST = "validateRequest - ";

	/** The Constant ID_REQUEST_VALIDATOR. */
	private static final String ID_REQUEST_VALIDATOR = "IdRequestValidator";

	/** The Constant ID_REPO. */
	private static final String ID_REPO = "IdRepo";

	/** The mosip logger. */
	Logger mosipLogger = IdRepoLogger.getLogger(IdRequestValidator.class);

	/** The Constant REQUEST. */
	private static final String REQUEST = "request";

	/** The Constant REGISTRATION_ID. */
	private static final String REGISTRATION_ID = "registrationId";

	/** The Constant STATUS_FIELD. */
	private static final String STATUS_FIELD = "status";

	/** The Constant ALL. */
	private static final String ALL = "all";

	/** The Constant CHECK_TYPE. */
	private static final String CHECK_TYPE = "checkType";

	/** The Constant TYPE. */
	private static final String TYPE = "type";

	/** The new registration fields. */
	@Value("#{'${mosip.kernel.idobjectvalidator.mandatory-attributes.id-repository.new-registration:}'.split(',')}")
	private List<String> newRegistrationFields;

	/** The update uin fields. */
	@Value("#{'${mosip.kernel.idobjectvalidator.mandatory-attributes.id-repository.update-uin:}'.split(',')}")
	private List<String> updateUinFields;

	/** The verified attributes fields. */
	@Value("#{'${mosip.kernel.idobjectvalidator.mandatory-attributes.id-repository.verified-attributes:}'.split(',')}")
	private List<String> verifiedAttributesFields;

	/** The status. */
	@Resource
	private List<String> uinStatus;

	/** The json validator. */
	@Autowired
	private IdObjectValidator idObjectValidator;

	@Autowired
	private IdObjectSchemaValidator idObjectSchemaValidator;

	/** The allowed types. */
	private List<String> allowedTypes = List.of("bio", "demo", "metadata", "all");

	@Value("${auth.types.allowed}")
	private String allowedAuthTypes;

	/** The uin validator. */
	@Autowired
	private UinValidator<String> uinValidator;

	/** The vid validator. */
	@Autowired
	private VidValidator<String> vidValidator;

	@Autowired
	private IdRepoServiceHelper idRepoServiceHelper;

	@Autowired
	@Qualifier("plainRestTemplate")
	private RestTemplate restTemplate;

	@Value("${mosip.idrepo.verified-attributes.schema-url}")
	private String verifiedAttributesSchemaUrl;

	@PostConstruct
	public void init() {
		newRegistrationFields.remove("");
		updateUinFields.remove("");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	@Override
	public boolean supports(Class<?> clazz) {
		return clazz.isAssignableFrom(IdRequestDTO.class) || clazz.isAssignableFrom(AuthTypeStatusRequestDto.class) || clazz.isAssignableFrom(RequestWrapper.class) 
					|| clazz.isAssignableFrom(IdRequestByIdDTO.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.validation.Validator#validate(java.lang.Object,
	 * org.springframework.validation.Errors)
	 */
	@Override
	public void validate(@Nonnull Object target, Errors errors) {
		if (target instanceof RequestWrapper<?> && ((RequestWrapper<?>) target).getRequest() instanceof IdRequestDTO) {
			RequestWrapper<IdRequestDTO<Object>> request = (RequestWrapper<IdRequestDTO<Object>>) target;

			validateReqTime(request.getRequesttime(), errors);

			if (!errors.hasErrors()) {
				validateVersion(request.getVersion(), errors);
			}

			if (!errors.hasErrors() && Objects.nonNull(request.getId())) {
				if (request.getId().equals(id.get(CREATE))) {
					validateStatus(request.getRequest().getStatus(), errors, CREATE);
					validateRequest(request.getRequest(), errors, CREATE);
				} else if (request.getId().equals(id.get(UPDATE))) {
					validateStatus(request.getRequest().getStatus(), errors, UPDATE);
					validateRequest(request.getRequest(), errors, UPDATE);
				}
			}
			validateRequest(request.getRequest(), errors, UPDATE);

			validateRegId(request.getRequest().getRegistrationId(), errors);
		}
	}

	/**
	 * Validate status.
	 *
	 * @param status the status
	 * @param errors the errors
	 * @param method the method
	 */
	private void validateStatus(String status, Errors errors, String method) {
		if (Objects.nonNull(status) && (method.equals(UPDATE) && !this.uinStatus.contains(status))) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REQUEST_VALIDATOR, "validateRegId",
					"Invalid status - " + status);
			errors.rejectValue(REQUEST, INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), STATUS_FIELD));
		}
	}

	/**
	 * Validate reg id.
	 *
	 * @param registrationId the registration id
	 * @param errors         the errors
	 */
	public void validateRegId(String registrationId, Errors errors) {
		if (Objects.isNull(registrationId)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REQUEST_VALIDATOR, "validateRegId", "NULL RID");
			errors.rejectValue(REQUEST, MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), REGISTRATION_ID));
		}
	}

	/**
	 * Validate request.
	 *
	 * @param request the request
	 * @param errors  the errors
	 * @param method  the method
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void validateRequest(Object request, Errors errors, String method) {
		try {
			if (Objects.nonNull(request)) {
				Map<String, Object> requestMap = idRepoServiceHelper.convertToMap(request);
				if (!(requestMap.containsKey(ROOT_PATH) && Objects.nonNull(requestMap.get(ROOT_PATH)))) {
					if (method.equals(CREATE)) {
						mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REQUEST_VALIDATOR, "validateRequest",
								"MISSING IDENTITY");
						errors.rejectValue(REQUEST, MISSING_INPUT_PARAMETER.getErrorCode(),
								String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), ROOT_PATH));
					}
				} else if (((Map) requestMap.get(ROOT_PATH)).isEmpty()) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REQUEST_VALIDATOR, "validateRequest",
							"INVALID IDENTITY");
					errors.rejectValue(REQUEST, INVALID_INPUT_PARAMETER.getErrorCode(),
							String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), ROOT_PATH));
				} else {
					validateDocuments(requestMap, errors);
					validateVerifiedAttributes(requestMap);
					requestMap.keySet().parallelStream().filter(key -> !key.contentEquals(ROOT_PATH)).forEach(requestMap::remove);
					if (!errors.hasErrors()) {
						String schemaVersion;
						if (requestMap.get(ROOT_PATH) != null) {
							schemaVersion = String
									.valueOf(((Map<String, Object>) requestMap.get(ROOT_PATH))
											.get(idRepoServiceHelper.getIdentityMapping().getIdentity().getIDSchemaVersion().getValue()));
							if (method.equals(CREATE)) {
								idObjectValidator.validateIdObject(idRepoServiceHelper.getSchema(schemaVersion), requestMap, newRegistrationFields);
							} else {
								idObjectValidator.validateIdObject(idRepoServiceHelper.getSchema(schemaVersion), requestMap, updateUinFields);
							}
						}
					}
				}
			} else if (method.equals(CREATE)) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REQUEST_VALIDATOR, "validateRequest", "MISSING REQUEST");
				errors.rejectValue(REQUEST, MISSING_INPUT_PARAMETER.getErrorCode(),
						String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), REQUEST));
			}
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REQUEST_VALIDATOR,
					(VALIDATE_REQUEST + "IdRepoAppException " + e.getMessage()));
			errors.rejectValue(REQUEST, INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), ROOT_PATH));
		} catch (IdObjectValidationFailedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REQUEST_VALIDATOR,
					(VALIDATE_REQUEST + "IdObjectValidationFailedException  " + e.getMessage()));
			IntStream.range(0, e.getErrorTexts().size()).boxed()
					.forEach(
							index -> errors.rejectValue(REQUEST,
									e.getCodes().get(index).equals(
											IdObjectValidatorErrorConstant.INVALID_INPUT_PARAMETER.getErrorCode())
													? INVALID_INPUT_PARAMETER.getErrorCode()
													: MISSING_INPUT_PARAMETER.getErrorCode(),
									String.format(
											e.getCodes().get(index)
													.equals(IdObjectValidatorErrorConstant.INVALID_INPUT_PARAMETER.getErrorCode())
															? INVALID_INPUT_PARAMETER.getErrorMessage()
															: MISSING_INPUT_PARAMETER.getErrorMessage(),
											Arrays.asList(e.getErrorTexts().get(index).split("-")[1].trim().split("\\|")).stream()
													.collect(Collectors.joining(" | ")))));
		} catch (InvalidIdSchemaException | IdObjectIOException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REQUEST_VALIDATOR,
					VALIDATE_REQUEST + " InvalidIdSchemaException | IdObjectIOException " + e.getMessage());
			errors.rejectValue(REQUEST, ID_OBJECT_PROCESSING_FAILED.getErrorCode(),
					ID_OBJECT_PROCESSING_FAILED.getErrorMessage());
		}
	}

	/**
	 * validate verified attributes.
	 * 
	 * @param requestMap
	 * @throws IdObjectValidationFailedException
	 * @throws IdObjectIOException
	 * @throws InvalidIdSchemaException
	 */
	@SuppressWarnings("unchecked")
	private void validateVerifiedAttributes(Map<String, Object> requestMap)
			throws IdObjectValidationFailedException, IdObjectIOException, InvalidIdSchemaException {
		if (requestMap.containsKey(VERIFIED_ATTRIBUTES) && Objects.nonNull(requestMap.get(VERIFIED_ATTRIBUTES))
				&& requestMap.get(VERIFIED_ATTRIBUTES) instanceof Map
				&& !((Map<String, Object>) requestMap.get(VERIFIED_ATTRIBUTES)).isEmpty()
				&& requestMap.containsKey(ROOT_PATH) && Objects.nonNull(requestMap.get(ROOT_PATH))
				&& !((Map<String, Object>) requestMap.get(ROOT_PATH)).isEmpty()) {
			String idSchema = restTemplate.getForObject(verifiedAttributesSchemaUrl, String.class);
			Map<String, Object> verifiedAttributesMap = (Map<String, Object>) requestMap.get(VERIFIED_ATTRIBUTES);
			idObjectSchemaValidator.validateIdObject(idSchema, verifiedAttributesMap, verifiedAttributesFields);
		}
	}

	/**
	 * Validate documents.
	 *
	 * @param requestMap the request map
	 * @param errors     the errors
	 */
	@SuppressWarnings("unchecked")
	private void validateDocuments(Map<String, Object> requestMap, Errors errors) {
		try {
			if (requestMap.containsKey(DOCUMENTS) && requestMap.containsKey(ROOT_PATH)
					&& Objects.nonNull(requestMap.get(ROOT_PATH))) {
				Map<String, Object> identityMap = idRepoServiceHelper.convertToMap(requestMap.get(ROOT_PATH));
				List<Map<String, String>> list = (List<Map<String, String>>) requestMap.get(DOCUMENTS);
				if (Objects.nonNull(requestMap.get(DOCUMENTS)) && requestMap.get(DOCUMENTS) instanceof List
						&& !list.isEmpty()) {
					if (!list.parallelStream()
							.allMatch(doc -> doc.containsKey(DOC_CAT) && Objects.nonNull(doc.get(DOC_CAT))
									&& doc.containsKey(DOC_VALUE) && Objects.nonNull(doc.get(DOC_VALUE)))) {
						mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REQUEST_VALIDATOR, "validateRequest",
								"INVALID DOC");
						errors.rejectValue(REQUEST, INVALID_INPUT_PARAMETER.getErrorCode(),
								String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), DOCUMENTS));
					} else {
						checkForDuplicates(requestMap, errors);
					}
					IntStream.range(0, list.size())
							.filter(index -> !errors.hasErrors() && list.get(index).containsKey(DOC_CAT)
									&& Objects.nonNull(list.get(index).get(DOC_CAT)))
							.forEach(index -> {
								Map<String, String> doc = list.get(index);
								if (!identityMap.containsKey(doc.get(DOC_CAT))) {
									mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REQUEST_VALIDATOR,
											(VALIDATE_REQUEST + "- validateDocuments failed for " + doc.get(DOC_CAT)));
									errors.rejectValue(REQUEST, INVALID_INPUT_PARAMETER.getErrorCode(),
											String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "documents/" + index + "/" + DOC_CAT));
								}
								if (StringUtils.isEmpty(doc.get(DOC_VALUE))) {
									mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REQUEST_VALIDATOR,
											(VALIDATE_REQUEST + "- empty doc value failed for " + doc.get(DOC_CAT)));
									errors.rejectValue(REQUEST, INVALID_INPUT_PARAMETER.getErrorCode(),
											String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "documents/" + index + "/" + DOC_VALUE));
								}
							});
				}
			}
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REQUEST_VALIDATOR,
					("validateDocuments " + e.getMessage()));
			errors.rejectValue(REQUEST, INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), ROOT_PATH));
		}
	}

	/**
	 * Check for duplicates.
	 *
	 * @param requestMap the request map
	 * @param errors     the errors
	 */
	@SuppressWarnings("unchecked")
	private void checkForDuplicates(Map<String, Object> requestMap, Errors errors) {
		List<Map<String, String>> docList = (List<Map<String, String>>) requestMap.get(DOCUMENTS);
		HashSet<String> docSet = new HashSet<>();
		for (int i = 0; i < docList.size(); i++) {
			if (!docSet.add(docList.get(i).get(DOC_CAT))) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO, ID_REQUEST_VALIDATOR,
						"checkForDuplicates - Duplicate category code");
				errors.rejectValue(REQUEST, INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), DOCUMENTS + "/" + i + "/" + DOC_CAT));
				break;
			}
		}
	}


	/**
	 * Validate uin.
	 *
	 * @param uin the uin
	 * @return true, if successful
	 * @throws IdRepoAppException the id repo app exception
	 */
	public boolean validateUin(String uin) throws IdRepoAppException {
		try {
			return uinValidator.validateId(uin);
		} catch (InvalidIDException e) {
			return false;
		}
	}

	/**
	 * Validate vid.
	 *
	 * @param vid the vid
	 * @return true, if successful
	 */
	public boolean validateVid(String vid) {
		try {
			return vidValidator.validateId(vid);
		} catch (InvalidIDException e) {
			return false;
		}
	}

	/**
	 * Validate type query parameter.
	 *
	 * @param type the type
	 * @return the string
	 * @throws IdRepoAppException the id repo app exception
	 */
	public String validateType(String type) throws IdRepoAppException {
		if (Objects.nonNull(type)) {
			List<String> typeList = Arrays.asList(StringUtils.split(type.toLowerCase(), ','));
			if (typeList.size() == 1 && !allowedTypes.containsAll(typeList)) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REQUEST_VALIDATOR, CHECK_TYPE,
						INVALID_INPUT_PARAMETER.getErrorMessage() + typeList);
				throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), TYPE));
			} else {
				if (typeList.contains(ALL) || allowedTypes.parallelStream().filter(allowedType -> !allowedType.equals(ALL))
						.allMatch(typeList::contains)) {
					type = ALL;
				} else if (!allowedTypes.containsAll(typeList)) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REQUEST_VALIDATOR, CHECK_TYPE,
							INVALID_INPUT_PARAMETER.getErrorMessage() + typeList);
					throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
							String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), TYPE));
				}
			}
		}
		return type;
	}

	public IdType validateIdType(String idType) throws IdRepoAppException {
		if (Objects.nonNull(idType)) {
			try {
				return IdType.valueOf(idType.toUpperCase());
			} catch (IllegalArgumentException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REQUEST_VALIDATOR, "validateIdType", e.getMessage());
				throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "idType"), e);
			}
		}
		return null;
	}

	public void validateTypeAndExtractionFormats(String type, Map<String, String> extractionFormats) throws IdRepoAppException {
		if (Objects.isNull(type) && !extractionFormats.isEmpty()) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REQUEST_VALIDATOR, "validateTypeAndExtractionFormats",
					"type is null but extraction format is not null");
			throw new IdRepoAppException(MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), "type"));
		} else if (Objects.nonNull(type) && !type.equalsIgnoreCase("bio") && !type.equalsIgnoreCase("all")
				&& !extractionFormats.isEmpty()) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REQUEST_VALIDATOR, "validateTypeAndExtractionFormats",
					"type is not bio but extraction format is not null");
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "type"));
		}
	}

	public void validateIdvId(String individualId, IdType idType) throws IdRepoAppException {
		if ((idType != IdType.UIN && idType != IdType.VID) || (idType == IdType.UIN && !this.validateUin(individualId))
				|| (idType == IdType.VID && !this.validateVid(individualId))) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REQUEST_VALIDATOR, "getIdType", "Invalid ID");
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "individualId"));
		}
	}

	public void validateAuthTypes(List<AuthtypeStatus> authTypeStatusList) throws IdRepoAppException {
		if (authTypeStatusList == null || authTypeStatusList.isEmpty()) {
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
					String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "authTypes"));
		}

		String[] authTypesArray = allowedAuthTypes.toLowerCase().split(",");
		List<String> authTypesAllowed = new ArrayList<>(Arrays.asList(authTypesArray));
		for (AuthtypeStatus authTypeStatus : authTypeStatusList) {
			String authType = getAuthTypeStatus(authTypeStatus);
			if (authType == null || !authTypesAllowed.contains(authType.toLowerCase())) {
				throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "authTypes"));
			}
		}
	}

	private String getAuthTypeStatus(AuthtypeStatus authTypeStatus) {
		if (authTypeStatus.getAuthType() != null && !authTypeStatus.getAuthType().isEmpty()) {
			if (authTypeStatus.getAuthSubType() != null && !authTypeStatus.getAuthSubType().isEmpty()) {
				return String.format("%s%s%s", authTypeStatus.getAuthType(), AUTH_TYPE_SEPERATOR,
						authTypeStatus.getAuthSubType());
			} else {
				return authTypeStatus.getAuthType();
			}
		}
		return null;
	}
}
