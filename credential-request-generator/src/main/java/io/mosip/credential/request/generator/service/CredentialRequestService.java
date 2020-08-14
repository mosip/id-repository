package io.mosip.credential.request.generator.service;

import io.mosip.credential.request.generator.dto.CredentialIssueRequestDto;
import io.mosip.credential.request.generator.dto.CredentialIssueResponse;


/**
 * The Interface CredentialRequestService.
 *
 * @author Sowmya
 */
public interface CredentialRequestService {
	
	/**
	 * Creates the credential issuance.
	 *
	 * @param credentialIssueRequestDto the credential issue request dto
	 * @return the credential issue response
	 */
	public CredentialIssueResponse createCredentialIssuance(CredentialIssueRequestDto credentialIssueRequestDto);

	public CredentialIssueResponse cancelCredentialRequest(String requestId);

}
