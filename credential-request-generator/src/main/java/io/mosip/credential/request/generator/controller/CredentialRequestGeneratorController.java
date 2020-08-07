package io.mosip.credential.request.generator.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.credential.request.generator.dto.CredentialIssueRequestDto;
import io.mosip.credential.request.generator.dto.CredentialIssueResponseDto;
import io.mosip.credential.request.generator.exception.CredentialIssueException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@Api(tags = "credential request generator")
public class CredentialRequestGeneratorController {

	
	// TODO authentication needed when it calls event from resident or idrepo ?
	//@PreAuthorize("hasAnyRole('CREDENTIAL_ISSUANCE')")
	@PostMapping(path = "/requestgenerator", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get the credential issuance request id", response = CredentialIssueResponseDto.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Get request id successfully"),
			@ApiResponse(code = 400, message = "Unable to get request id") })
	public ResponseEntity<Object> credentialIssue(
			@RequestBody(required = true) CredentialIssueRequestDto credentialIssueRequestDto) {

		
		try {
			// TODO to call service layer
			// return
			// ResponseEntity.status(HttpStatus.OK).body(buildCredentialIssueResponse(CredentialIssueResponse));
		} catch (CredentialIssueException e) {
			// TODO create the errorlist
		} finally {
			// TODO create response with error list or response
		}
		return null;
	}
}
