package io.mosip.idrepository.identity.helper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.dto.IDAEventDTO;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;

/**
 * @author Manoj SP
 *
 */
@Component
public class IdRepoWebSubHelper {

	/** The mosip logger. */
	Logger mosipLogger = IdRepoLogger.getLogger(IdRepoWebSubHelper.class);
	
	@Value("${" + IdRepoConstants.WEB_SUB_PUBLISH_URL + "}")
	public String publisherHubURL;

	@Autowired
	private PublisherClient<String, IDAEventDTO, HttpHeaders> publisher;

	@Autowired
	private TokenIDGenerator tokenIdGenerator;

	@Async
	public void tryRegisteringTopic(String topic) {
		try {
			publisher.registerTopic(topic, publisherHubURL);
		} catch (WebSubClientException e) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), "IdRepoConfig", "init", e.getMessage().toUpperCase());
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), "IdRepoConfig", "init", e.getMessage().toUpperCase());
		}
	}

	@Async
	public void publishEvent(String individualId, List<AuthtypeStatus> authTypeStatusList, String topic,
			String partnerId) {
		IDAEventDTO event = new IDAEventDTO();
		event.setTokenId(tokenIdGenerator.generateTokenID(individualId, partnerId));
		event.setAuthTypeStatusList(authTypeStatusList);
		publisher.publishUpdate(topic, event, MediaType.APPLICATION_JSON_UTF8_VALUE, null, publisherHubURL);
	}

}
