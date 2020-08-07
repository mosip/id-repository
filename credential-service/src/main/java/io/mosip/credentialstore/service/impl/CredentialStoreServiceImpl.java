package io.mosip.credentialstore.service.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import io.mosip.credentialstore.dto.CredentialServiceRequestDto;
import io.mosip.credentialstore.dto.CredentialServiceResponseDto;
import io.mosip.credentialstore.dto.PolicyDetailResponseDto;
import io.mosip.credentialstore.service.CredentialStoreService;
import io.mosip.credentialstore.util.PolicyUtil;

@Component
public class CredentialStoreServiceImpl implements CredentialStoreService {

	@Autowired
	private PolicyUtil policyUtil;


    public CredentialServiceResponseDto createCredentialIssuance(
			CredentialServiceRequestDto credentialServiceRequestDto) {
		CredentialServiceResponseDto credentialServiceResponseDto= new CredentialServiceResponseDto() ;
		// TODO Auto-generated method stub
		PolicyDetailResponseDto policyDetailResponseDto = policyUtil.getPolicyDetail(credentialServiceRequestDto.getCredentialType(), credentialServiceRequestDto.getIssuer());
		// TODO if policy details not present then it will be failure
		if (policyDetailResponseDto != null) {
			// TODO call io.mosip.credentialstore.util.IdrepositaryUtil.getData(String, String)
			// check shareableAttributes from policy response  and get values from id repo map
			//TODO need to construct MAP with encrypt the each value if it is specified in policy
			//TODO encryption pin or ref id based on which input
			// TODO create Verifiable Credential document 
			//TODO DATA share api need to call with verifiable credential document
			//TODO sent url and event to event hub
			//TODO update status in credentialIssueResponse
			return credentialServiceResponseDto;
		}
		return null;
	}

}
