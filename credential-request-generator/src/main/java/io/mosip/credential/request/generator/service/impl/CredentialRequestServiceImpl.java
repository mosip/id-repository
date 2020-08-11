package io.mosip.credential.request.generator.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.credential.request.generator.dto.CredentialIssueRequestDto;
import io.mosip.credential.request.generator.dto.CredentialIssueResponse;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.CredentialIssueException;
import io.mosip.credential.request.generator.repositary.CredentialRepositary;
import io.mosip.credential.request.generator.service.CredentialRequestService;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;

public class CredentialRequestServiceImpl implements CredentialRequestService {
	
	@Autowired
	CredentialRepositary<CredentialEntity, String> credentialRepositary;
	
	private static final String USER = "MOSIP_SYSTEM";

	@Override
	public CredentialIssueResponse createCredentialIssuance(CredentialIssueRequestDto credentialIssueRequestDto) {

		CredentialIssueResponse credentialIssueResponse= new CredentialIssueResponse();
		try{
			String requestId = generateId();
		
		credentialIssueResponse.setRequestId(requestId);
	    CredentialEntity credential=new CredentialEntity();
		credential.setRequestId(requestId);
		credential.setRequest(credentialIssueRequestDto.toString());
		credential.setStatusCode("NEW");
		credential.setCreateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credential.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
		credential.setCreatedBy(USER);
		// TODO whether to add cellname
		credentialRepositary.save(credential);
	    }catch(DataAccessLayerException e) {
	    	throw new CredentialIssueException(CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorCode(), CredentialRequestErrorCodes.DATA_ACCESS_LAYER_EXCEPTION.getErrorMessage(),e);
	    }
		return credentialIssueResponse;
	}
	private String generateId() {
		return UUID.randomUUID().toString();
	}
    
}
