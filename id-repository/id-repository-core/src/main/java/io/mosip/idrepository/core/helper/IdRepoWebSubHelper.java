package io.mosip.idrepository.core.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.model.EventModel;
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
	private PublisherClient<String, IDAEventDTO, HttpHeaders> authTypePublisher;
	
	@Autowired
	private PublisherClient<String, EventModel, HttpHeaders> idaPublisher;

	@Autowired
	private TokenIDGenerator tokenIdGenerator; 

	@Autowired
	private DummyPartnerCheckUtil dummyCheck;
	
	private Set<String> registeredTopicCache = new HashSet<>();
	
	/*
	 * Cacheable is added to execute topic registration only once per topic
	 */
	@Async
	public void tryRegisteringTopic(String topic) {
		synchronized (registeredTopicCache) {
			//Skip topic registration if already registered
			if(registeredTopicCache.contains(topic)) {
				return;
			} else {
				try {
					authTypePublisher.registerTopic(topic, publisherHubURL);
					registeredTopicCache.add(topic);
				} catch (WebSubClientException e) {
					mosipLogger.warn(IdRepoSecurityManager.getUser(), "IdRepoConfig", "init", e.getMessage().toUpperCase());
				} catch (IdRepoAppUncheckedException e) {
					mosipLogger.warn(IdRepoSecurityManager.getUser(), "IdRepoConfig", "init", e.getMessage().toUpperCase());
				}
			}
		}
	}

	@Async
	public void publishAuthTypeStatusUpdateEvent(String individualId, List<AuthtypeStatus> authTypeStatusList, String topic,
			String partnerId) {
		IDAEventDTO event = new IDAEventDTO();
		event.setTokenId(tokenIdGenerator.generateTokenID(individualId, partnerId));
		event.setAuthTypeStatusList(authTypeStatusList);
		authTypePublisher.publishUpdate(topic, event, MediaType.APPLICATION_JSON_UTF8_VALUE, null, publisherHubURL);
	}
	
	/**
	 * Send event to IDA.
	 *
	 * @param model the model
	 */
	@Async
	public void sendEventToIDA(EventModel model, Consumer<EventModel> idaEventModelConsumer) {
		String partnerId = model.getTopic().split("//")[0];
		if(!dummyCheck.isDummyOLVPartner(partnerId)) {
			try {
				mosipLogger.info(IdRepoSecurityManager.getUser(),  this.getClass().getCanonicalName(), "sendEventToIDA",
						"Trying registering topic: " + model.getTopic());
				this.tryRegisteringTopic(model.getTopic());
			} catch (Exception e) {
				// Exception will be there if topic already registered. Ignore that
				mosipLogger.warn(IdRepoSecurityManager.getUser(),  this.getClass().getCanonicalName(), "sendEventToIDA",
						"Error in registering topic: " + model.getTopic() + " : " + e.getMessage());
			}
			mosipLogger.info(IdRepoSecurityManager.getUser(),  this.getClass().getCanonicalName(), "sendEventToIDA",
					"Publising event to topic: " + model.getTopic());
			idaPublisher.publishUpdate(model.getTopic(), model, MediaType.APPLICATION_JSON_VALUE, null,
					publisherHubURL);
		}
		
		if(idaEventModelConsumer != null) {
			idaEventModelConsumer.accept(model);
		}
	}

}
