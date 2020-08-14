package io.mosip.credentialstore.util;

import java.util.Map;

import org.springframework.stereotype.Component;

import io.mosip.credentialstore.dto.CredentialServiceRequestDto;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;

@Component
public class EncryptionUtil {

	public Map<String, Object> encrypt(Map<String, Object> sharableAttributeMap,
			CredentialServiceRequestDto credentialServiceRequestDto, PolicyDetailResponseDto policyDetailResponseDto) {
		// TODO need to construct MAP with encrypt the each value if it is specified in
		// policy
		// TODO encryption pin or ref id based on which input
		return null;
	}
}
