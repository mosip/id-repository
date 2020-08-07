package io.mosip.credential.request.generator.service;

import io.mosip.credential.request.generator.dto.CredentialIssueRequestDto;
import io.mosip.credential.request.generator.dto.CredentialIssueResponse;

public interface CredentialRequestService {
	
	public CredentialIssueResponse createCredentialIssuance(CredentialIssueRequestDto credentialIssueRequestDto);

}
