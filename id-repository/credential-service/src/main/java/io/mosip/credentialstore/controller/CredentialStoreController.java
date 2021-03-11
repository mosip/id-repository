package io.mosip.credentialstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.credentialstore.dto.CredentialTypeResponse;
import io.mosip.credentialstore.service.CredentialStoreService;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * The Class CredentialStoreController.
 * 
 * @author Sowmya
 */
@RestController
@Api(tags = "Credential Store")
public class CredentialStoreController {

	/** The credential store service. */
	@Autowired
	private CredentialStoreService credentialStoreService;


	/**
	 * Credential issue.
	 *
	 * @param credentialServiceRequestDto the credential service request dto
	 * @return the response entity
	 */
	@PreAuthorize("hasAnyRole('CREDENTIAL_ISSUANCE')")
	@PostMapping(path = "/issue", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "create credential", response = CredentialServiceResponseDto.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "create credential successfully"),
			@ApiResponse(code = 400, message = "Unable to create credential ") })
	public ResponseEntity<Object> credentialIssue(
			@RequestBody(required = true) CredentialServiceRequestDto credentialServiceRequestDto) {
		
		CredentialServiceResponseDto credentialIssueResponseDto = credentialStoreService
				.createCredentialIssuance(credentialServiceRequestDto);

		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseDto);

	}


	@GetMapping(path = "/types", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "get the credential types", response = CredentialTypeResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "get the credential types successfully"),
			@ApiResponse(code = 400, message = "Unable get the credential types"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	@ResponseBody
	public ResponseEntity<Object> getCredentialTypes() {

		CredentialTypeResponse credentialTypeResponse = null;

		credentialTypeResponse = credentialStoreService.getCredentialTypes();

		return ResponseEntity.status(HttpStatus.OK).body(credentialTypeResponse);
	}

}
