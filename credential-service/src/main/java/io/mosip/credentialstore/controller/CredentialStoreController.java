package io.mosip.credentialstore.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.credentialstore.dto.CredentialServiceRequestDto;
import io.mosip.credentialstore.dto.CredentialServiceResponseDto;
import io.mosip.credentialstore.dto.CredentialTypeResponse;
import io.mosip.credentialstore.dto.ErrorDTO;
import io.mosip.credentialstore.exception.CredentialServiceException;
import io.mosip.credentialstore.service.CredentialStoreService;
import io.mosip.kernel.core.util.DateUtils;
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
@Api(tags = "credential store")
public class CredentialStoreController {

	/** The credential store service. */
	@Autowired
	private CredentialStoreService credentialStoreService;

	/** The env. */
	@Autowired
	private Environment env;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	/** The Constant CREDENTIAL_SERVICE_SERVICE_ID. */
	private static final String CREDENTIAL_SERVICE_SERVICE_ID = "mosip.credential.service.service.id";

	/** The Constant CREDENTIAL_SERVICE_SERVICE_VERSION. */
	private static final String CREDENTIAL_SERVICE_SERVICE_VERSION = "mosip.credential.service.service.version";

	/**
	 * Credential issue.
	 *
	 * @param credentialServiceRequestDto the credential service request dto
	 * @return the response entity
	 */
	//@PreAuthorize("hasAnyRole('CREDENTIAL_ISSUANCE')")
	@PostMapping(path = "/issue", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "get the status of credential", response = CredentialServiceResponseDto.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Get status of credential successfully"),
			@ApiResponse(code = 400, message = "Unable toget status of credential ") })
	public ResponseEntity<Object> credentialIssue(
			@RequestBody(required = true) CredentialServiceRequestDto credentialServiceRequestDto) {
		List<ErrorDTO> errorList = new ArrayList<>();
		CredentialServiceResponseDto credentialIssueResponseDto = new CredentialServiceResponseDto();
		String status = null;
		
		try {
			status = credentialStoreService.createCredentialIssuance(credentialServiceRequestDto);

		} catch (CredentialServiceException e) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(e.getErrorCode());
			error.setMessage(e.getMessage());
			errorList.add(error);
			status = "FAILED";
		} finally {

			credentialIssueResponseDto.setId(CREDENTIAL_SERVICE_SERVICE_ID);
			credentialIssueResponseDto
					.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
			credentialIssueResponseDto.setVersion(env.getProperty(CREDENTIAL_SERVICE_SERVICE_VERSION));
			credentialIssueResponseDto.setStatus(status);
			if (!errorList.isEmpty()) {
				credentialIssueResponseDto.setErrors(errorList);
			}

		}
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
