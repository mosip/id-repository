package io.mosip.credentialstore.controller;

import io.mosip.credentialstore.dto.CredentialTypeResponse;
import io.mosip.credentialstore.service.CredentialStoreService;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;
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
import org.springframework.web.bind.annotation.*;

/**
 * The Class CredentialStoreController.
 * 
 * @author Sowmya
 */
@RestController
@Tag(name = "Credential Store", description = "Credential Store Controller")
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
	//@PreAuthorize("hasAnyRole('CREDENTIAL_ISSUANCE')")
	@PreAuthorize("hasAnyRole(@authorizedRoles.getPostissue())")
	@PostMapping(path = "/issue", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "create credential", description = "create credential", tags = { "Credential Store" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "create credential successfully",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = CredentialServiceResponseDto.class)))),
			@ApiResponse(responseCode = "201", description = "Created" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "400", description = "Unable to create credential" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true)))})
	public ResponseEntity<Object> credentialIssue(
			@RequestBody(required = true) CredentialServiceRequestDto credentialServiceRequestDto) {
		
		CredentialServiceResponseDto credentialIssueResponseDto = credentialStoreService
				.createCredentialIssuance(credentialServiceRequestDto);

		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseDto);

	}

	//@PreAuthorize("hasAnyRole(@authorizedRoles.getGetissuetypes())")
	@GetMapping(path = "/types", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "get the credential types", description = "get the credential types", tags = { "Credential Store" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "get the credential types successfully",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = CredentialTypeResponse.class)))),
			@ApiResponse(responseCode = "400", description = "Unable get the credential types" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "Unauthorized" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Forbidden" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Not Found" ,content = @Content(schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "500", description = "Internal Server Error" ,content = @Content(schema = @Schema(hidden = true)))})
	@ResponseBody
	public ResponseEntity<Object> getCredentialTypes() {

		CredentialTypeResponse credentialTypeResponse = null;

		credentialTypeResponse = credentialStoreService.getCredentialTypes();

		return ResponseEntity.status(HttpStatus.OK).body(credentialTypeResponse);
	}

}