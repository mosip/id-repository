package io.mosip.credential.request.generator.batch.config;

import java.io.IOException;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.credential.request.generator.constants.LoggerFileConstant;
import io.mosip.credential.request.generator.dto.CredentialIssueRequestDto;
import io.mosip.credential.request.generator.dto.CredentialServiceRequestDto;
import io.mosip.credential.request.generator.dto.CredentialServiceResponseDto;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.ApiNotAccessibleException;
import io.mosip.credential.request.generator.logger.CredentialRequestGeneratorLogger;
import io.mosip.credential.request.generator.util.PartnerManageUtil;
import io.mosip.credential.request.generator.util.RestUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;


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

	/** The Constant LOGGER. */
	private static final Logger LOGGER = CredentialRequestGeneratorLogger.getLogger(CredentialItemProcessor.class);

	@Override
	public CredentialEntity process(CredentialEntity credential) {
        try {
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.ID.toString(),
					credential.getRequestId(),
					"CredentialItemProcessor::process()::entry");
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
				LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.ID.toString(),
						credential.getRequestId(), responseObject.toString());
			}
				credential.setStatusCode(responseObject.getStatus());
			LOGGER.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.ID.toString(),
					credential.getRequestId(),
					"CredentialItemProcessor::process()::exit");
		} catch (ApiNotAccessibleException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.ID.toString(),
					credential.getRequestId(), ExceptionUtils.getStackTrace(e));
        	credential.setStatusCode("FAILED");
		} catch (IOException e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.ID.toString(),
					credential.getRequestId(), ExceptionUtils.getStackTrace(e));
			credential.setStatusCode("FAILED");
		} catch (Exception e) {
			LOGGER.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.ID.toString(),
					credential.getRequestId(), ExceptionUtils.getStackTrace(e));
			credential.setStatusCode("FAILED");
		}
		return credential;
	}



}
