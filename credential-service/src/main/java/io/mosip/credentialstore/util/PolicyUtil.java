package io.mosip.credentialstore.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.dto.ShareableAttributeDto;

@Component
public class PolicyUtil {


	public PolicyDetailResponseDto getPolicyDetail(String policyId, String issuer) {
		// partner id issuer
		// TODO call REST api of partner management
		// Now its Mocked to give PolicyDetailResponseDto
		List<ShareableAttributeDto> sharableAttributesList = new ArrayList<ShareableAttributeDto>();
		PolicyDetailResponseDto policyDetailResponse = new PolicyDetailResponseDto();
		policyDetailResponse.setEncryptionNeeded(true);
		policyDetailResponse.setExtensionAllowed(true);
		policyDetailResponse.setValidForInMinutes(60);
		policyDetailResponse.setTransactionsAllowed(2);
		policyDetailResponse.setShareDomain("mosip.ip");
		policyDetailResponse.setSha256("");
		policyDetailResponse.setShareableAttributes(sharableAttributesList);
		return policyDetailResponse;
	                	 
	}


}
