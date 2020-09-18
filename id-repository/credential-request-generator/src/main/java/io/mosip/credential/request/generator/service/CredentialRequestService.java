package io.mosip.credential.request.generator.service;

import org.springframework.stereotype.Service;

import io.mosip.credential.request.generator.dto.CredentialStatusEvent;
import io.mosip.credential.request.generator.exception.CredentialrRequestGeneratorException;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.dto.CredentialIssueStatusResponse;
import io.mosip.kernel.core.http.ResponseWrapper;


/**
 * The Interface CredentialRequestService.
 *
 * @author Sowmya
 */
@Service
public interface CredentialRequestService {
	
	/**
	 * Creates the credential issuance.
	 *
	 * @param credentialIssueRequestDto the credential issue request dto
	 * @return the credential issue response
	 */
	public ResponseWrapper<CredentialIssueResponse> createCredentialIssuance(CredentialIssueRequestDto credentialIssueRequestDto);

	/**
	 * Cancel credential request.
	 *
	 * @param requestId the request id
	 * @return the credential issue response dto
	 */
	public ResponseWrapper<CredentialIssueResponse> cancelCredentialRequest(String requestId);
	
	
	/**
	 * Cancel credential request.
	 *
	 * @param requestId the request id
	 * @return the credential issue response dto
	 */
	public ResponseWrapper<CredentialIssueStatusResponse> getCredentialRequestStatus(String requestId);

	public void updateCredentialStatus(CredentialStatusEvent credentialStatusEvent) throws CredentialrRequestGeneratorException;

}
