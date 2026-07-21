package io.mosip.idrepository.pipeline;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.credential.store.service.CredentialStoreService;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;

/**
 * SDK adapter for credential issuance within the consolidated ID Repository JVM.
 * <p>
 * Delegates directly to {@link CredentialStoreService} (no internal HTTP).
 * </p>
 *
 * @see InProcessIdentityClient
 * @see CredentialStoreService#createCredentialIssuance(CredentialServiceRequestDto)
 */
@Component
public class InProcessCredentialClient {

	@Autowired
	@Lazy
	private CredentialStoreService credentialStoreService;

	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * Issues a credential by calling {@link CredentialStoreService} in-process.
	 *
	 * @param request credential issuance request DTO
	 * @return credential service response with issuance details
	 */
	public CredentialServiceResponseDto issueCredential(CredentialServiceRequestDto request) {
		return credentialStoreService.createCredentialIssuance(request);
	}

	/**
	 * Issues a credential and serializes the response to JSON.
	 *
	 * @param request credential issuance request DTO
	 * @return JSON string representation of the issuance response
	 * @throws Exception if JSON serialization fails
	 */
	public String issueCredentialAsJson(CredentialServiceRequestDto request) throws Exception {
		return objectMapper.writeValueAsString(issueCredential(request));
	}
}
