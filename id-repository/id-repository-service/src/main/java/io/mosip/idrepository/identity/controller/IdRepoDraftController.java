package io.mosip.idrepository.identity.controller;

import io.mosip.kernel.core.util.DateUtils2;
import jakarta.annotation.PostConstruct;
import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import org.springframework.beans.factory.annotation.Value;
import io.mosip.idrepository.core.dto.DraftResponseDto;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.IdRepoDraftService;
import io.mosip.idrepository.core.util.DataValidationUtil;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Parameter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static io.mosip.idrepository.core.constant.IdRepoConstants.FACE_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.FINGER_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IRIS_EXTRACTION_FORMAT;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;

/**
 * REST controller for draft identity APIs ({@code /idrepository/v1/identity/draft/*}).
 */
@RestController
@Tag(name = "id-repo-draft-controller", description = "Id Repo Draft Controller")
@RequestMapping(path = "/draft")
public class IdRepoDraftController {

	private static final String UIN = "UIN";

	private static final String ID_REPO_DRAFT_CONTROLLER = "IdRepoDraftController";
	private static final String GET_DRAFT_UIN = "getDraftUin";

	/** Mosip logger. */
	private final Logger mosipLogger = IdRepoLogger.getLogger(IdRepoDraftController.class);

	@Autowired
	/** Validator. */
	private IdRequestValidator validator;

	@Autowired
	private IdRepoDraftService<IdRequestDTO, IdResponseDTO> draftService;

	@Autowired
	/** Audit helper. */
	private AuditHelper auditHelper;

	@Autowired
	/** Environment. */
	private Environment environment;

	@Value("${" + IdRepoConstants.RID_VALIDATION_PATTERN + ":" + IdRepoConstants.RID_VALIDATION_PATTERN_DEFAULT + "}")
	/** Rid pattern. */
	private String ridPattern;

	/** Rid compiled pattern. */
	private Pattern ridCompiledPattern;

	@PostConstruct
	/**
	 * Compile pattern.
	 */
	public void compilePattern() {
		ridCompiledPattern = Pattern.compile(ridPattern);
	}

	@InitBinder
	/**
	 * Init binder.
	 * @param binder binder
	 */
	public void initBinder(WebDataBinder binder) {
		binder.addValidators(validator);
	}
	
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getPostdraftcreateregistrationId())")
	//@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@PostMapping(path = "/create/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "createDraft", description = "createDraft", tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	/**
	 * Create draft.
	 * @param registrationId registration id
	 * @param UIN uin
	 * @param uin uin
	 * @return response entity<id response dto>
	 */
	public ResponseEntity<IdResponseDTO> createDraft(@PathVariable String registrationId,
			@RequestParam(name = UIN, required = false) @Nullable String uin)
			throws IdRepoAppException {
		try {
			if (!ridCompiledPattern.matcher(registrationId).matches()) {
				throw new IdRepoAppException(
						IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "Registration Id")
				);
			}
			return new ResponseEntity<>(draftService.createDraft(registrationId, uin), HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_DRAFT_REQUEST_RESPONSE,
					registrationId, IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "createDraft", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_DRAFT_REQUEST_RESPONSE,
					registrationId, IdType.ID, "Create draft requested");
		}
	}

	//@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getPatchdraftupdateregistrationId())")
	@PatchMapping(path = "/update/{registrationId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "updateDraft", description = "updateDraft", tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	/**
	 * Update draft.
	 * @param registrationId registration id
	 * @param request request
	 * @param errors errors
	 * @return response entity<id response dto>
	 */
	public ResponseEntity<IdResponseDTO> updateDraft(@PathVariable String registrationId, @RequestBody IdRequestDTO request,
			@Parameter(hidden = true) Errors errors) throws IdRepoAppException {
		try {
			request.getRequest().setRegistrationId(registrationId);
			validator.validateRequest(request.getRequest(), errors, "update");
			DataValidationUtil.validate(errors);
			return new ResponseEntity<>(draftService.updateDraft(registrationId, request), HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.UPDATE_DRAFT_REQUEST_RESPONSE,
					registrationId, IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "updateDraft", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_DRAFT_REQUEST_RESPONSE,
					registrationId, IdType.ID, "Update draft requested");
		}
	}

	//@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getGetdraftpublishregistrationId())")
	@GetMapping(path = "/publish/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "publishDraft", description = "publishDraft", tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	/**
	 * Publish draft.
	 * @param registrationId registration id
	 * @return response entity<id response dto>
	 */
	public ResponseEntity<IdResponseDTO> publishDraft(@PathVariable String registrationId) throws IdRepoAppException {
		try {
			return new ResponseEntity<>(draftService.publishDraft(registrationId), HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.PUBLISH_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "publishDraft", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, "Publish draft requested");
		}
	}

	//@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getDeletedraftdiscardregistrationId())")
	@DeleteMapping(path = "/discard/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "discardDraft", description = "discardDraft", tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "204", description = "No Content" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	/**
	 * Discard draft.
	 * @param registrationId registration id
	 * @return response entity<id response dto>
	 */
	public ResponseEntity<IdResponseDTO> discardDraft(@PathVariable String registrationId) throws IdRepoAppException {
		try {
			return new ResponseEntity<>(draftService.discardDraft(registrationId), HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.DISCARD_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "discardDraft", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.DISCARD_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, "Discard draft requested");
		}
	}

	//@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getDraftregistrationId())")
	@RequestMapping(method = RequestMethod.HEAD, path = "/{registrationId}")
	@Operation(summary = "hasDraft", description = "hasDraft", tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "204", description = "No Content" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			})
	/**
	 * Has draft.
	 * @param registrationId registration id
	 * @return response entity<void>
	 */
	public ResponseEntity<Void> hasDraft(@PathVariable String registrationId) throws IdRepoAppException {
		try {
			HttpStatus responseStatus = draftService.hasDraft(registrationId) ? HttpStatus.OK : HttpStatus.NO_CONTENT;
			return new ResponseEntity<>(responseStatus);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.HAS_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "hasDraft", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.HAS_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, "Has draft requested");
		}
	}

	//@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getGetdraftregistrationId())")
	@GetMapping(path = "/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "getDraft", description = "getDraft", tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true))),
	})
	/**
	 * Get draft.
	 * @param registrationId registration id
	 * @param FINGER_EXTRACTION_FORMAT finger extraction format
	 * @param fingerExtractionFormat finger extraction format
	 * @param IRIS_EXTRACTION_FORMAT iris extraction format
	 * @param irisExtractionFormat iris extraction format
	 * @param FACE_EXTRACTION_FORMAT face extraction format
	 * @param faceExtractionFormat face extraction format
	 * @return response entity<id response dto>
	 */
	public ResponseEntity<IdResponseDTO> getDraft(@PathVariable String registrationId,
			@RequestParam(name = FINGER_EXTRACTION_FORMAT, required = false) @Nullable String fingerExtractionFormat,
			@RequestParam(name = IRIS_EXTRACTION_FORMAT, required = false) @Nullable String irisExtractionFormat,
			@RequestParam(name = FACE_EXTRACTION_FORMAT, required = false) @Nullable String faceExtractionFormat)
			throws IdRepoAppException {
		try {
			return new ResponseEntity<>(draftService.getDraft(registrationId,
					buildExtractionFormatMap(fingerExtractionFormat, irisExtractionFormat, faceExtractionFormat)),
					HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.GET_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "getDraft", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.GET_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, "Publish draft requested");
		}
	}
	
	//@PreAuthorize("hasAnyRole('REGISTRATION_PROCESSOR')")
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getPutdraftextractbiometricsregistrationId())")
	@PutMapping(path = "/extractbiometrics/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "extractBiometrics", description = "extractBiometrics", tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true))),
	})
	/**
	 * Extract biometrics.
	 * @param registrationId registration id
	 * @param FINGER_EXTRACTION_FORMAT finger extraction format
	 * @param fingerExtractionFormat finger extraction format
	 * @param IRIS_EXTRACTION_FORMAT iris extraction format
	 * @param irisExtractionFormat iris extraction format
	 * @param FACE_EXTRACTION_FORMAT face extraction format
	 * @param faceExtractionFormat face extraction format
	 * @return response entity<id response dto>
	 */
	public ResponseEntity<IdResponseDTO> extractBiometrics(@PathVariable String registrationId,
			@RequestParam(name = FINGER_EXTRACTION_FORMAT, required = false) @Nullable String fingerExtractionFormat,
			@RequestParam(name = IRIS_EXTRACTION_FORMAT, required = false) @Nullable String irisExtractionFormat,
			@RequestParam(name = FACE_EXTRACTION_FORMAT, required = false) @Nullable String faceExtractionFormat) throws IdRepoAppException {
		try {
			return new ResponseEntity<>(draftService.extractBiometrics(registrationId,
					buildExtractionFormatMap(fingerExtractionFormat, irisExtractionFormat, faceExtractionFormat)),
					HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.EXTRACT_BIOMETRICS_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "extractBiometrics", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.EXTRACT_BIOMETRICS_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, "Extract Biometrics draft requested");
		}
	}

	private Map<String, String> buildExtractionFormatMap(String fingerExtractionFormat, String irisExtractionFormat,
			String faceExtractionFormat) {
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
		return extractionFormats;
	}

	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getGetdraftUIN())")
	@GetMapping(path = "/uin/{UIN}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "getDraftUIN", description = "getDraft", tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true))),
	})
	/**
	 * Get draft uin.
	 * @param uin uin
	 * @return response entity<response wrapper<draft response dto>>
	 */
	public ResponseEntity<ResponseWrapper<DraftResponseDto>> getDraftUIN(@PathVariable(value = "UIN") String uin)
			throws IdRepoAppException {
		ResponseWrapper<DraftResponseDto> responseWrapper = new ResponseWrapper<>();
		responseWrapper.setId(environment.getProperty(IdRepoConstants.GET_DRAFT_UIN_ID));
		responseWrapper.setVersion(environment.getProperty(IdRepoConstants.GET_DRAFT_UIN_VERSION));
		responseWrapper.setResponsetime(DateUtils2.getUTCCurrentDateTime());
		try {
			if (!validator.validateUin(uin)) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, GET_DRAFT_UIN, "Invalid uin");
				responseWrapper.setErrors(List.of(new ServiceError(INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(INVALID_INPUT_PARAMETER.getErrorMessage(), IdType.UIN))));
				return new ResponseEntity<>(responseWrapper,
						HttpStatus.BAD_REQUEST);
			} else {
				responseWrapper.setResponse(draftService.getDraftUin(uin));
				return new ResponseEntity<>(responseWrapper,
						HttpStatus.OK);
			}
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.GET_DRAFT_UIN_REQUEST_RESPONSE, UIN,
					IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, GET_DRAFT_UIN, e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.GET_DRAFT_UIN_REQUEST_RESPONSE, UIN,
					IdType.ID, "Get Draft UIN requested");
		}
	}
	
}
