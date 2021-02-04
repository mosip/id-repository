package io.mosip.credential.request.generator.batch.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.credential.request.generator.constants.CredentialStatusCode;
import io.mosip.credential.request.generator.constants.LoggerFileConstant;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.util.RestUtil;
import io.mosip.credential.request.generator.util.TrimExceptionMessage;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponse;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;
import io.mosip.idrepository.core.dto.ErrorDTO;
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
	

	/** The Constant LOGGER. */
	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialItemProcessor.class);
	
	private static final String CREDENTIAL_USER = "service-account-mosip-crereq-client";


	@Override
	public CredentialEntity process(CredentialEntity credential) {
		int retryCount = 0;
		TrimExceptionMessage trimMessage = new TrimExceptionMessage();
        try {
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credential.getRequestId(),
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
				LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
						credential.getRequestId(),
			   		 responseObject.toString());
				ErrorDTO error = responseObject.getErrors().get(0);
				credential.setStatusCode(CredentialStatusCode.FAILED.name());
				credential.setStatusComment(error.getMessage());
				retryCount = credential.getRetryCount() != null ? credential.getRetryCount() + 1 : 1;

			}else {
				CredentialServiceResponse credentialServiceResponse=responseObject.getResponse();
				credential.setCredentialId(credentialServiceResponse.getCredentialId());
				credential.setDataShareUrl(credentialServiceResponse.getDataShareUrl());
				credential.setIssuanceDate(credentialServiceResponse.getIssuanceDate());
				credential.setStatusCode(credentialServiceResponse.getStatus());
				credential.setSignature(credentialServiceResponse.getSignature());
				credential.setStatusComment("credentials issued to partner");

			}

			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credential.getRequestId(),
					"ended processing item");
		} catch (IOException e) {

			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credential.getRequestId(),
					ExceptionUtils.getStackTrace(e));
			credential.setStatusCode("FAILED");
			credential.setStatusComment(trimMessage.trimExceptionMessage(e.getMessage()));
			retryCount = credential.getRetryCount() != null ? credential.getRetryCount() + 1 : 1;
		} catch (Exception e) {
			String errorMessage;
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				errorMessage = httpClientException.getResponseBodyAsString();
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				errorMessage = httpServerException.getResponseBodyAsString();
			} else {
				errorMessage = e.getMessage();
			}

			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credential.getRequestId(),
					ExceptionUtils.getStackTrace(e));
			credential.setStatusCode("FAILED");
			credential.setStatusComment(trimMessage.trimExceptionMessage(errorMessage));
			retryCount = credential.getRetryCount() != null ? credential.getRetryCount() + 1 : 1;
		} finally {
			credential.setUpdatedBy(CREDENTIAL_USER);
			credential.setUpdateDateTime(LocalDateTime.now(ZoneId.of("UTC")));
			if (retryCount != 0) {
				credential.setRetryCount(retryCount);
			}
		}
		return credential;
	}



}
