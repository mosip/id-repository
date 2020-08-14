package io.mosip.credentialstore.provider.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.mosip.credentialstore.dto.CredentialServiceRequestDto;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.dto.ShareableAttribute;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.util.JsonUtil;


/**
 * The Class IdAuthProvider.
 * 
 * @author Sowmya
 */
@Component
public class IdAuthProvider implements CredentialProvider {

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.credentialstore.provider.CredentialProvider#
	 * getFormattedCredentialData(io.mosip.credentialstore.dto.
	 * PolicyDetailResponseDto,
	 * io.mosip.credentialstore.dto.CredentialServiceRequestDto, java.util.Map)
	 */
	@Override
	public byte[] getFormattedCredentialData(PolicyDetailResponseDto policyDetailResponseDto,
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
			return data.getBytes();
		} catch (IOException e) {
			throw new CredentialFormatterException(e);
		}


	}

}
