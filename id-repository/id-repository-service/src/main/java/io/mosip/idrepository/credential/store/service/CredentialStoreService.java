package io.mosip.idrepository.credential.store.service;

import org.springframework.stereotype.Service;

import io.mosip.idrepository.credential.store.dto.CredentialTypeResponse;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;

/**
 * Credential issuance service API ({@code /v1/credentialservice/*}).
 * <p>
 * Resolves partner policy, builds credential payload from identity, encrypts/signs per type,
 * uploads to Data Share, and publishes WebSub status events. Invoked in-process from
 * {@link io.mosip.idrepository.pipeline.InProcessCredentialClient} or via HTTP from batch tasklets.
 * </p>
 *
 * @see io.mosip.idrepository.credential.store.service.impl.CredentialStoreServiceImpl
 * @author Sowmya
 */
@Service
public interface CredentialStoreService {

	/**
	 * Issues a partner credential for the given identity reference and policy.
	 *
	 * @param credentialServiceRequestDto partner id, credential type, id (UIN/VID), sharable attributes
	 * @return issuance response with credential id, data-share URL, signature, and status
	 */
	CredentialServiceResponseDto createCredentialIssuance(CredentialServiceRequestDto credentialServiceRequestDto);

	/**
	 * Lists credential types configured for this deployment.
	 *
	 * @return type metadata including issuers and descriptions
	 */
	CredentialTypeResponse getCredentialTypes();
}
