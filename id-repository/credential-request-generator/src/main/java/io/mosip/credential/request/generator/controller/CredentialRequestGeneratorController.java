package io.mosip.credential.request.generator.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


import io.mosip.credential.request.generator.service.CredentialRequestService;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.dto.CredentialIssueResponseDto;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
	@ApiOperation(value = "Get the credential issuance request id", response = CredentialIssueResponseDto.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Get request id successfully"),
			@ApiResponse(code = 400, message = "Unable to get request id") })
	public ResponseEntity<Object> credentialIssue(
			@RequestBody  RequestWrapper<CredentialIssueRequestDto>  credentialIssueRequestDto) {

		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = credentialRequestService
				.createCredentialIssuance(credentialIssueRequestDto.getRequest());
		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseWrapper);
	}
	
	@GetMapping(path = "/cancel/{requestId}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "cancel the request", response = CredentialIssueResponseDto.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "cancel the request successfully"),
	@ApiResponse(code=400,message="Unable to cancel the request"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	@ResponseBody
	public ResponseEntity<Object> cancelCredentialRequest(@PathVariable("requestId") String requestId) {

		ResponseWrapper<CredentialIssueResponse> credentialIssueResponseWrapper = credentialRequestService
				.cancelCredentialRequest(requestId);
		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseWrapper);
	}

}
