package io.mosip.credential.request.generator.batch.config;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credential.request.generator.dto.CredentialIssueRequestDto;
import io.mosip.credential.request.generator.dto.CredentialServiceRequestDto;
import io.mosip.credential.request.generator.dto.CredentialServiceResponseDto;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.ApiNotAccessibleException;
import io.mosip.credential.request.generator.util.RestUtil;



public class CredentialItemProcessor implements ItemProcessor<CredentialEntity, CredentialEntity> {

	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private RestUtil restUtil;
	
	@Autowired
	private Environment environment;
	
	@Override
	public CredentialEntity process(CredentialEntity credential) throws Exception {
        try {
		CredentialIssueRequestDto credentialIssueRequestDto = mapper.readValue(credential.getRequest(), CredentialIssueRequestDto.class);
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		credentialServiceRequestDto.setCredentialType(credentialIssueRequestDto.getCredentialType());
		credentialServiceRequestDto.setId(credentialIssueRequestDto.getId());
		credentialServiceRequestDto.setIssuer(credentialIssueRequestDto.getIssuer());
		credentialServiceRequestDto.setRecepiant(credentialIssueRequestDto.getIssuer());
		credentialServiceRequestDto.setSharableAttributes(credentialIssueRequestDto.getSharableAttributes());
		credentialServiceRequestDto.setUser(credentialIssueRequestDto.getUser());
		credentialServiceRequestDto.setFormatter("extraction");
		//TODO call partner management get details about formatters handle exception also
		HttpEntity<CredentialServiceRequestDto> httpEntity = new HttpEntity<>(credentialServiceRequestDto,
				new HttpHeaders());
        
		String responseString = restUtil.postApi(environment.getProperty("CRDENTIALSERVICE"), MediaType.APPLICATION_JSON, httpEntity, String.class);

		CredentialServiceResponseDto responseObject = mapper.readValue(responseString, CredentialServiceResponseDto.class);

			if (responseObject != null &&
				responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
			// TODO log error
			credential.setStatusCode("FAILED");
			} else {
			credential.setStatusCode(responseObject.getStatus());
			}
		
		} catch (ApiNotAccessibleException e) {
        	// TODO log error 
        	credential.setStatusCode("FAILED");
        }
		return credential;
	}



}
