package io.mosip.credential.request.generator.service;

import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponseDto;

// TODO: Auto-generated Javadoc
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
	public CredentialIssueResponseDto createCredentialIssuance(CredentialIssueRequestDto credentialIssueRequestDto);

	/**
	 * Cancel credential request.
	 *
	 * @param requestId the request id
	 * @return the credential issue response dto
	 */
	public CredentialIssueResponseDto cancelCredentialRequest(String requestId);

}
