package io.mosip.credential.request.generator.batch.config;

import java.io.IOException;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.credential.request.generator.dto.CredentialIssueRequestDto;
import io.mosip.credential.request.generator.dto.CredentialServiceRequestDto;
import io.mosip.credential.request.generator.dto.CredentialServiceResponseDto;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.ApiNotAccessibleException;
import io.mosip.credential.request.generator.util.PartnerManageUtil;
import io.mosip.credential.request.generator.util.RestUtil;


/**
 * 
 * @author Sowmya
 *
 */
public class CredentialItemProcessor implements ItemProcessor<CredentialEntity, CredentialEntity> {

	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private RestUtil restUtil;
	

	
	@Autowired
	private PartnerManageUtil partnerManageUtil;

	@Override
	public CredentialEntity process(CredentialEntity credential) {
        try {
		CredentialIssueRequestDto credentialIssueRequestDto = mapper.readValue(credential.getRequest(), CredentialIssueRequestDto.class);
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		credentialServiceRequestDto.setCredentialType(credentialIssueRequestDto.getCredentialType());
		credentialServiceRequestDto.setId(credentialIssueRequestDto.getId());
		credentialServiceRequestDto.setIssuer(credentialIssueRequestDto.getIssuer());
		credentialServiceRequestDto.setRecepiant(credentialIssueRequestDto.getIssuer());
		credentialServiceRequestDto.setSharableAttributes(credentialIssueRequestDto.getSharableAttributes());
		credentialServiceRequestDto.setUser(credentialIssueRequestDto.getUser());

			credentialServiceRequestDto
					.setFormatter(partnerManageUtil.getFormatter(credentialIssueRequestDto.getIssuer()));

			String responseString = restUtil.postApi(ApiName.CRDENTIALSERVICE, null, "", "",
					MediaType.APPLICATION_JSON, credentialServiceRequestDto, String.class);

		CredentialServiceResponseDto responseObject = mapper.readValue(responseString, CredentialServiceResponseDto.class);

			if (responseObject != null &&
				responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
			// TODO log error
			}
				credential.setStatusCode(responseObject.getStatus());
		
		} catch (ApiNotAccessibleException e) {
        	// TODO log error 
        	credential.setStatusCode("FAILED");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return credential;
	}



}
