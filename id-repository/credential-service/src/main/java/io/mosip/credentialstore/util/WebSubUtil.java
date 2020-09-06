package io.mosip.credentialstore.util;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import io.mosip.credentialstore.constants.CredentialConstants;

import io.mosip.idrepository.core.dto.EventModel;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;

@Component
public class WebSubUtil {
	@Autowired
	private PublisherClient<String, EventModel, HttpHeaders> pb; 
	   

	/** The config server file storage URL. */
	@Value("${mosip.partnerhuburl}")
	private String partnerhuburl;

	@Autowired
	RestUtil restUtil;
	
	private static final Logger LOGGER = IdRepoLogger.getLogger(WebSubUtil.class);


	private static final String WEBSUBUTIL = "WebSubUtil";


	private static final String REGISTERTOPIC = "registerTopic";
	
	public void publishSuccess(String issuer,EventModel eventModel) throws WebSubClientException, IOException{
        registerTopic(issuer);
        HttpHeaders httpHeaders=new HttpHeaders();
		httpHeaders.add("Cookie",restUtil.getToken());
		pb.publishUpdate(issuer+"/"+CredentialConstants.CREDENTIAL_ISSUED, eventModel, MediaType.APPLICATION_JSON_UTF8_VALUE, httpHeaders,  partnerhuburl); 
	
		
	}

	private void registerTopic(String issuer) {
		try {
			pb.registerTopic(issuer+"/"+CredentialConstants.CREDENTIAL_ISSUED, partnerhuburl);
		}catch(WebSubClientException e){
			LOGGER.error(IdRepoSecurityManager.getUser(), WEBSUBUTIL, REGISTERTOPIC,
					"Topic already registered");
		}
		
	}


}
