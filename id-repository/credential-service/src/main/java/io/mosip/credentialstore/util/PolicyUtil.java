package io.mosip.credentialstore.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import io.mosip.credentialstore.dto.DataSharePolicies;
import io.mosip.credentialstore.dto.Policies;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.dto.ShareableAttribute;

@Component
public class PolicyUtil {


	public PolicyDetailResponseDto getPolicyDetail(String policyId, String issuer) {


		// TODO call REST api of policy manager
		// PolicyResponse contains errors and PolicyDetailResponseDto
		// Now its Mocked to give PolicyDetailResponseDto
		PolicyDetailResponseDto policyDetailResponseDto = new PolicyDetailResponseDto();
		policyDetailResponseDto.setPolicyId("45678451034176");
		policyDetailResponseDto.setVersion("1.1");
		policyDetailResponseDto.setPolicyName("Digital QR Code Policy");
		policyDetailResponseDto.setPolicyDesc("");
		DataSharePolicies dataSharePolicies = new DataSharePolicies();
		dataSharePolicies.setEncryptionType("partnerBased");
		dataSharePolicies.setShareDomain("mosip.io");
		dataSharePolicies.setTransactionsAllowed(2);
		dataSharePolicies.setValidForInMinutes(30);
		Policies policies = new Policies();
		policies.setDataSharePolicies(dataSharePolicies);
		List<ShareableAttribute> sharableAttributesList = new ArrayList<ShareableAttribute>();
		ShareableAttribute shareableAttribute1 = new ShareableAttribute();
		shareableAttribute1.setAttributeName("fullName");
		shareableAttribute1.setEncrypted(true);
		sharableAttributesList.add(shareableAttribute1);
		ShareableAttribute shareableAttribute2 = new ShareableAttribute();
		shareableAttribute2.setAttributeName("dateOfBirth");
		shareableAttribute2.setEncrypted(true);
		sharableAttributesList.add(shareableAttribute2);
		ShareableAttribute shareableAttribute3 = new ShareableAttribute();
		shareableAttribute3.setAttributeName("face");
		shareableAttribute3.setEncrypted(true);
		shareableAttribute3.setFormat("mock");
		sharableAttributesList.add(shareableAttribute3);
		policies.setShareableAttributes(sharableAttributesList);
		policyDetailResponseDto.setPolicies(policies);
		return policyDetailResponseDto;
	                	 
	}


}
