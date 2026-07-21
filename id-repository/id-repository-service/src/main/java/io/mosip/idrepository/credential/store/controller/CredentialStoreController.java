package io.mosip.idrepository.credential.store.controller;

import io.mosip.idrepository.credential.store.dto.CredentialTypeResponse;
import io.mosip.idrepository.credential.store.service.CredentialStoreService;
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
 * REST controller for credential issuance ({@code /v1/credentialservice/*}).
 * <p>
 * Thin HTTP layer over {@link CredentialStoreService}; builds partner credentials from
 * identity data, encrypts/signs per policy, and returns data-share URLs.
 * </p>
 *
 * @see io.mosip.idrepository.credential.store.provider.CredentialProvider
 * @author Sowmya
 */
@RestController
@Tag(name = "Credential Store", description = "Credential Store Controller")
public class CredentialStoreController {

	/** Core credential issuance and type listing service. */
	@Autowired
	private CredentialStoreService credentialStoreService;


	/**
	 * Issues a credential for the given partner policy and identity reference.
	 *
	 * @param credentialServiceRequestDto issuance request (partner, credential type, id, sharable attributes)
	 * @return HTTP 200 with {@link CredentialServiceResponseDto} (credential id, data-share URL, signature)
	 */
	//@PreAuthorize("hasAnyRole('CREDENTIAL_ISSUANCE')")
	@PreAuthorize("hasAnyRole(@credentialAuthorizedRoles.getPostissue())")
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

	/**
	 * Returns credential types supported by this deployment (IdAuth, QR, Verifiable Credential, etc.).
	 *
	 * @return HTTP 200 with {@link CredentialTypeResponse}
	 */
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
		CredentialTypeResponse credentialTypeResponse = credentialStoreService.getCredentialTypes();
		return ResponseEntity.status(HttpStatus.OK).body(credentialTypeResponse);
	}
}