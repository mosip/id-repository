package io.mosip.credentialstore.service;

import org.springframework.stereotype.Service;


import io.mosip.credentialstore.dto.CredentialServiceRequestDto;
import io.mosip.credentialstore.dto.CredentialServiceResponseDto;

@Service
public interface CredentialStoreService {

	public CredentialServiceResponseDto createCredentialIssuance(CredentialServiceRequestDto credentialServiceRequestDto);
}
