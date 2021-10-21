package io.mosip.credential.request.generator.controller;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.credential.request.generator.dto.CredentialStatusEvent;
import io.mosip.credential.request.generator.exception.CredentialrRequestGeneratorException;
import io.mosip.credential.request.generator.service.CredentialRequestService;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.dto.CredentialIssueResponseDto;
import io.mosip.idrepository.core.dto.CredentialIssueStatusResponse;
import io.mosip.idrepository.core.dto.CredentialRequestIdsDto;
import io.mosip.idrepository.core.dto.PageDto;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.websub.api.annotation.PreAuthenticateContentAndVerifyIntent;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


/**
 * The Class CredentialRequestGeneratorController.
 *
 * @author Sowmya
 */
@RestController
@Api(tags = "Credential Request Renerator")
public class CredentialRequestGeneratorController {
	
	/** The credential request service. */
	@Autowired
	private CredentialRequestService credentialRequestService;



	/**
	 * Credential issue.
	 *
	 * @param credentialIssueRequestDto the credential issue request dto
	 * @return the response entity
	 */
	@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@PostMapping(path = "/requestgenerator", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Create the  credential issuance request", response = CredentialIssueResponseDto.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Created request id successfully"),
			@ApiResponse(code = 400, message = "Unable to get request id") })
	public ResponseEntity<Object> credentialIssue(
			@RequestBody  RequestWrapper<CredentialIssueRequestDto>  credentialIssueRequestDto) {

		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = credentialRequestService
				.createCredentialIssuance(credentialIssueRequestDto.getRequest());
		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseWrapper);
	}
	@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@GetMapping(path = "/cancel/{requestId}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "cancel the credential issuance request", response = CredentialIssueResponseDto.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "cancel the request successfully"),
	@ApiResponse(code=400,message="Unable to cancel the request"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	@ResponseBody
	public ResponseEntity<Object> cancelCredentialRequest(@PathVariable("requestId") String requestId) {

		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = credentialRequestService
				.cancelCredentialRequest(requestId);
		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseWrapper);
	}
	
	@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@GetMapping(path = "/get/{requestId}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "get credential issuance request status", response = CredentialIssueResponseDto.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "get the credential issuance status of request successfully"),
			@ApiResponse(code = 400, message = "Unable to get the status of credential issuance request"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	@ResponseBody
	public ResponseEntity<Object> getCredentialRequestStatus(@PathVariable("requestId") String requestId) {

		ResponseWrapper<CredentialIssueStatusResponse> credentialIssueResponseWrapper = credentialRequestService
				.getCredentialRequestStatus(requestId);
		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseWrapper);
	}
	
	

	@PostMapping(path = "/callback/notifyStatus", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Request authenticated successfully") })
	@PreAuthenticateContentAndVerifyIntent(secret = "test", callback = "/v1/credentialrequest/callback/notifyStatus", topic = "CREDENTIAL_STATUS_UPDATE")
	public ResponseWrapper<?> handleSubscribeEvent( @RequestBody CredentialStatusEvent credentialStatusEvent) throws CredentialrRequestGeneratorException {
		credentialRequestService.updateCredentialStatus(credentialStatusEvent);
		return new ResponseWrapper<>();
	}

	@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@GetMapping(path = "/getRequestIds", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "get credential issuance request ids", response = CredentialRequestIdsDto.class)
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "get credential issuance request ids successfully"),
			@ApiResponse(code = 400, message = "Unable to get credential issuance request ids"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
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

	@PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@GetMapping(path = "/retrigger/{requestId}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "retrigger the credential issuance request", response = CredentialIssueResponseDto.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "retrigger the  the request successfully"),
			@ApiResponse(code = 400, message = "Unable to retrigger the request"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	@ResponseBody
	public ResponseEntity<Object> reprocessCredentialRequest(@PathVariable("requestId") String requestId) {

		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = credentialRequestService
				.retriggerCredentialRequest(requestId);
		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseWrapper);
	}
}
