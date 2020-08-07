package io.mosip.credentialstore.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


import io.mosip.credentialstore.dto.CredentialServiceRequestDto;
import io.mosip.credentialstore.dto.CredentialServiceResponseDto;
import io.mosip.credentialstore.exception.CredentialServiceException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@Api(tags = "credential store")
public class CredentialStoreController {

	//TODO is needed here
	//@PreAuthorize("hasAnyRole('CREDENTIAL_ISSUANCE')")
	@PostMapping(path = "/issue", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "get the status of credential", response = CredentialServiceResponseDto.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Get status of credential successfully"),
			@ApiResponse(code = 400, message = "Unable toget status of credential ") })
	public ResponseEntity<Object> credentialIssue(
			@RequestBody(required = true) CredentialServiceRequestDto credentialServiceRequestDto) {

		
		try {
			// TODO to call service layer
			// return
			// ResponseEntity.status(HttpStatus.OK).body(buildCredentialServiceResponse(CredentialServiceResponseDto));
		} catch (CredentialServiceException e) {
			// TODO create the errorlist
		} finally {
			// TODO create response with error list or response
		}
		return null;
	}

}
