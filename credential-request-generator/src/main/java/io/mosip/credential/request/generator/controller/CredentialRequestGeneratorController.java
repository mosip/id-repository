package io.mosip.credential.request.generator.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.mosip.credential.request.generator.dto.CredentialIssueRequestDto;
import io.mosip.credential.request.generator.dto.CredentialIssueResponse;
import io.mosip.credential.request.generator.dto.CredentialIssueResponseDto;
import io.mosip.credential.request.generator.dto.ErrorDTO;
import io.mosip.credential.request.generator.exception.CredentialIssueException;
import io.mosip.credential.request.generator.service.CredentialRequestService;
import io.mosip.kernel.core.util.DateUtils;
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
@Api(tags = "credential request generator")
public class CredentialRequestGeneratorController {
	
	/** The credential request service. */
	@Autowired
	private CredentialRequestService credentialRequestService;
	
	
	/** The env. */
	@Autowired
	private Environment env;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.credential.request.datetime.pattern";

	/** The Constant CREDENTIAL_REQUEST_SERVICE_ID. */
	private static final String CREDENTIAL_REQUEST_SERVICE_ID = "mosip.credential.request.service.id";

	/** The Constant CREDENTIAL_REQUEST_SERVICE_VERSION. */
	private static final String CREDENTIAL_REQUEST_SERVICE_VERSION = "mosip.credential.request.service.version";

	

	/**
	 * Credential issue.
	 *
	 * @param credentialIssueRequestDto the credential issue request dto
	 * @return the response entity
	 */
	// @PreAuthorize("hasAnyRole('CREDENTIAL_REQUEST')")
	@PostMapping(path = "/requestgenerator", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get the credential issuance request id", response = CredentialIssueResponseDto.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Get request id successfully"),
			@ApiResponse(code = 400, message = "Unable to get request id") })
	public ResponseEntity<Object> credentialIssue(
			@RequestBody(required = true) CredentialIssueRequestDto credentialIssueRequestDto) {
		List<ErrorDTO> errorList =new ArrayList<>();
		CredentialIssueResponseDto credentialIssueResponseDto=new CredentialIssueResponseDto();
		CredentialIssueResponse  credentialIssueResponse=null;
		try {
			
			 credentialIssueResponse=credentialRequestService.createCredentialIssuance(credentialIssueRequestDto);

		} catch (CredentialIssueException e) {
			ErrorDTO error=new ErrorDTO();
			error.setErrorCode(e.getErrorCode());
			error.setMessage(e.getMessage());
			errorList.add(error);
		} finally {
			
			credentialIssueResponseDto.setId(CREDENTIAL_REQUEST_SERVICE_ID);
			credentialIssueResponseDto.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
			credentialIssueResponseDto.setVersion(env.getProperty(CREDENTIAL_REQUEST_SERVICE_VERSION));
	     if(!errorList.isEmpty()) {
	    	 credentialIssueResponseDto.setErrors(errorList);
	      }else {
	    	  credentialIssueResponseDto.setResponse(credentialIssueResponse);
	      }
	   
		}
		  return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseDto);
	}
	
	@GetMapping(path = "/cancel/{requestId}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "cancel the request", response = CredentialIssueResponseDto.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "cancel the request successfully"),
	@ApiResponse(code=400,message="Unable to cancel the request"),
			@ApiResponse(code = 500, message = "Internal Server Error") })
	@ResponseBody
	public ResponseEntity<Object> cancelCredentialRequest(@PathVariable("requestId") String requestId) {
		List<ErrorDTO> errorList = new ArrayList<>();
		CredentialIssueResponseDto credentialIssueResponseDto = new CredentialIssueResponseDto();
		CredentialIssueResponse credentialIssueResponse = null;
		try {

			credentialIssueResponse = credentialRequestService.cancelCredentialRequest(requestId);

		} catch (CredentialIssueException e) {
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(e.getErrorCode());
			error.setMessage(e.getMessage());
			errorList.add(error);
		} finally {

			credentialIssueResponseDto.setId(CREDENTIAL_REQUEST_SERVICE_ID);
			credentialIssueResponseDto
					.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
			credentialIssueResponseDto.setVersion(env.getProperty(CREDENTIAL_REQUEST_SERVICE_VERSION));
			if (!errorList.isEmpty()) {
				credentialIssueResponseDto.setErrors(errorList);
			} else {
				credentialIssueResponseDto.setResponse(credentialIssueResponse);
			}

		}
		return ResponseEntity.status(HttpStatus.OK).body(credentialIssueResponseDto);
	}

}
