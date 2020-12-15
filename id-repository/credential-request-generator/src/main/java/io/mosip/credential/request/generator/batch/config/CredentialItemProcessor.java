package io.mosip.credential.request.generator.batch.config;

import java.io.IOException;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.credential.request.generator.constants.CredentialStatusCode;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.ApiNotAccessibleException;
import io.mosip.credential.request.generator.util.RestUtil;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponse;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;


/**
 * 
 * @author Sowmya
 *
 */
@Component
public class CredentialItemProcessor implements ItemProcessor<CredentialEntity, CredentialEntity> {

	
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private RestUtil restUtil;
	
	
	/** The Constant BIOMETRICS. */
	private static final String PROCESS = "process";

	/** The Constant ID_REPO_SERVICE_IMPL. */
	private static final String CREDENTIAL_ITEM_PROCESSOR = "CredentialItemProcessor";

	/** The Constant LOGGER. */
	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialItemProcessor.class);
	
	private static final String CREDENTIAL_USER = "service-account-mosip-crereq-client";


	@Override
	public CredentialEntity process(CredentialEntity credential) {
        try {
        	LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_PROCESSOR, PROCESS,
					"started processing item");
		CredentialIssueRequestDto credentialIssueRequestDto = mapper.readValue(credential.getRequest(), CredentialIssueRequestDto.class);
		CredentialServiceRequestDto credentialServiceRequestDto=new CredentialServiceRequestDto();
		credentialServiceRequestDto.setCredentialType(credentialIssueRequestDto.getCredentialType());
		credentialServiceRequestDto.setId(credentialIssueRequestDto.getId());
		credentialServiceRequestDto.setIssuer(credentialIssueRequestDto.getIssuer());
		credentialServiceRequestDto.setRecepiant(credentialIssueRequestDto.getIssuer());
		credentialServiceRequestDto.setSharableAttributes(credentialIssueRequestDto.getSharableAttributes());
		credentialServiceRequestDto.setUser(credentialIssueRequestDto.getUser());
		credentialServiceRequestDto.setRequestId(credential.getRequestId());
		credentialServiceRequestDto.setEncrypt(credentialIssueRequestDto.isEncrypt());
		credentialServiceRequestDto.setEncryptionKey(credentialIssueRequestDto.getEncryptionKey());
		credentialServiceRequestDto.setAdditionalData(credentialIssueRequestDto.getAdditionalData());
			String responseString = restUtil.postApi(ApiName.CRDENTIALSERVICE, null, "", "",
					MediaType.APPLICATION_JSON, credentialServiceRequestDto, String.class);

		CredentialServiceResponseDto responseObject = mapper.readValue(responseString, CredentialServiceResponseDto.class);

			if (responseObject != null &&
				responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
			   	LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_PROCESSOR, PROCESS,
			   		 responseObject.toString());
				
				credential.setStatusCode(CredentialStatusCode.FAILED.name());

			}else {
				CredentialServiceResponse credentialServiceResponse=responseObject.getResponse();
				credential.setCredentialId(credentialServiceResponse.getCredentialId());
				credential.setDataShareUrl(credentialServiceResponse.getDataShareUrl());
				credential.setIssuanceDate(credentialServiceResponse.getIssuanceDate());
				credential.setStatusCode(credentialServiceResponse.getStatus());
				credential.setSignature(credentialServiceResponse.getSignature());

			}
			credential.setUpdatedBy(CREDENTIAL_USER);
			LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_PROCESSOR, PROCESS,
					"ended processing item");
		} catch (ApiNotAccessibleException e) {

			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_PROCESSOR, PROCESS,
					ExceptionUtils.getStackTrace(e));
        	credential.setStatusCode("FAILED");
		} catch (IOException e) {

			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_PROCESSOR, PROCESS,
					ExceptionUtils.getStackTrace(e));
			credential.setStatusCode("FAILED");
		} catch (Exception e) {

			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_ITEM_PROCESSOR, PROCESS,
					ExceptionUtils.getStackTrace(e));
			credential.setStatusCode("FAILED");
		}
		return credential;
	}



}
