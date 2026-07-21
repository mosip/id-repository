package io.mosip.idrepository.credential.request.service;

import io.mosip.idrepository.core.dto.*;
import org.springframework.stereotype.Service;

import io.mosip.idrepository.credential.request.dto.CredentialStatusEvent;
import io.mosip.idrepository.credential.request.exception.CredentialRequestGeneratorException;
import io.mosip.kernel.core.http.ResponseWrapper;


/**
 * Credential-request queue and status API ({@code /v1/credentialrequest/*}).
 * <p>
 * Accepts issuance requests from identity or partners, persists them on {@code mosip_credential},
 * and exposes status/cancel/retrigger operations. Spring Batch tasklets drain the queue and
 * call {@link io.mosip.idrepository.credential.store.service.CredentialStoreService}.
 * </p>
 *
 * @see io.mosip.idrepository.credential.request.service.impl.CredentialRequestServiceImpl
 * @author Sowmya
 */
@Service
public interface CredentialRequestService {

	/**
	 * Queues a new credential issuance request for batch processing.
	 * <p>
	 * Persists encrypted request JSON with status {@code NEW} and returns the generated request id.
	 * </p>
	 *
	 * @param credentialIssueRequestDto partner/credential-type issuance parameters
	 * @return wrapper containing request id and initial status
	 */
	ResponseWrapper<CredentialIssueResponse> createCredentialIssuance(CredentialIssueRequest credentialIssueRequestDto);

	/**
	 * Cancels a queued or in-flight credential request.
	 *
	 * @param requestId credential request identifier returned by {@link #createCredentialIssuance}
	 * @return wrapper with updated cancellation status
	 */
	ResponseWrapper<CredentialIssueResponse> cancelCredentialRequest(String requestId);

	/**
	 * Returns current status and metadata for a credential request.
	 *
	 * @param requestId credential request identifier
	 * @return wrapper with status code, credential id, data-share URL when issued, and error comment
	 */
	ResponseWrapper<CredentialIssueStatusResponse> getCredentialRequestStatus(String requestId);

	/**
	 * Applies asynchronous credential status updates from WebSub ({@code CREDENTIAL_STATUS_UPDATE}).
	 * <p>
	 * Invoked by {@code /callback/notifyStatus} when credential store publishes status changes.
	 * </p>
	 *
	 * @param credentialStatusEvent WebSub payload with request id and new status
	 * @throws CredentialRequestGeneratorException if the request id is unknown or update is invalid
	 */
	void updateCredentialStatus(CredentialStatusEvent credentialStatusEvent) throws CredentialRequestGeneratorException;

	/**
	 * Lists credential request ids filtered by status and effective time with pagination.
	 *
	 * @param statusCode       queue status filter (e.g. FAILED)
	 * @param effectivedtimes  optional lower bound on effective timestamp (ISO format)
	 * @param page             zero-based page index
	 * @param size             page size
	 * @param orderBy          sort column (e.g. updateDateTime)
	 * @param direction        ASC or DESC
	 * @return paginated list of request ids matching the filter
	 */
	ResponseWrapper<PageDto<CredentialRequestIdsDto>> getRequestIds(String statusCode, String effectivedtimes,
			int page, int size, String orderBy, String direction);

	/**
	 * Resets a failed request to be picked up again by the reprocess batch job.
	 *
	 * @param requestId credential request identifier
	 * @return wrapper confirming retrigger acceptance
	 */
	ResponseWrapper<CredentialIssueResponse> retriggerCredentialRequest(String requestId);

	/**
	 * Queues credential issuance bound to a registration id (RID) instead of UIN/VID.
	 *
	 * @param request issuance parameters
	 * @param rid     registration identifier used to resolve identity
	 * @return wrapper containing request id and initial status
	 */
	ResponseWrapper<CredentialIssueResponse> createCredentialIssuanceByRid(CredentialIssueRequest request, String rid);
}
