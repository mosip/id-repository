package io.mosip.idrepository.identity.controller;

import static io.mosip.idrepository.core.constant.IdRepoConstants.MOSIP_KERNEL_IDREPO_JSON_PATH;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DATA_VALIDATION_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_REQUEST;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.MISSING_INPUT_PARAMETER;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;

import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.AuthTypeStatusRequestDto;
import io.mosip.idrepository.core.dto.AuthtypeResponseDto;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.AuthtypeStatusService;
import io.mosip.idrepository.core.spi.IdRepoService;
import io.mosip.idrepository.core.util.DataValidationUtil;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import springfox.documentation.annotations.ApiIgnore;

/**
 * The Class IdRepoController - Controller class for Identity service. These
 * services is used by Registration Processor to store/update during
 * registration process and ID Authentication to retrieve Identity of an
 * Individual for their authentication.
 *
 * @author Manoj SP
 */
@RestController
public class IdRepoController {

	private static final String FACE_EXTRACTION_FORMAT = "faceExtractionFormat";

	private static final String IRIS_EXTRACTION_FORMAT = "irisExtractionFormat";

	private static final String FINGER_EXTRACTION_FORMAT = "fingerExtractionFormat";

	private static final String ID_TYPE = "idType";

	/** The Constant RETRIEVE_IDENTITY. */
	private static final String RETRIEVE_IDENTITY = "retrieveIdentity";

	/** The mosip logger. */
	Logger mosipLogger = IdRepoLogger.getLogger(IdRepoController.class);

	/** The Constant CREATE. */
	private static final String CREATE = "create";

	/** The Constant CREATE. */
	private static final String UPDATE = "update";

	/** The Constant TYPE. */
	private static final String TYPE = "type";

	/** The Constant ID_REPO_CONTROLLER. */
	private static final String ID_REPO_CONTROLLER = "IdRepoController";

	/** The Constant ADD_IDENTITY. */
	private static final String ADD_IDENTITY = "addIdentity";

	/** The Constant UPDATE_IDENTITY. */
	private static final String UPDATE_IDENTITY = "updateIdentity";

	/** The Constant UIN. */
	private static final String UIN = "UIN";

	/** The id. */
	@Resource
	private Map<String, String> id;

	/** The allowed types. */
	@Resource
	private List<String> allowedTypes;

	/** The id repo service. */
	@Autowired
	private IdRepoService<IdRequestDTO, IdResponseDTO> idRepoService;

	/** The validator. */
	@Autowired
	private IdRequestValidator validator;

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private AuditHelper auditHelper;

	/** The env. */
	@Autowired
	private Environment env;

	@Autowired
	private AuthtypeStatusService authTypeStatusService;

	/**
	 * Inits the binder.
	 *
	 * @param binder
	 *            the binder
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.addValidators(validator);
	}

	/**
	 * This service will create a new ID record in ID repository and store
	 * corresponding demographic and bio-metric documents.
	 *
	 * @param request
	 *            the request
	 * @param errors
	 *            the errors
	 * @return the response entity
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@PostMapping(path = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdResponseDTO> addIdentity(@Validated @RequestBody IdRequestDTO request,
			@ApiIgnore Errors errors) throws IdRepoAppException {
		String regId = Optional.ofNullable(request.getRequest()).map(req -> String.valueOf(req.getRegistrationId()))
				.orElse("null");
		try {
			String uin = getUin(request.getRequest());
			validator.validateId(request.getId(), CREATE);
			DataValidationUtil.validate(errors);
			if (!validator.validateUin(uin)) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, "addIdentity", "Invalid uin");
				throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), UIN));
			}
			return new ResponseEntity<>(idRepoService.addIdentity(request, uin), HttpStatus.OK);
		} catch (IdRepoDataValidationException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE,
					regId, IdType.RID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, ADD_IDENTITY, e.getMessage());
			throw new IdRepoAppException(DATA_VALIDATION_FAILED, e);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE,
					regId, IdType.RID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, RETRIEVE_IDENTITY, e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, regId,
					IdType.RID, "Create Identity requested");
		}
	}

	/**
	 * This service will retrieve an ID record from ID repository for a given UIN
	 * and identity type as bio/demo/all.
	 *
	 * @param uin
	 *            the uin
	 * @param type
	 *            the type
	 *
	 * @return the response entity
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	@PreAuthorize("hasAnyRole('RESIDENT','REGISTRATION_ADMIN','REGISTRATION_SUPERVISOR','REGISTRATION_OFFICER','REGISTRATION_PROCESSOR','ID_AUTHENTICATION','RESIDENT')")
	@GetMapping(path = "/idvid/{id}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdResponseDTO> retrieveIdentity(@PathVariable String id,
			@RequestParam(name = TYPE, required = false) @Nullable String type,
			@RequestParam(name = ID_TYPE, required = false) @Nullable String idType,
			@RequestParam(name = FINGER_EXTRACTION_FORMAT, required = false) @Nullable String fingerExtractionFormat,
			@RequestParam(name = IRIS_EXTRACTION_FORMAT, required = false) @Nullable String irisExtractionFormat,
			@RequestParam(name = FACE_EXTRACTION_FORMAT, required = false) @Nullable String faceExtractionFormat)
			throws IdRepoAppException {
		try {
			type = validator.validateType(type);
			Map<String, String> extractionFormats = new HashMap<>();
			if(Objects.nonNull(fingerExtractionFormat)) {
				extractionFormats.put(FINGER_EXTRACTION_FORMAT, fingerExtractionFormat);
			}
			if(Objects.nonNull(irisExtractionFormat)) {
				extractionFormats.put(IRIS_EXTRACTION_FORMAT, irisExtractionFormat);
			}
			if(Objects.nonNull(faceExtractionFormat)) {
				extractionFormats.put(FACE_EXTRACTION_FORMAT, faceExtractionFormat);
			}
			extractionFormats.remove(null);
			validator.validateTypeAndExtractionFormats(type, extractionFormats);
			return new ResponseEntity<>(idRepoService.retrieveIdentity(id,
					Objects.isNull(idType) ? getIdType(id) : validator.validateIdType(idType), type, extractionFormats),
					HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE,
					AuditEvents.RETRIEVE_IDENTITY_REQUEST_RESPONSE_UIN, id, IdType.UIN, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, RETRIEVE_IDENTITY, e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.RETRIEVE_IDENTITY_REQUEST_RESPONSE_UIN, id,
					IdType.UIN, "Retrieve Identity requested");
		}
	}

	/**
	 * This operation will update an existing ID record in the ID repository for a
	 * given UIN.
	 *
	 * @param request
	 *            the request
	 * @param errors
	 *            the errors
	 * @return the response entity
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@PatchMapping(path = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<IdResponseDTO> updateIdentity(@Validated @RequestBody IdRequestDTO request,
			@ApiIgnore Errors errors) throws IdRepoAppException {
		String regId = Optional.ofNullable(request.getRequest()).map(req -> String.valueOf(req.getRegistrationId()))
				.orElse("null");
		try {
			String uin = getUin(request.getRequest());
			validator.validateId(request.getId(), UPDATE);
			DataValidationUtil.validate(errors);
			if (!validator.validateUin(uin)) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, "addIdentity", "Invalid uin");
				throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), UIN));
			}
			return new ResponseEntity<>(idRepoService.updateIdentity(request, uin), HttpStatus.OK);
		} catch (IdRepoDataValidationException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.UPDATE_IDENTITY_REQUEST_RESPONSE,
					regId, IdType.RID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, UPDATE_IDENTITY, e.getMessage());
			throw new IdRepoAppException(DATA_VALIDATION_FAILED, e);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.UPDATE_IDENTITY_REQUEST_RESPONSE,
					regId, IdType.RID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, RETRIEVE_IDENTITY, e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.UPDATE_IDENTITY_REQUEST_RESPONSE, regId,
					IdType.RID, "Update Identity requested");
		}
	}

	/**
	 * To fetch Auth Type status based on Individual's details
	 *
	 * @param authtypeResponseDto
	 *            as request body
	 * @param errors
	 *            associate error
	 * @param partnerId
	 *            the partner id
	 * @param mispLK
	 *            the misp LK
	 * @return authtypeResponseDto
	 * @throws IdAuthenticationAppException
	 *             the id authentication app exception
	 * @throws IDDataValidationException
	 *             the ID data validation exception
	 */
	@PreAuthorize("hasAnyRole('RESIDENT')")
	@ApiOperation(value = "Authtype Status Request", response = IdRepoAppException.class)
	@GetMapping(path = "/authtypes/status/individualIdType/{IDType}/individualId/{ID}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Request authenticated successfully"),
			@ApiResponse(code = 400, message = "No Records Found") })
	public ResponseEntity<AuthtypeResponseDto> getAuthTypeStatus(@PathVariable("ID") String individualId,
			@PathVariable("IDType") String individualIdType) throws IdRepoAppException {
		AuthtypeResponseDto authtypeResponseDto = new AuthtypeResponseDto();
		boolean isIdTypeValid = false;
		try {
			IdType idType = validator.validateIdTypeForAuthTypeStatus(individualIdType);
			isIdTypeValid = true;
			validator.validateIdvId(individualId, idType);
			List<AuthtypeStatus> authtypeStatusList = authTypeStatusService.fetchAuthTypeStatus(individualId, idType);
			Map<String, List<AuthtypeStatus>> authtypestatusmap = new HashMap<>();
			authtypestatusmap.put("authTypes", authtypeStatusList);
			authtypeResponseDto.setResponse(authtypestatusmap);
			authtypeResponseDto.setResponsetime(DateUtils.getUTCCurrentDateTime());

			auditHelper.audit(AuditModules.AUTH_TYPE_STATUS, AuditEvents.UPDATE_AUTH_TYPE_STATUS_REQUEST_RESPONSE,
					individualId, IdType.valueOf(individualIdType), "auth type status update status : " + true);

			return new ResponseEntity<>(authtypeResponseDto, HttpStatus.OK);
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, "getAuthTypeStatus", e.getMessage());
			auditHelper.auditError(AuditModules.AUTH_TYPE_STATUS, AuditEvents.UPDATE_AUTH_TYPE_STATUS_REQUEST_RESPONSE,
					individualId, isIdTypeValid ? IdType.valueOf(individualIdType) : IdType.UIN, e);
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	/**
	 * Update authtype status.
	 *
	 * @param authTypeStatusDto
	 *            the auth type status dto
	 * @param errors
	 *            the e
	 * @return the response entity
	 * @throws IdAuthenticationAppException
	 *             the id authentication app exception
	 * @throws IDDataValidationException
	 */
	@PreAuthorize("hasAnyRole('RESIDENT')")
	@PostMapping(path = "authtypes/status", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ApiOperation(value = "Authenticate Internal Request", response = IdRepoAppException.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Request authenticated successfully"),
			@ApiResponse(code = 400, message = "Request authenticated failed") })
	public ResponseEntity<IdResponseDTO> updateAuthtypeStatus(
			@RequestBody AuthTypeStatusRequestDto authTypeStatusRequest) throws IdRepoAppException {
		boolean isIdTypeValid = false;
		try {
			IdType idType = validator.validateIdTypeForAuthTypeStatus(authTypeStatusRequest.getIndividualIdType());
			isIdTypeValid = true;
			validator.validateIdvId(authTypeStatusRequest.getIndividualId(), idType);
			IdResponseDTO updateAuthtypeStatus = authTypeStatusService.updateAuthTypeStatus(
					authTypeStatusRequest.getIndividualId(), idType, authTypeStatusRequest.getRequest());
			auditHelper.audit(AuditModules.AUTH_TYPE_STATUS, AuditEvents.UPDATE_AUTH_TYPE_STATUS_REQUEST_RESPONSE,
					authTypeStatusRequest.getIndividualId(),
					IdType.valueOf(authTypeStatusRequest.getIndividualIdType()),
					"auth type status update status : " + true);
			return new ResponseEntity<>(updateAuthtypeStatus, HttpStatus.OK);
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, "updateAuthtypeStatus",
					e.getMessage());
			auditHelper.auditError(AuditModules.AUTH_TYPE_STATUS, AuditEvents.UPDATE_AUTH_TYPE_STATUS_REQUEST_RESPONSE,
					authTypeStatusRequest.getIndividualId(),
					isIdTypeValid ? IdType.valueOf(authTypeStatusRequest.getIndividualIdType()) : IdType.UIN, e);
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	/**
	 * This Method returns Uin from the Identity Object.
	 *
	 * @param request
	 *            the request
	 * @return the uin
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private String getUin(Object request) throws IdRepoAppException {
		if (Objects.isNull(request)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, "getUin", "request is null");
			throw new IdRepoAppException(MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), "request"));
		}
		Object uin = null;
		String pathOfUin = env.getProperty(MOSIP_KERNEL_IDREPO_JSON_PATH);
		try {
			String identity = mapper.writeValueAsString(request);
			JsonPath jsonPath = JsonPath.compile(pathOfUin);
			uin = jsonPath.read(identity);
			return String.valueOf(uin);
		} catch (JsonProcessingException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, "getUin", e.getMessage());
			throw new IdRepoAppException(INVALID_REQUEST, e);
		} catch (JsonPathException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, "getUin", e.getMessage());
			throw new IdRepoAppException(MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), pathOfUin.replace(".", "/")));
		}
	}

	private IdType getIdType(String id) throws IdRepoAppException {
		if (validator.validateUin(id))
			return IdType.UIN;
		if (validator.validateVid(id))
			return IdType.VID;
		if (validator.validateRid(id))
			return IdType.RID;
		mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, "getIdType", "Invalid ID");
		throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
				String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), "id"));
	}
}
