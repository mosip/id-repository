package io.mosip.credentialstore.util;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import io.mosip.credentialstore.constants.LoggerFileConstant;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;

@Component
public class WebSubUtil {
	@Autowired
	private PublisherClient<String, EventModel, HttpHeaders> pb; 
	   

	/** The config server file storage URL. */
	@Value("${" + IdRepoConstants.WEB_SUB_PUBLISH_URL + "}")
	private String partnerhuburl;


	private static final Logger LOGGER = IdRepoLogger.getLogger(WebSubUtil.class);


	@Retryable(value = { WebSubClientException.class,
			IOException.class }, maxAttemptsExpression = "${mosip.credential.service.retry.maxAttempts}", backoff = @Backoff(delayExpression = "${mosip.credential.service.retry.maxDelay}"))
	public void publishSuccess(String issuer,EventModel eventModel) throws WebSubClientException, IOException{
		String requestId=eventModel.getEvent().getTransactionId();
		registerTopic(issuer, requestId);
        HttpHeaders httpHeaders=new HttpHeaders();
		pb.publishUpdate(issuer+"/"+IDAEventType.CREDENTIAL_ISSUED, eventModel, MediaType.APPLICATION_JSON_UTF8_VALUE, httpHeaders,  partnerhuburl);
		LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
				requestId,
				"Publish the update successfully");
		
	}

	private void registerTopic(String issuer, String requestId) {
		try {
			pb.registerTopic(issuer+"/"+IDAEventType.CREDENTIAL_ISSUED, partnerhuburl);
		}catch(WebSubClientException e){
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Topic already registered");
		}

	}


}
