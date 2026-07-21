package io.mosip.idrepository.identity.controller;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import static io.mosip.idrepository.core.constant.IdRepoConstants.FACE_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.FINGER_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IRIS_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DATA_VALIDATION_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_REQUEST;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.MISSING_INPUT_PARAMETER;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.mosip.idrepository.identity.validator.IndividualIdValidator;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;

import io.mosip.idrepository.core.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import io.mosip.kernel.core.http.RequestWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;

import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.AuthtypeStatusService;
import io.mosip.idrepository.core.spi.IdRepoService;
import io.mosip.idrepository.core.util.DataValidationUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.dto.AttributeListDto;
import io.mosip.idrepository.identity.dto.RidDto;
import io.mosip.idrepository.identity.dto.UpdateCountDto;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.lang.StringUtils;

/**
 * REST controller for identity APIs ({@code /idrepository/v1/identity/*}).
 * <p>
 * Thin HTTP layer over {@link io.mosip.idrepository.core.spi.IdRepoService}.
 * </p>
 */
@RestController
@Tag(name = "id-repo-controller", description = "Id Repo Controller")
public class IdRepoController {

	private static final String GET_UIN = "getUin";

	private static final String ID_TYPE = "idType";

	/** The Constant RETRIEVE_IDENTITY. */
	private static final String RETRIEVE_IDENTITY = "retrieveIdentity";

	/** Mosip logger. */
	Logger mosipLogger = IdRepoLogger.getLogger(IdRepoController.class);

	/** The Constant CREATE. */
	private static final String CREATE = "create";

	/** The Constant CREATE. */
	private static final String UPDATE = "update";

	/** The Constant TYPE. */
	private static final String TYPE = "type";

	/** The Constant ID. */
	private static final String ID = "id";

	/** The Constant ID_REPO_CONTROLLER. */
	private static final String ID_REPO_CONTROLLER = "IdRepoController";

	/** The Constant ADD_IDENTITY. */
	private static final String ADD_IDENTITY = "addIdentity";

	/** The Constant UPDATE_IDENTITY. */
	private static final String UPDATE_IDENTITY = "updateIdentity";

	/** The Constant UIN. */
	private static final String UIN = "UIN";

	/** Id. */
	@Resource
	private Map<String, String> id;

	/** Id repo service. */
	@Autowired
	private IdRepoService<IdRequestDTO, IdResponseDTO> idRepoService;

	/** Validator. */
	@Autowired
	private IdRequestValidator validator;

	/** The IndividualIdValidator. */
	@Autowired
	/** Individual id validator. */
	private IndividualIdValidator individualIdValidator;

	/** Mapper. */
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	/** Audit helper. */
	private AuditHelper auditHelper;

	@Autowired
	/** Auth type status service. */
	private AuthtypeStatusService authTypeStatusService;

	@Value("${" + IdRepoConstants.RID_GET_ID + "}")
	/** Rid id. */
	private String ridId;

	@Value("${" + IdRepoConstants.RID_GET_VERSION + "}")
	/** Rid version. */
	private String ridVersion;

	@Value("${" + IdRepoConstants.IDVID_METADATA_ID + "}")
	/** Idvid metadata id. */
	private String idvidMetadataId;

	@Value("${" + IdRepoConstants.IDVID_METADATA_VERSION + "}")
	/** Idvid metadata version. */
	private String idvidMetadataVersion;

	/**
	 * Registers {@link IdRequestValidator} for {@code idRequestDTO} binding.
	 *
	 * @param binder web data binder for identity requests
	 */
	@InitBinder("idRequestDTO")
	public void initBinder(WebDataBinder binder) {
		binder.addValidators(validator);
	}

	@InitBinder("idVidMetadataRequestWrapper")
	/**
	 * Registers {@link IndividualIdValidator} for ID/VID metadata search requests.
	 *
	 * @param binder web data binder for metadata wrapper
	 */
	public void initIdVidMetadataRequestWrapperBinder(WebDataBinder binder) {
		binder.addValidators(individualIdValidator);
	}

	/**
	 * Creates a new ID record in the ID repository with demographic and biometric documents.
	 *
	 * @param idRequestDTO MOSIP identity create request envelope
	 * @param errors       Spring validation errors binding result
	 * @return created identity response
	 * @throws IdRepoAppException on validation or persistence failure
	 */
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getPostidrepo())")
	@PostMapping(path = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "addIdentity", description = "addIdentity", tags = { "id-repo-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<IdResponseDTO> addIdentity(@Validated @RequestBody IdRequestDTO idRequestDTO,
			@Parameter(hidden = true) Errors errors) throws IdRepoAppException {
		String regId = Optional.ofNullable(idRequestDTO.getRequest()).map(req -> String.valueOf(req.getRegistrationId()))
				.orElse("null");
		try {
			String uin = getUin(idRequestDTO.getRequest());
			validator.validateId(idRequestDTO.getId(), CREATE);
			DataValidationUtil.validate(errors);
			if (!validator.validateUin(uin)) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, ADD_IDENTITY, "Invalid uin");
				throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), UIN));
			}
			return new ResponseEntity<>(idRepoService.addIdentity(idRequestDTO, uin), HttpStatus.OK);
		} catch (IdRepoDataValidationException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE,
					regId, IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, ADD_IDENTITY, e.getMessage());
			throw new IdRepoAppException(DATA_VALIDATION_FAILED, e);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE,
					regId, IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, RETRIEVE_IDENTITY, e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_IDENTITY_REQUEST_RESPONSE, regId,
					IdType.ID, "Create Identity requested");
		}
	}

	/**
	 * Retrieves an ID record for a given UIN/VID and identity type (bio/demo/all).
	 *
	 * @param id                    individual identifier (UIN or VID)
	 * @param type                  identity slice to return (bio, demo, or all)
	 * @param idType                optional explicit ID type when {@code id} is ambiguous
	 * @param fingerExtractionFormat optional fingerprint template format
	 * @param irisExtractionFormat   optional iris template format
	 * @param faceExtractionFormat   optional face template format
	 * @return retrieved identity response
	 * @throws IdRepoAppException on validation or lookup failure
	 */
	@Deprecated
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getGetidvidid())")
	@GetMapping(path = "/idvid/{id}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "retrieveIdentity", description = "retrieveIdentity", tags = { "id-repo-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<IdResponseDTO> retrieveIdentity(@PathVariable("id") String id,
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
	 * Retrieves identity by individual ID (POST body).
	 *
	 * @param idRequestByIdDTO retrieve request with ID, type, and extraction formats
	 * @param errors           Spring validation errors binding result
	 * @return retrieved identity response
	 * @throws IdRepoAppException on validation or lookup failure
	 */
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getPostidvidid())")
	@PostMapping(path = "/idvid/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "retrieveIdentityById", description = "retrieveIdentityById", tags = { "id-repo-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<IdResponseDTO> retrieveIdentityById(@Validated @RequestBody IdRequestByIdDTO idRequestByIdDTO,
														   @Parameter(hidden = true) Errors errors) throws IdRepoAppException {
		try {
			String type = validator.validateType(idRequestByIdDTO.getType());
			Map<String, String> extractionFormats = new HashMap<>();
			if(Objects.nonNull(idRequestByIdDTO.getFingerExtractionFormat())) {
				extractionFormats.put(FINGER_EXTRACTION_FORMAT, idRequestByIdDTO.getFingerExtractionFormat());
			}
			if(Objects.nonNull(idRequestByIdDTO.getIrisExtractionFormat())) {
				extractionFormats.put(IRIS_EXTRACTION_FORMAT, idRequestByIdDTO.getIrisExtractionFormat());
			}
			if(Objects.nonNull(idRequestByIdDTO.getFaceExtractionFormat())) {
				extractionFormats.put(FACE_EXTRACTION_FORMAT, idRequestByIdDTO.getFaceExtractionFormat());
			}
			extractionFormats.remove(null);
			validator.validateTypeAndExtractionFormats(type, extractionFormats);
			return new ResponseEntity<>(idRepoService.retrieveIdentity(idRequestByIdDTO.getId(),
					Objects.isNull(idRequestByIdDTO.getIdType()) ? getIdType(idRequestByIdDTO.getId()) : validator.validateIdType(idRequestByIdDTO.getIdType()), type, extractionFormats),
					HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE,
					AuditEvents.RETRIEVE_IDENTITY_REQUEST_RESPONSE_UIN, idRequestByIdDTO.getId(), IdType.UIN, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, RETRIEVE_IDENTITY, e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.RETRIEVE_IDENTITY_REQUEST_RESPONSE_UIN, idRequestByIdDTO.getId(),
					IdType.UIN, "Retrieve Identity requested");
		}
	}

	/**
	 * Retrieves identity by individual ID in the MOSIP request wrapper (v2).
	 *
	 * @param request MOSIP request wrapper containing {@link IdRequestByIdDTO}
	 * @param errors  Spring validation errors binding result
	 * @return retrieved identity response
	 * @throws IdRepoAppException on validation or lookup failure
	 */
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getPostidvididv2())")
	@PostMapping(path = "/idvid/v2", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "retrieveIdentityByIdV2", description = "retrieveIdentityByIdV2", tags = { "id-repo-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<IdResponseDTO> retrieveIdentityByIdV2(@Validated @RequestBody RequestWrapper<IdRequestByIdDTO> request,
																@Parameter(hidden = true) Errors errors) throws IdRepoAppException {
		try {
			String type = validator.validateType(request.getRequest().getType());
			Map<String, String> extractionFormats = new HashMap<>();
			if(Objects.nonNull(request.getRequest().getFingerExtractionFormat())) {
				extractionFormats.put(FINGER_EXTRACTION_FORMAT, request.getRequest().getFingerExtractionFormat());
			}
			if(Objects.nonNull(request.getRequest().getIrisExtractionFormat())) {
				extractionFormats.put(IRIS_EXTRACTION_FORMAT, request.getRequest().getIrisExtractionFormat());
			}
			if(Objects.nonNull(request.getRequest().getFaceExtractionFormat())) {
				extractionFormats.put(FACE_EXTRACTION_FORMAT, request.getRequest().getFaceExtractionFormat());
			}
			extractionFormats.remove(null);
			validator.validateTypeAndExtractionFormats(type, extractionFormats);
			return new ResponseEntity<>(idRepoService.retrieveIdentity(request.getRequest().getId(),
					Objects.isNull(request.getRequest().getIdType()) ? getIdType(request.getRequest().getId()) : validator.validateIdType(request.getRequest().getIdType()), type, extractionFormats),
					HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE,
					AuditEvents.RETRIEVE_IDENTITY_REQUEST_RESPONSE_UIN, request.getRequest().getId(), IdType.UIN, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, RETRIEVE_IDENTITY, e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.RETRIEVE_IDENTITY_REQUEST_RESPONSE_UIN, request.getRequest().getId(),
					IdType.UIN, "Retrieve Identity requested");
		}
	}

	/**
	 * Updates an existing ID record in the ID repository for a given UIN.
	 *
	 * @param idRequestDTO MOSIP identity update request envelope
	 * @param errors       Spring validation errors binding result
	 * @return updated identity response
	 * @throws IdRepoAppException on validation or persistence failure
	 */
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getPatchidrepo())")
	@PatchMapping(path = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "updateIdentity", description = "updateIdentity", tags = { "id-repo-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "204", description = "No Content" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			})
	public ResponseEntity<IdResponseDTO> updateIdentity(@Validated @RequestBody IdRequestDTO idRequestDTO,
			@Parameter(hidden = true) Errors errors) throws IdRepoAppException {
		String regId = Optional.ofNullable(idRequestDTO.getRequest()).map(req -> String.valueOf(req.getRegistrationId()))
				.orElse("null");
		try {
			String uin = getUin(idRequestDTO.getRequest());
			validator.validateId(idRequestDTO.getId(), UPDATE);
			DataValidationUtil.validate(errors);
			if (!validator.validateUin(uin)) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, ADD_IDENTITY, "Invalid uin");
				throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), UIN));
			}
			return new ResponseEntity<>(idRepoService.updateIdentity(idRequestDTO, uin), HttpStatus.OK);
		} catch (IdRepoDataValidationException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.UPDATE_IDENTITY_REQUEST_RESPONSE,
					regId, IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, UPDATE_IDENTITY, e.getMessage());
			throw new IdRepoAppException(DATA_VALIDATION_FAILED, e);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.UPDATE_IDENTITY_REQUEST_RESPONSE,
					regId, IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, RETRIEVE_IDENTITY, e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.UPDATE_IDENTITY_REQUEST_RESPONSE, regId,
					IdType.ID, "Update Identity requested");
		}
	}

	/**
	 * Fetches authentication type lock status for an individual.
	 *
	 * @param individualId     UIN, VID, or RID
	 * @param individualIdType ID type discriminator
	 * @return authentication type status response
	 * @throws IdRepoAppException on validation or lookup failure
	 */
	@Deprecated(since = "1.2.0")
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getGetauthtypesstatusindividualidtypeindividualid())")
	@GetMapping(path = "/authtypes/status/individualIdType/{IDType}/individualId/{ID}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Authtype Status Request", description = "Authtype Status Request", tags = { "id-repo-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Request authenticated successfully",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = IdRepoAppException.class)))),
			@ApiResponse(responseCode = "400", description = "No Records Found" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<AuthtypeResponseDto> getAuthTypeStatus(@PathVariable("ID") String individualId,
			@PathVariable("IDType") String individualIdType) throws IdRepoAppException {
		AuthtypeResponseDto authtypeResponseDto = new AuthtypeResponseDto();
		boolean isIdTypeValid = false;
		IdType idType = null;
		try {
			if(StringUtils.isEmpty(individualId.trim())){
				throw new IdRepoAppException("IDR-IDC-002", "Invalid Input Parameter");
			}
			idType = validator.validateIdType(individualIdType);
			isIdTypeValid = true;
			validator.validateIdvId(individualId, idType);
			List<AuthtypeStatus> authtypeStatusList = authTypeStatusService.fetchAuthTypeStatus(individualId, idType);
			Map<String, List<AuthtypeStatus>> authtypestatusmap = new HashMap<>();
			authtypestatusmap.put("authTypes", authtypeStatusList);
			authtypeResponseDto.setResponse(authtypestatusmap);
			authtypeResponseDto.setResponsetime(DateUtils2.getUTCCurrentDateTime());

			auditHelper.audit(AuditModules.AUTH_TYPE_STATUS, AuditEvents.UPDATE_AUTH_TYPE_STATUS_REQUEST_RESPONSE,
					individualId, idType, "auth type status update status : " + true);

			return new ResponseEntity<>(authtypeResponseDto, HttpStatus.OK);
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, "getAuthTypeStatus", e.getMessage());
			auditHelper.auditError(AuditModules.AUTH_TYPE_STATUS, AuditEvents.UPDATE_AUTH_TYPE_STATUS_REQUEST_RESPONSE,
					individualId, isIdTypeValid ? idType : IdType.UIN, e);
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getGetauthtypesstatusindividualidtypeindividualid())")
	@GetMapping(path = "/authtypes/status/{ID}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Authtype Status Request", description = "Authtype Status Request", tags = {
			"id-repo-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Request authenticated successfully", content = @Content(array = @ArraySchema(schema = @Schema(implementation = IdRepoAppException.class)))),
			@ApiResponse(responseCode = "400", description = "No Records Found", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(hidden = true))) })
	/**
	 * Get auth type status.
	 * @param individualId individual id
	 * @return response entity<authtype response dto>
	 */
	public ResponseEntity<AuthtypeResponseDto> getAuthTypeStatus(@PathVariable("ID") String individualId) throws IdRepoAppException {
		return this.getAuthTypeStatus(individualId, getIdType(individualId).getIdType());
	}

	/**
	 * Updates authentication type lock status for an individual.
	 *
	 * @param authTypeStatusRequest MOSIP auth-type status update request
	 * @return update acknowledgement response
	 * @throws IdRepoAppException on validation or persistence failure
	 */
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getPostauthtypesstatus())")
	@PostMapping(path = "authtypes/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Authenticate Internal Request", description = "Authenticate Internal Request", tags = { "id-repo-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Request authenticated successfully",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = IdRepoAppException.class)))),
			@ApiResponse(responseCode = "400", description = "Request authenticated failed" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<IdResponseDTO> updateAuthtypeStatus(
			@RequestBody AuthTypeStatusRequestDto authTypeStatusRequest) throws IdRepoAppException {
		String individualId = authTypeStatusRequest.getIndividualId();
		try {
			if (StringUtils.isBlank(authTypeStatusRequest.getId())
					|| StringUtils.isBlank(authTypeStatusRequest.getVersion())
					|| StringUtils.isBlank(authTypeStatusRequest.getRequestTime())) {
				throw new IdRepoAppException(INVALID_REQUEST);
			}
				authTypeStatusRequest.setIndividualIdType(Objects.nonNull(authTypeStatusRequest.getIndividualIdType())
						? authTypeStatusRequest.getIndividualIdType()
						: getIdType(individualId).getIdType());
				IdType idType = validator.validateIdType(authTypeStatusRequest.getIndividualIdType());
				validator.validateIdvId(individualId, idType);
				validator.validateAuthTypes(authTypeStatusRequest.getRequest());
				IdResponseDTO updateAuthtypeStatus = authTypeStatusService.updateAuthTypeStatus(
						individualId, idType, authTypeStatusRequest.getRequest());
				String individualIdType = authTypeStatusRequest.getIndividualIdType();
				auditHelper.audit(AuditModules.AUTH_TYPE_STATUS, AuditEvents.UPDATE_AUTH_TYPE_STATUS_REQUEST_RESPONSE,
						individualId,
						individualIdType == null ? IdType.UIN : IdType.valueOf(individualIdType),
						"auth type status update status : " + true);
				return new ResponseEntity<>(updateAuthtypeStatus, HttpStatus.OK);
		} catch (IdRepoAppException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, "updateAuthtypeStatus",
					e.getMessage());
			auditHelper.auditError(AuditModules.AUTH_TYPE_STATUS, AuditEvents.UPDATE_AUTH_TYPE_STATUS_REQUEST_RESPONSE,
					individualId,
					authTypeStatusRequest.getIndividualIdType() == null ? IdType.UIN
							: IdType.valueOf(authTypeStatusRequest.getIndividualIdType()),
					e);
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	@Deprecated(since = "1.2.3")
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getGetRidByIndividualId())")
	@GetMapping(path = "/rid/{individualId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get RID by IndividualId Request", description = "Get RID by IndividualId Request", tags = {
			"id-repo-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Request authenticated successfully", content = @Content(array = @ArraySchema(schema = @Schema(implementation = IdRepoAppException.class)))),
			@ApiResponse(responseCode = "400", description = "No Records Found", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(hidden = true))) })
	/**
	 * Get rid by individual id.
	 *
	 * @param individualId UIN, VID, or RID
	 * @param idType       optional explicit ID type
	 * @return RID for the individual
	 */
	public ResponseEntity<ResponseWrapper<RidDto>> getRidByIndividualId(@PathVariable("individualId") String individualId,
			@RequestParam(name = ID_TYPE, required = false) @Nullable String idType) throws IdRepoAppException {
		IdType individualIdType = Objects.isNull(idType) ? getIdType(individualId) : validator.validateIdType(idType);
		auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.GET_RID_BY_INDIVIDUALID,
				individualId, individualIdType, "Get RID by IndividualId Request received");
		ResponseWrapper<RidDto> responseWrapper = new ResponseWrapper<>();
		RidDto ridDto = new RidDto();
		ridDto.setRid(idRepoService.getRidByIndividualId(individualId, individualIdType));
		responseWrapper.setId(ridId);
		responseWrapper.setVersion(ridVersion);
		responseWrapper.setResponse(ridDto);
		auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.GET_RID_BY_INDIVIDUALID,
				individualId, individualIdType, "Get RID by IndividualId Request success");
		return new ResponseEntity<>(responseWrapper, HttpStatus.OK);
	}

	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getPostSearchIdVidMetadata())")
	@PostMapping(path = "/idvid-metadata/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Search IdVid metadata using Individual Id", description = "Search IdVid metadata using Individual Id", tags = {
			"id-repo-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Request authenticated successfully", content = @Content(schema = @Schema(implementation = IdVidMetadataResponseDTO.class))),
			@ApiResponse(responseCode = "400", description = "No Records Found", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(hidden = true))) })
	/**
	 * Search id vid metadata.
	 * @param idVidMetadataRequestWrapper id vid metadata request wrapper
	 * @param errors errors
	 * @return response entity<response wrapper<id vid metadata response dto>>
	 */
	public ResponseEntity<ResponseWrapper<IdVidMetadataResponseDTO>> searchIdVidMetadata(
			@Validated @RequestBody IdVidMetadataRequestWrapper idVidMetadataRequestWrapper,
			@Parameter(hidden = true) Errors errors) throws IdRepoAppException {
		IdVidMetadataRequestDTO metadataRequest = idVidMetadataRequestWrapper.getRequest();
		String individualId = metadataRequest != null ? metadataRequest.getIndividualId() : null;
		String idType = metadataRequest != null ? metadataRequest.getIdType() : null;
		try {
			DataValidationUtil.validate(errors);
			individualId = metadataRequest.getIndividualId();
			idType = metadataRequest.getIdType();
			IdType individualIdType = Objects.isNull(idType) ? getIdType(individualId) : validator.validateIdType(idType);
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.ID_VID_METADATA,
					individualId, individualIdType, "IdVid metadata search request received");

			IdVidMetadataResponseDTO metadataResponse = idRepoService.getIdVidMetadata(individualId, individualIdType);

			ResponseWrapper<IdVidMetadataResponseDTO> responseWrapper = new ResponseWrapper<>();
			responseWrapper.setId(idvidMetadataId);
			responseWrapper.setVersion(idvidMetadataVersion);
			responseWrapper.setResponse(metadataResponse);

			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.ID_VID_METADATA,
					individualId, individualIdType, "IdVid metadata search request successful");

			return new ResponseEntity<>(responseWrapper, HttpStatus.OK);
		} catch (IdRepoDataValidationException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.ID_VID_METADATA,
					individualId, resolveAuditIdType(idType), e);
			throw new IdRepoAppException(DATA_VALIDATION_FAILED, e);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.ID_VID_METADATA,
					individualId, resolveAuditIdType(idType), e);
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		}
	}

	private IdType resolveAuditIdType(String idType) {
		if (idType == null) {
			return IdType.UIN;
		}
		try {
			return IdType.valueOf(idType);
		} catch (IllegalArgumentException ex) {
			return IdType.UIN;
		}
	}

	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getRemainingUpdateCountByIndividualId())")
	@GetMapping(path = "/{individualId}/update-counts", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get Remaining update count by Individual Id Request", description = "Get Remaining update count by Individual Id Request", tags = {
			"id-repo-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Request authenticated successfully", content = @Content(array = @ArraySchema(schema = @Schema(implementation = IdRepoAppException.class)))),
			@ApiResponse(responseCode = "400", description = "No Records Found", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(hidden = true))) })
	/**
	 * Get remaining update count by individual id.
	 *
	 * @param individualId  UIN, VID, or RID
	 * @param idType        optional explicit ID type
	 * @param attributeList optional list of demographic attributes to check
	 * @return per-attribute remaining update counts
	 */
	public ResponseEntity<ResponseWrapper<AttributeListDto>> getRemainingUpdateCountByIndividualId(
			@PathVariable String individualId, @RequestParam(name = ID_TYPE, required = false) @Nullable String idType,
			@RequestParam(name = "attribute_list", required = false) @Nullable List<String> attributeList)
			throws IdRepoAppException {
		IdType individualIdType = Objects.isNull(idType) ? getIdType(individualId) : validator.validateIdType(idType);
		auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.GET_RID_BY_INDIVIDUALID, individualId,
				individualIdType, "Get Remaining update count by Individual Id Request received");
		ResponseWrapper<AttributeListDto> responseWrapper = new ResponseWrapper<>();
		AttributeListDto attributeListDto = new AttributeListDto();
		Map<String, Integer> countMap = idRepoService.getRemainingUpdateCountByIndividualId(individualId,
				individualIdType, attributeList);
		List<UpdateCountDto> dtoList = countMap.entrySet().stream()
				.map(map -> new UpdateCountDto(map.getKey(),map.getValue()))
				.collect(Collectors.toList());
		auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.GET_RID_BY_INDIVIDUALID, individualId,
				individualIdType, "Get Remaining update count by Individual Id Request success");
		attributeListDto.setAttributes(dtoList);
		responseWrapper.setResponse(attributeListDto);
		return new ResponseEntity<>(responseWrapper, HttpStatus.OK);
	}

	/**
	 * Extracts UIN from the identity payload using the configured JSON path.
	 *
	 * @param request identity request body object (demographics map)
	 * @return UIN string
	 * @throws IdRepoAppException if request is null or UIN path is missing
	 */
	private String getUin(Object request) throws IdRepoAppException {
		if (Objects.isNull(request)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, GET_UIN, "request is null");
			throw new IdRepoAppException(MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), "request"));
		}
		Object uin = null;
		String pathOfUin = EnvUtil.getUinJsonPath();
		try {
			String identity = mapper.writeValueAsString(request);
			JsonPath jsonPath = JsonPath.compile(pathOfUin);
			uin = jsonPath.read(identity);
			return String.valueOf(uin);
		} catch (JsonProcessingException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, GET_UIN, e.getMessage());
			throw new IdRepoAppException(INVALID_REQUEST, e);
		} catch (JsonPathException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_CONTROLLER, GET_UIN, e.getMessage());
			throw new IdRepoAppException(MISSING_INPUT_PARAMETER.getErrorCode(),
					String.format(MISSING_INPUT_PARAMETER.getErrorMessage(), pathOfUin.replace(".", "/")));
		}
	}

	private IdType getIdType(String id) throws IdRepoAppException {
		if (validator.validateUin(id))
			return IdType.UIN;
		if (validator.validateVid(id))
			return IdType.VID;
		return IdType.ID;
	}
}