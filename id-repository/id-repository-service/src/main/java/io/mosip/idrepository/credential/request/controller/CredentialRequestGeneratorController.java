package io.mosip.idrepository.credential.request.controller;

import io.mosip.idrepository.credential.request.dto.CredentialStatusEvent;
import io.mosip.idrepository.credential.request.exception.CredentialRequestGeneratorException;
import io.mosip.idrepository.credential.request.init.CredentialInstializer;
import io.mosip.idrepository.credential.request.init.SubscribeEvent;
import io.mosip.idrepository.credential.request.service.CredentialRequestService;
import io.mosip.idrepository.credential.request.validator.RequestValidator;
import io.mosip.idrepository.core.dto.*;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.websub.api.annotation.PreAuthenticateContentAndVerifyIntent;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import javax.annotation.Nullable;


/**
 * REST controller for the credential request queue ({@code /v1/credentialrequest/*}).
 * <p>
 * Queues issuance work on {@code mosip_credential}, exposes status/cancel/retrigger APIs,
 * and receives WebSub callbacks for asynchronous status updates.
 * </p>
 *
 * @see CredentialRequestService
 * @author Sowmya
 */
@RestController
@Tag(name = "Credential Request Generator", description = "Credential Request Generator")
public class CredentialRequestGeneratorController {

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialRequestGeneratorController.class);

	/** Queue and status business logic. */
	@Autowired
	private CredentialRequestService credentialRequestService;

	/** Schedules WebSub subscription retries on startup. */
	@Autowired
	private CredentialInstializer credentialInstializer;

	/** Registers WebSub topic subscription for credential status updates. */
	@Autowired
	private SubscribeEvent subscribeEvent;

	/** Validates incoming request-generator payloads. */
	@Autowired
	private RequestValidator requestValidator;

	/**
	 * Creates a credential issuance request and issues it synchronously in-process.
	 *
	 * @param credentialIssueRequestDto MOSIP request wrapper with issuance parameters
	 * @return HTTP 200 with request id and status
	 * @throws IdRepoAppException if validation fails
	 */
	//@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@PreAuthorize("hasAnyRole(@credReqAuthorizedRoles.getPostrequestgenerator())")
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
	 * Creates a credential issuance request resolved by registration id (RID).
	 *
	 * @param credentialIssueRequestDto MOSIP request wrapper with issuance parameters
	 * @param rid                       registration identifier
	 * @return HTTP 200 with request id and status
	 */
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
			@RequestBody  RequestWrapper<CredentialIssueRequest>  credentialIssueRequestDto, @PathVariable("rid") String rid) {
		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = credentialRequestService
				.createCredentialIssuanceByRid(credentialIssueRequestDto.getRequest(),rid);
		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseWrapper);
	}

	/**
	 * Cancels a queued credential issuance request.
	 *
	 * @param requestId credential request identifier
	 * @return HTTP 200 with cancellation result
	 */
	//@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@PreAuthorize("hasAnyRole(@credReqAuthorizedRoles.getGetcancelrequestid())")
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
	
	/**
	 * Returns status for a single credential request.
	 *
	 * @param requestId credential request identifier
	 * @return HTTP 200 with issuance status and artifact metadata
	 */
	//@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@PreAuthorize("hasAnyRole(@credReqAuthorizedRoles.getGetgetrequestid())")
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

	/**
	 * WebSub callback for {@code CREDENTIAL_STATUS_UPDATE} topic.
	 *
	 * @param credentialStatusEvent authenticated WebSub payload
	 * @return empty success wrapper
	 * @throws CredentialRequestGeneratorException if status update cannot be applied
	 */
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

	/**
	 * Lists credential request ids with pagination and status filter (operations/support API).
	 *
	 * @param statusCode      status filter (default FAILED)
	 * @param effectivedtimes optional effective-time lower bound
	 * @param page            page number
	 * @param size            page size
	 * @param orderBy         sort field
	 * @param direction       sort direction
	 * @return paginated request id list
	 */
	//@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@PreAuthorize("hasAnyRole(@credReqAuthorizedRoles.getGetgetrequestids())")
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
			@RequestParam(value = "statusCode", defaultValue = "FAILED") @Parameter(description = "get the requested data with statuscode") String statusCode,
			@RequestParam(value = "effectivedtimes") @Parameter(description = "Effective date time") @Nullable String effectivedtimes,
			@RequestParam(value = "pageNumber", defaultValue = "0") @Parameter(description = "page number for the requested data") int page,
			@RequestParam(value = "pageSize", defaultValue = "1") @Parameter(description = "page size for the request data") int size,
			@RequestParam(value = "orderBy", defaultValue = "updateDateTime") @Parameter(description = "sort the requested data based on param value") String orderBy,
			@RequestParam(value = "direction", defaultValue = "DESC") @Parameter(description = "order the requested data based on param") String direction) {
        return credentialRequestService.getRequestIds(statusCode, effectivedtimes, page, size, orderBy, direction);
	}

	/**
	 * Retriggers a failed credential request and issues it synchronously in-process.
	 *
	 * @param requestId credential request identifier
	 * @return HTTP 200 with retrigger confirmation
	 */
	//@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@PreAuthorize("hasAnyRole(@credReqAuthorizedRoles.getPutretriggerrequestid())")
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

	/**
	 * Admin endpoint to retry WebSub subscription registration.
	 *
	 * @return subscription scheduling result message
	 */
	@GetMapping(path = "/scheduleRetrySubscription")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request authenticated successfully") })
	public String handleReSubscribeEvent() {
		return credentialInstializer.scheduleRetrySubscriptions();
	}

	/**
	 * Admin endpoint to register WebSub subscription for credential status updates.
	 *
	 * @return subscription scheduling result message
	 */
	@GetMapping(path = "/scheduleWebsubSubscription")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request authenticated successfully") })
	public String handleSubscribeEvent() {
		return subscribeEvent.scheduleSubscription();
	}
	
}