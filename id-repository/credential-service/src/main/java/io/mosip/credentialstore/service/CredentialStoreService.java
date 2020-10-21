package io.mosip.credentialstore.service;

import org.springframework.stereotype.Service;

import io.mosip.credentialstore.dto.CredentialTypeResponse;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;



/**
 * The Interface CredentialStoreService.
 * 
 * @author Sowmya
 */
@Service
public interface CredentialStoreService {

	/**
	 * Creates the credential issuance.
	 *
	 * @param credentialServiceRequestDto the credential service request dto
	 * @return the string
	 */
	public CredentialServiceResponseDto createCredentialIssuance(
			CredentialServiceRequestDto credentialServiceRequestDto);

	/**
	 * Gets the credential types.
	 *
	 * @return the credential types
	 */
	public CredentialTypeResponse getCredentialTypes();
}
