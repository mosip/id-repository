package io.mosip.credentialstore.provider.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.dto.ShareableAttribute;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.util.JsonUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;


/**
 * The Class IdAuthProvider.
 * 
 * @author Sowmya
 */
@Component
public class IdAuthProvider implements CredentialProvider {
	
	
	@Autowired
    Utilities utilities;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.credentialstore.provider.CredentialProvider#
	 * getFormattedCredentialData(io.mosip.credentialstore.dto.
	 * PolicyDetailResponseDto,
	 * io.mosip.credentialstore.dto.CredentialServiceRequestDto, java.util.Map)
	 */
	@Override
	public DataProviderResponse getFormattedCredentialData(PolicyDetailResponseDto policyDetailResponseDto,
			CredentialServiceRequestDto credentialServiceRequestDto, Map<String, Object> sharableAttributeMap)
			throws CredentialFormatterException {

		try {
			List<ShareableAttribute> shareableAttributes = policyDetailResponseDto.getPolicies()
					.getShareableAttributes();
		Map<String, Object> formattedMap=new HashMap<>();
		for (ShareableAttribute attribute : shareableAttributes) {
				Object value = sharableAttributeMap.get(attribute.getAttributeName());
				if (attribute.isEncrypted()) {
					// TODO use zero knowledge encryption to encrypt the value then put in map
					formattedMap.put(attribute.getAttributeName(), value);
				} else {
					formattedMap.put(attribute.getAttributeName(), value);
				}

		}
			String data = JsonUtil.objectMapperObjectToJson(formattedMap);
			DataProviderResponse dataProviderResponse=new DataProviderResponse();
			dataProviderResponse.setFormattedData(data.getBytes());
			String credentialId = utilities.generateId();
			dataProviderResponse.setCredentialId(credentialId);
			return dataProviderResponse;
		} catch (IOException e) {
			throw new CredentialFormatterException(e);
		}


	}

}
