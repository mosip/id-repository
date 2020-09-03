package io.mosip.credentialstore.provider.impl;

import java.util.Map;

import org.springframework.stereotype.Component;

import io.mosip.credentialstore.dto.DataProviderResponse;

import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
@Component
public class QrCodeProvider implements CredentialProvider {

	@Override
	public DataProviderResponse getFormattedCredentialData(	Map<String,Boolean> encryptMap,
			CredentialServiceRequestDto credentialServiceRequestDto, Map<String, Object> sharableAttributeMap)
			throws CredentialFormatterException {
		// TODO Auto-generated method stub
		return null;
	}

}
