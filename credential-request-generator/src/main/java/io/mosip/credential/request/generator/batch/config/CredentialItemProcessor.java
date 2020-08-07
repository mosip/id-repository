package io.mosip.credential.request.generator.batch.config;

import org.springframework.batch.item.ItemProcessor;

import io.mosip.credential.request.generator.entity.CredentialEntity;



public class CredentialItemProcessor implements ItemProcessor<CredentialEntity, CredentialEntity> {

	@Override
	public CredentialEntity process(CredentialEntity credential) throws Exception {
		// TODO get request from entity and map to CredentialIssueRequestDto class
		//call partner management get details about formatters
		//construct CredentialServiceRequestDto and call Credential service api
		//Based on CredentialServiceResponse success or failure update the entity status and return from processor
		return credential;
	}



}
