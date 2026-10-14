package io.mosip.idrepository.identity.controller;

import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.dto.DraftResponseDto;
import io.mosip.idrepository.core.dto.IdRequestDTO;
import io.mosip.idrepository.core.dto.IdRequestV2DTO;
import io.mosip.idrepository.core.dto.RequestDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.IdRepoDraftService;
import io.mosip.idrepository.core.util.DataValidationUtil;
import io.mosip.idrepository.identity.dto.CreateDraftV2RequestDto;
import io.mosip.idrepository.identity.dto.UpdateDraftUinDataRequestDto;
import io.mosip.idrepository.identity.validator.IdRequestValidator;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * <p>
 * A draft holds identity changes from one registration packet until Registration
 * Processor publishes or discards it. Two families of endpoints are exposed:
 * <ul>
 * <li><b>V1</b> ({@code /create}, {@code /update}, {@code /publish}, {@code /discard},
 * {@code /{registrationId}}, {@code /extractbiometrics}, {@code /uin}): draft files
 * are stored under the UIN hash path.</li>
 * <li><b>V2</b> ({@code /v2/create}, {@code /uindata}, {@code /v2/update},
 * {@code /v2/publish}, {@code /v2/{registrationId}}, {@code /v2/extractbiometrics},
 * {@code /v2/discard}): draft files are stored under {@code _draft/{ridHash}} and are
 * moved to the live path only on publish. V2 also supports LOST packets, whose UIN is
 * stamped after ABIS deduplication.</li>
 * </ul>
 * Every endpoint is guarded by a role list from the {@code identityAuthorizedRoles}
 * bean, and every request and failure is audited.
 */
@RestController
@Tag(name = "id-repo-draft-controller", description = "Id Repo Draft Controller")
@RequestMapping(path = "/draft")
public class IdRepoDraftController {

	/** Audit id used for the get-draft-by-UIN endpoint, which has no registration id. */
	private static final String UIN = "UIN";

	/** Class name used as the logger component id. */
	private static final String ID_REPO_DRAFT_CONTROLLER = "IdRepoDraftController";

	/** Method name used in logs of {@link #getDraftUIN(String)}. */
	private static final String GET_DRAFT_UIN = "getDraftUin";

	/** Logger for request failures. */
	private final Logger mosipLogger = IdRepoLogger.getLogger(IdRepoDraftController.class);

	@Autowired
	/** Validates UINs and identity request bodies; also registered on the data binder. */
	private IdRequestValidator validator;

	/** Draft service that implements both the V1 and V2 draft flows. */
	@Autowired
	private IdRepoDraftService<IdRequestDTO, IdResponseDTO> draftService;

	@Autowired
	/** Records an audit event for every draft request and every failure. */
	private AuditHelper auditHelper;

	@Autowired
	/** Source of the response id and version of the get-draft-by-UIN endpoint. */
	private Environment environment;

	@Value("${" + IdRepoConstants.RID_VALIDATION_PATTERN + ":" + IdRepoConstants.RID_VALIDATION_PATTERN_DEFAULT + "}")
	/** Regular expression a registration id must match; compiled once into {@link #ridCompiledPattern}. */
	private String ridPattern;

	/** Compiled {@link #ridPattern}, used to validate registration ids on create. */
	private Pattern ridCompiledPattern;

	@PostConstruct
	/**
	 * Compiles {@link #ridPattern} once at startup so requests do not recompile it.
	 *
	 * @throws java.util.regex.PatternSyntaxException when the configured pattern is invalid
	 */
	public void compilePattern() {
		ridCompiledPattern = Pattern.compile(ridPattern);
	}

	@InitBinder
	/**
	 * Registers {@link #validator} on the data binder when it supports the bound target
	 * type, so request bodies are validated before the handler runs.
	 *
	 * @param binder data binder of the current request
	 */
	public void initBinder(WebDataBinder binder) {
		if (binder.getTarget() != null && validator.supports(binder.getTarget().getClass())) {
			binder.addValidators(validator);
		}
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

	/**
	 * Collects the optional extraction-format query parameters into a map keyed by
	 * {@code fingerExtractionFormat}, {@code irisExtractionFormat} and
	 * {@code faceExtractionFormat}. Null parameters are left out, so an empty map
	 * means no template extraction is requested.
	 *
	 * @param fingerExtractionFormat finger SDK format, or {@code null}
	 * @param irisExtractionFormat   iris SDK format, or {@code null}
	 * @param faceExtractionFormat   face SDK format, or {@code null}
	 * @return modality key to extraction format; never {@code null}
	 */
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

	/**
	 * Creates a V2 identity draft for the given registration ID.
	 * <p>
	 * Use this instead of {@link #createDraft} when the packet should follow the V2
	 * draft storage path (ridHash). Typical callers are Registration Processor packet
	 * stages:
	 * <ul>
	 * <li>NEW — omit {@code uin} and leave {@code generateUin} true so a UIN is allocated.</li>
	 * <li>UPDATE — pass the resident's existing {@code uin}.</li>
	 * <li>LOST — set {@code generateUin} false and omit {@code uin}; stamp the matched
	 * UIN later with {@link #updateDraftUinData} after ABIS.</li>
	 * </ul>
	 *
	 * @param registrationId registration ID of the packet
	 * @param request create-draft options ({@code uin}, {@code generateUin})
	 * @return draft create response
	 * @throws IdRepoAppException if the RID is invalid or the draft cannot be created
	 */
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getPostdraftcreateregistrationId())")
	@PostMapping(path = "/v2/create/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "createDraftV2",
			description = "Creates a draft for NEW, UPDATE, or LOST packets.",
			tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<IdResponseDTO> createDraftV2(
			@Parameter(description = "Registration ID of the draft.")
			@PathVariable String registrationId,
			@RequestBody CreateDraftV2RequestDto request) throws IdRepoAppException {
		try {
			if (!ridCompiledPattern.matcher(registrationId).matches()) {
				throw new IdRepoAppException(
						IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), "Registration Id")
				);
			}
			return new ResponseEntity<>(
					draftService.createDraftV2(registrationId, request.getUin(), request.isGenerateUin()),
					HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_DRAFT_REQUEST_RESPONSE,
					registrationId, IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "createDraftV2", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.CREATE_DRAFT_REQUEST_RESPONSE,
					registrationId, IdType.ID, "Create draft v2 requested");
		}
	}

	/**
	 * Stamps a UIN on a LOST draft after ABIS identifies the matched identity.
	 * <p>
	 * Call this only after {@link #createDraftV2} with {@code generateUin=false}.
	 * It does not replace {@link #updateDraftV2}: packet demographics, biometrics, and
	 * documents still go through update. Missing live demographic fields are backfilled
	 * here so the draft can be published.
	 *
	 * @param registrationId registration ID of the LOST draft
	 * @param request body containing the matched UIN
	 * @return draft update response
	 * @throws IdRepoAppException if the draft is missing, the UIN is invalid, or the UIN
	 *         does not match an existing identity
	 */
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getPatchdraftupdateregistrationId())")
	@PatchMapping(path = "/uindata/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "updateDraftUinData",
			description = "Stamps a UIN on an existing LOST draft after ABIS resolves the matched registration's UIN.",
			tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<IdResponseDTO> updateDraftUinData(
			@Parameter(description = "Registration ID of the draft.")
			@PathVariable String registrationId,
			@RequestBody UpdateDraftUinDataRequestDto request) throws IdRepoAppException {
		try {
			if (!validator.validateUin(request.getUin())) {
				throw new IdRepoAppException(
						IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorCode(),
						String.format(IdRepoErrorConstants.INVALID_INPUT_PARAMETER.getErrorMessage(), IdType.UIN)
				);
			}
			return new ResponseEntity<>(draftService.updateDraftUinData(registrationId, request.getUin()), HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.UPDATE_DRAFT_REQUEST_RESPONSE,
					registrationId, IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "updateDraftUinData", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.UPDATE_DRAFT_REQUEST_RESPONSE,
					registrationId, IdType.ID, "Update draft UIN data requested");
		}
	}

	/**
	 * Updates identity, biometric, and supporting-document data on an existing V2 draft.
	 * <p>
	 * Use after {@link #createDraftV2} (and for LOST packets, alongside
	 * {@link #updateDraftUinData}) when Registration Processor has packet data to merge
	 * into the draft. Prefer this over {@link #updateDraft} so files are written on the
	 * V2 ridHash path that {@link #publishDraftV2} and {@link #getDraftV2} read.
	 * {@code verifiedAttributes} uses the identity v2 object form (trust framework,
	 * verification process, claims, metadata).
	 *
	 * @param registrationId registration ID of the draft
	 * @param request identity update payload in the identity v2 envelope
	 * @param errors request-validation errors populated by the binder
	 * @return draft update response
	 * @throws IdRepoAppException if validation fails or the draft cannot be updated
	 */
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getPatchdraftupdateregistrationId())")
	@PatchMapping(path = "/v2/update/{registrationId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "updateDraftV2",
			description = "Updates draft identity and biometric data.",
			tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<IdResponseDTO> updateDraftV2(
			@Parameter(description = "Registration ID of the draft.")
			@PathVariable String registrationId,
			@RequestBody IdRequestV2DTO request, @Parameter(hidden = true) Errors errors) throws IdRepoAppException {
		try {
			IdRequestDTO legacyRequest = request.toLegacyRequest();
			if (legacyRequest.getRequest() == null) {
				legacyRequest.setRequest(new RequestDTO());
			}
			legacyRequest.getRequest().setRegistrationId(registrationId);
			validator.validateRequest(legacyRequest.getRequest(), errors, "update");
			DataValidationUtil.validate(errors);
			validator.validateVerificationMetadata(legacyRequest.getRequest().getVerifiedAttributesMetadata());
			return new ResponseEntity<>(draftService.updateDraftV2(registrationId, legacyRequest), HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.UPDATE_DRAFT_REQUEST_RESPONSE,
					registrationId, IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "updateDraftV2", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.UPDATE_DRAFT_REQUEST_RESPONSE,
					registrationId, IdType.ID, "Update draft v2 requested");
		}
	}

	/**
	 * Publishes a completed V2 draft as the live identity.
	 * <p>
	 * Use when packet processing has finished successfully (identity updated, and for
	 * LOST packets the UIN already stamped). This copies draft object-store content to
	 * the live path and removes the draft. Do not call it for a rejected or incomplete
	 * packet — use {@link #discardDraftV2} instead.
	 *
	 * @param registrationId registration ID of the draft to publish
	 * @return publish response
	 * @throws IdRepoAppException if the draft is missing or cannot be published
	 */
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getGetdraftpublishregistrationId())")
	@GetMapping(path = "/v2/publish/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "publishDraftV2",
			description = "Publishes a draft to the ID Repository.",
			tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<IdResponseDTO> publishDraftV2(
			@Parameter(description = "Registration ID of the draft.")
			@PathVariable String registrationId) throws IdRepoAppException {
		try {
			return new ResponseEntity<>(draftService.publishDraftV2(registrationId), HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.PUBLISH_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "publishDraftV2", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.PUBLISH_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, "Publish draft v2 requested");
		}
	}

	/**
	 * Retrieves a V2 draft by registration ID.
	 * <p>
	 * Use during processing when a stage needs to read draft content (for example ABIS,
	 * manual adjudication, or verification). Optional {@code type} limits the payload to
	 * demographics, biometrics, supporting documents, or all. Extraction-format query
	 * parameters apply when biometrics are returned.
	 *
	 * @param registrationId registration ID of the draft
	 * @param fingerExtractionFormat optional finger biometric extraction format
	 * @param irisExtractionFormat optional iris biometric extraction format
	 * @param faceExtractionFormat optional face biometric extraction format
	 * @param type optional content selector; omitted or blank defaults to all
	 * @return draft response for the requested type
	 * @throws IdRepoAppException if the draft is missing or {@code type} is invalid
	 */
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getGetdraftregistrationId())")
	@GetMapping(path = "/v2/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "getDraftV2",
			description = "Retrieves a draft by registration ID.",
			tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true))),
	})
	public ResponseEntity<IdResponseDTO> getDraftV2(
			@Parameter(description = "Registration ID of the draft.")
			@PathVariable String registrationId,
			@Parameter(description = "Finger biometric extraction format.")
			@RequestParam(name = FINGER_EXTRACTION_FORMAT, required = false) @Nullable String fingerExtractionFormat,
			@Parameter(description = "Iris biometric extraction format.")
			@RequestParam(name = IRIS_EXTRACTION_FORMAT, required = false) @Nullable String irisExtractionFormat,
			@Parameter(description = "Face biometric extraction format.")
			@RequestParam(name = FACE_EXTRACTION_FORMAT, required = false) @Nullable String faceExtractionFormat,
			@Parameter(
					description = "Draft content to return. Omitted or blank defaults to all.",
					schema = @Schema(
							allowableValues = { "demographics", "biometrics", "supportingdocuments", "all" },
							defaultValue = "all"))
			@RequestParam(name = "type", required = false) @Nullable String type)
			throws IdRepoAppException {
		try {
			return new ResponseEntity<>(draftService.getDraftV2(registrationId,
					buildExtractionFormatMap(fingerExtractionFormat, irisExtractionFormat, faceExtractionFormat),
					type),
					HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.GET_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "getDraftV2", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.GET_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, "Get draft v2 requested");
		}
	}

	/**
	 * Extracts biometric templates from a V2 draft.
	 * <p>
	 * Use this method for template extraction from the draft CBEFF on the V2 ridHash
	 * path. Optional extraction-format query parameters select the SDK format for
	 * finger, iris, and face.
	 *
	 * @param registrationId registration ID of the draft
	 * @param fingerExtractionFormat optional finger biometric extraction format
	 * @param irisExtractionFormat optional iris biometric extraction format
	 * @param faceExtractionFormat optional face biometric extraction format
	 * @return extraction result
	 * @throws IdRepoAppException if the draft is missing or extraction fails
	 */
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getPutdraftextractbiometricsregistrationId())")
	@PutMapping(path = "/v2/extractbiometrics/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "extractBiometricsV2",
			description = "Extracts biometric templates for a draft.",
			tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true))),
	})
	public ResponseEntity<IdResponseDTO> extractBiometricsV2(
			@Parameter(description = "Registration ID of the draft.")
			@PathVariable String registrationId,
			@Parameter(description = "Finger biometric extraction format.")
			@RequestParam(name = FINGER_EXTRACTION_FORMAT, required = false) @Nullable String fingerExtractionFormat,
			@Parameter(description = "Iris biometric extraction format.")
			@RequestParam(name = IRIS_EXTRACTION_FORMAT, required = false) @Nullable String irisExtractionFormat,
			@Parameter(description = "Face biometric extraction format.")
			@RequestParam(name = FACE_EXTRACTION_FORMAT, required = false) @Nullable String faceExtractionFormat)
			throws IdRepoAppException {
		try {
			return new ResponseEntity<>(draftService.extractBiometricsV2(registrationId,
					buildExtractionFormatMap(fingerExtractionFormat, irisExtractionFormat, faceExtractionFormat)),
					HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.EXTRACT_BIOMETRICS_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "extractBiometricsV2", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.EXTRACT_BIOMETRICS_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, "Extract Biometrics draft v2 requested");
		}
	}

	/**
	 * Discards a V2 draft and its stored files.
	 * <p>
	 * Removes the draft record and associated ridHash object-store objects.
	 *
	 * @param registrationId registration ID of the draft to discard
	 * @return discard response
	 * @throws IdRepoAppException if the draft is missing or cannot be discarded
	 */
	@PreAuthorize("hasAnyRole(@identityAuthorizedRoles.getDeletedraftdiscardregistrationId())")
	@DeleteMapping(path = "/v2/discard/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "discardDraftV2",
			description = "Discards a draft.",
			tags = { "id-repo-draft-controller" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OK"),
			@ApiResponse(responseCode = "204", description = "No Content" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<IdResponseDTO> discardDraftV2(
			@Parameter(description = "Registration ID of the draft.")
			@PathVariable String registrationId) throws IdRepoAppException {
		try {
			return new ResponseEntity<>(draftService.discardDraftV2(registrationId), HttpStatus.OK);
		} catch (IdRepoAppException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.DISCARD_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, e);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_DRAFT_CONTROLLER, "discardDraftV2", e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} finally {
			auditHelper.audit(AuditModules.ID_REPO_CORE_SERVICE, AuditEvents.DISCARD_DRAFT_REQUEST_RESPONSE, registrationId,
					IdType.ID, "Discard draft v2 requested");
		}
	}
}
