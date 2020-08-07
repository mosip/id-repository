package io.mosip.credential.request.generator.service.impl;

import java.util.UUID;

import io.mosip.credential.request.generator.dto.CredentialIssueRequestDto;
import io.mosip.credential.request.generator.dto.CredentialIssueResponse;
import io.mosip.credential.request.generator.service.CredentialRequestService;

public class CredentialRequestServiceImpl implements CredentialRequestService {

	@Override
	public CredentialIssueResponse createCredentialIssuance(CredentialIssueRequestDto credentialIssueRequestDto) {

		CredentialIssueResponse credentialIssueResponse= new CredentialIssueResponse();
		String requestId = generateId();
		credentialIssueResponse.setRequestId(requestId);
		// TODO create CredentialEntity with NEW status and store in CredentialRepositary
		// TODO need to instiate here again batch or it will be running?
		return null;
	}
	private String generateId() {
		return UUID.randomUUID().toString();
	}

}
