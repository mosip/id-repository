package io.mosip.credential.request.generator.controller;

import javax.annotation.Nullable;

import io.mosip.idrepository.core.dto.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.credential.request.generator.dto.CredentialStatusEvent;
import io.mosip.credential.request.generator.exception.CredentialRequestGeneratorException;
import io.mosip.credential.request.generator.init.CredentialInstializer;
import io.mosip.credential.request.generator.init.SubscribeEvent;
import io.mosip.credential.request.generator.service.CredentialRequestService;
import io.mosip.credential.request.generator.validator.RequestValidator;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.websub.api.annotation.PreAuthenticateContentAndVerifyIntent;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * The Class CredentialRequestGeneratorController.
 *
 * @author Sowmya
 */
@RestController
@Tag(name = "Credential Request Generator", description = "Credential Request Generator")
public class CredentialRequestGeneratorController {
	
	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialRequestGeneratorController.class);
	
	/** The credential request service. */
	@Autowired
	private CredentialRequestService credentialRequestService;

	@Autowired
	private CredentialInstializer credentialInstializer;

	@Autowired
	private SubscribeEvent subscribeEvent;

	@Autowired
	RequestValidator requestValidator;

	@Autowired
	JobLauncher jobLauncher;

	/**
	 * Credential issue.
	 *
	 * @param credentialIssueRequestDto the credential issue request dto
	 * @return the response entity
	 */
	//@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostrequestgenerator())")
	@PostMapping(path = "/requestgenerator", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Create the  credential issuance request", description = "Create the  credential issuance request", tags = { "Credential Request Generator" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Created request id successfully"),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "400", description = "Unable to get request id" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> credentialIssue(
			@RequestBody  RequestWrapper<CredentialIssueRequest>  credentialIssueRequestDto) throws IdRepoAppException {
		requestValidator.validateRequestGeneratorRequest(credentialIssueRequestDto);
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = credentialRequestService
				.createCredentialIssuance(credentialIssueRequestDto.getRequest());
		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseWrapper);
	}

	/**
	 * Credential issue.
	 *
	 * @param credentialIssueRequestDto the credential issue request dto
	 * @return the response entity
	 */
	//@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostv2requestgeneratorrid())")
	@PostMapping(path = "/v2/requestgenerator/{rid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Create the  credential issuance request", description = "Create the  credential issuance request", tags = { "Credential Request Generator" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Created request id successfully"),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "400", description = "Unable to get request id" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> credentialIssueByRid(
			@RequestBody  RequestWrapper<CredentialIssueRequestDto>  credentialIssueRequestDto, @PathVariable("rid") String rid) {

		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = credentialRequestService
				.createCredentialIssuanceByRid(credentialIssueRequestDto.getRequest(),rid);
		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseWrapper);
	}
	//@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getGetcancelrequestid())")
	@GetMapping(path = "/cancel/{requestId}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "cancel the credential issuance request", description = "cancel the credential issuance request", tags = { "Credential Request Generator" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "cancel the request successfully",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = CredentialIssueResponseDto.class)))),
			@ApiResponse(responseCode = "400", description = "Unable to cancel the request" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error" ,content = @Content(schema = @Schema(hidden = true)))})
	@ResponseBody
	public ResponseEntity<Object> cancelCredentialRequest(@PathVariable("requestId") String requestId) {

		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = credentialRequestService
				.cancelCredentialRequest(requestId);
		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseWrapper);
	}
	
	//@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getGetgetrequestid())")
	@GetMapping(path = "/get/{requestId}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "get credential issuance request status", description = "get credential issuance request status", tags = { "Credential Request Generator" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "get the credential issuance status of request successfully",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = CredentialIssueResponseDto.class)))),
			@ApiResponse(responseCode = "400", description = "Unable to get the status of credential issuance request" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error" ,content = @Content(schema = @Schema(hidden = true)))})
	@ResponseBody
	public ResponseEntity<Object> getCredentialRequestStatus(@PathVariable("requestId") String requestId) {

		ResponseWrapper<CredentialIssueStatusResponse> credentialIssueResponseWrapper = credentialRequestService
				.getCredentialRequestStatus(requestId);
		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseWrapper);
	}
	
	

	@PostMapping(path = "/callback/notifyStatus", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "callback", description = "callback", tags = { "Credential Request Generator" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Request authenticated successfully"),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "400", description = "Unable to request callback" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error" ,content = @Content(schema = @Schema(hidden = true)))})
	@PreAuthenticateContentAndVerifyIntent(secret = "test", callback = "/v1/credentialrequest/callback/notifyStatus", topic = "CREDENTIAL_STATUS_UPDATE")
	public ResponseWrapper<?> handleSubscribeEvent( @RequestBody CredentialStatusEvent credentialStatusEvent) throws CredentialRequestGeneratorException {
		credentialRequestService.updateCredentialStatus(credentialStatusEvent);
		return new ResponseWrapper<>();
	}

	//@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getGetgetrequestids())")
	@GetMapping(path = "/getRequestIds", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "get credential issuance request ids", description = "get credential issuance request ids", tags = { "Credential Request Generator" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "get credential issuance request ids successfully",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = CredentialRequestIdsDto.class)))),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "400", description = "Unable to get credential issuance request ids" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error" ,content = @Content(schema = @Schema(hidden = true)))})
	@ResponseBody
	public ResponseWrapper<PageDto<CredentialRequestIdsDto>> getRequestIds(
			@RequestParam(value = "statusCode", defaultValue = "FAILED") @ApiParam(value = "get the requested data with statuscode", defaultValue = "FAILED") String statusCode,
			@RequestParam(value = "effectivedtimes") @ApiParam(value = "Effective date time") @Nullable String effectivedtimes,
			@RequestParam(value = "pageNumber", defaultValue = "0") @ApiParam(value = "page number for the requested data", defaultValue = "0") int page,
			@RequestParam(value = "pageSize", defaultValue = "1") @ApiParam(value = "page size for the request data", defaultValue = "1") int size,
			@RequestParam(value = "orderBy", defaultValue = "upd_dtimes") @ApiParam(value = "sort the requested data based on param value", defaultValue = "updateDateTime") String orderBy,
			@RequestParam(value = "direction", defaultValue = "DESC") @ApiParam(value = "order the requested data based on param", defaultValue = "ASC") String direction) {
		ResponseWrapper<PageDto<CredentialRequestIdsDto>> responseWrapper =
				credentialRequestService.getRequestIds(statusCode, effectivedtimes, page, size, orderBy, direction);
		return responseWrapper;
	}

	//@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPutretriggerrequestid())")
	@PutMapping(path = "/retrigger/{requestId}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "retrigger the credential issuance request", description = "retrigger the credential issuance request", tags = { "Credential Request Generator" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "retrigger the  the request successfully",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = CredentialIssueResponseDto.class)))),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "400", description = "Unable to retrigger the request" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error" ,content = @Content(schema = @Schema(hidden = true)))})
	@ResponseBody
	public ResponseEntity<Object> reprocessCredentialRequest(@PathVariable("requestId") String requestId) {

		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = credentialRequestService
				.retriggerCredentialRequest(requestId);
		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseWrapper);
	}
	@GetMapping(path = "/scheduleRetrySubscription")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request authenticated successfully") })
	public String handleReSubscribeEvent() {
		return credentialInstializer.scheduleRetrySubscriptions();
	}
	@GetMapping(path = "/scheduleWebsubSubscription")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request authenticated successfully") })
	public String handleSubscribeEvent() {
		return subscribeEvent.scheduleSubscription();
	}
	
}