package io.mosip.credential.request.generator.service;

import org.springframework.stereotype.Service;

import io.mosip.credential.request.generator.dto.CredentialStatusEvent;
import io.mosip.credential.request.generator.exception.CredentialrRequestGeneratorException;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueResponse;
import io.mosip.idrepository.core.dto.CredentialIssueStatusResponse;
import io.mosip.idrepository.core.dto.CredentialRequestIdsDto;
import io.mosip.idrepository.core.dto.PageDto;
import io.mosip.kernel.core.http.ResponseWrapper;


// TODO: Auto-generated Javadoc
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

	/**
	 * Update credential status.
	 *
	 * @param credentialStatusEvent the credential status event
	 * @throws CredentialrRequestGeneratorException the credentialr request
	 *                                              generator exception
	 */
	public void updateCredentialStatus(CredentialStatusEvent credentialStatusEvent) throws CredentialrRequestGeneratorException;

	/**
	 * Gets the request ids.
	 *
	 * @param statusCode      the status code
	 * @param effectivedtimes the effectivedtimes
	 * @param page            the page
	 * @param size            the size
	 * @param orderBy         the order by
	 * @param direction       the direction
	 * @return the request ids
	 */
	public ResponseWrapper<PageDto<CredentialRequestIdsDto>> getRequestIds(String statusCode, String effectivedtimes,
			int page, int size,
			String orderBy, String direction);

	/**
	 * Reprocess credential request.
	 *
	 * @param requestId the request id
	 * @return the response wrapper
	 */
	public ResponseWrapper<CredentialIssueResponse> retriggerCredentialRequest(String requestId);

}
