package io.mosip.idrepository.core.util;

import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.WEB_SUB_PUBLISH_URL;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.EventType;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.dto.VidInfoDTO;
import io.mosip.idrepository.core.dto.VidsInfosDTO;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.websub.model.Event;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.model.Type;
import io.mosip.kernel.core.websub.spi.PublisherClient;

/**
 * The Class CredentialRequestManager.
 * 
 * @author Loganathan Sekar
 */
@Component
public class CredentialRequestManager {
	
	/** The Constant mosipLogger. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(CredentialRequestManager.class);
	
	/** The Constant TOKEN. */
	private static final String TOKEN = "TOKEN";

	/** The Constant ID_HASH. */
	private static final String ID_HASH = "id_hash";

	/** The Constant EXPIRY_TIMESTAMP. */
	private static final String EXPIRY_TIMESTAMP = "expiry_timestamp";

	/** The Constant TRANSACTION_LIMIT. */
	private static final String TRANSACTION_LIMIT = "transaction_limit";

	/** The Constant ID_REPO. */
	private static final String ID_REPO = "ID_REPO";

	/** The Constant IDA. */
	private static final String IDA = "IDA";

	/** The Constant PARTNER_ACTIVE_STATUS. */
	private static final String PARTNER_ACTIVE_STATUS = "Active";

	/** The Constant AUTH. */
	private static final String AUTH = "auth";

	/** The Constant ACTIVE. */
	private static final String ACTIVE = "ACTIVE";

	/** The Constant BLOCKED. */
	private static final String BLOCKED = "BLOCKED";
	
	/** The Constant REVOKED. */
	private static final String REVOKED = "REVOKED";
	
	/** The env. */
	@Autowired
	private Environment env;
	
	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;
	
	/** The rest helper. */
	@Autowired
	private RestHelper restHelper;

	/** The rest builder. */
	@Autowired
	private RestRequestBuilder restBuilder;
	
	/** The security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	/** The ida event type namespace. */
	@Value("${id-repo-ida-event-type-namespace:mosip}")
	private String idaEventTypeNamespace;

	/** The ida event type name. */
	@Value("${id-repo-ida-event-type-name:ida}")
	private String idaEventTypeName;

	/** The credential type. */
	@Value("${id-repo-ida-credential-type:" + AUTH + "}")
	private String credentialType;

	/** The credential recepiant. */
	@Value("${id-repo-ida-credential-recepiant:" + IDA + "}")
	private String credentialRecepiant;

	/** The pb. */
	@Autowired
	private PublisherClient<String, EventModel, HttpHeaders> pb;

	/** The token ID generator. */
	@Autowired
	private TokenIDGenerator tokenIDGenerator;
	
	/**
	 * Notify uin credential.
	 *
	 * @param uin the uin
	 * @param expiryTimestamp the expiry timestamp
	 * @param status the status
	 * @param isUpdate the is update
	 * @param txnId the txn id
	 * @param saltRetreivalFunction the salt retreival function
	 * @param credentialResponseConsumer the credential response consumer
	 * @param idaEventModelConsumer 
	 */
	public void notifyUinCredential(String uin, LocalDateTime expiryTimestamp, String status, boolean isUpdate, String txnId, IntFunction<String> saltRetreivalFunction, BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer, Consumer<EventModel> idaEventModelConsumer) {
		try {
			List<VidInfoDTO> vidInfoDtos = null;
			if (isUpdate) {
				RestRequestDTO restRequest = restBuilder.buildRequest(RestServicesConstants.RETRIEVE_VIDS_BY_UIN, null,
						VidsInfosDTO.class);
				restRequest.setUri(restRequest.getUri().replace("{uin}", uin));
				VidsInfosDTO response = restHelper.requestSync(restRequest);
				vidInfoDtos = response.getResponse();
			}

			List<String> partnerIds = getPartnerIds();

			if ((status != null && isUpdate) && (!ACTIVE.equals(status) || expiryTimestamp != null)) {
				// Event to be sent to IDA for deactivation/blocked uin state
				sendUINEventToIDA(uin, expiryTimestamp, status, vidInfoDtos, partnerIds, txnId, id -> securityManager.getIdHash(id, saltRetreivalFunction), idaEventModelConsumer);
			} else {
				// For create uin, or update uin with null expiry (active status), send event to
				// credential service.
				sendUinEventsToCredService(uin, expiryTimestamp, isUpdate, vidInfoDtos, partnerIds, saltRetreivalFunction, credentialRequestResponseConsumer);
			}

		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "notify", e.getMessage());
		}
	}
	
	/**
	 * Notify VID credential.
	 *
	 * @param uin the uin
	 * @param status the status
	 * @param vids the vids
	 * @param isUpdated the is updated
	 * @param saltRetreivalFunction the salt retreival function
	 * @param credentialResponseConsumer the credential response consumer
	 * @param idaEventModelConsumer 
	 */
	public void notifyVIDCredential(String uin, String status, List<VidInfoDTO> vids, boolean isUpdated, IntFunction<String> saltRetreivalFunction, BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer, Consumer<EventModel> idaEventModelConsumer) {
		try {
			List<String> partnerIds = getPartnerIds();
			if (isUpdated) {
				sendVIDEventsToIDA(status, vids, partnerIds, idaEventModelConsumer);
			} else {
				sendVidEventsToCredService(uin, status, vids, isUpdated, partnerIds, saltRetreivalFunction, credentialRequestResponseConsumer);
			}
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "getPartnerIds",
					e.getMessage());
		}
	}

	/**
	 * Gets the partner ids.
	 *
	 * @return the partner ids
	 */
	@SuppressWarnings("unchecked")
	private List<String> getPartnerIds() {
		try {
			Map<String, Object> responseWrapperMap = restHelper
					.requestSync(restBuilder.buildRequest(RestServicesConstants.PARTNER_SERVICE, null, Map.class));
			Object response = responseWrapperMap.get("response");
			if (response instanceof Map) {
				Object partners = ((Map<String, ?>) response).get("partners");
				if (partners instanceof List) {
					List<Map<String, Object>> partnersList = (List<Map<String, Object>>) partners;
					return partnersList.stream()
							.filter(partner -> PARTNER_ACTIVE_STATUS.equalsIgnoreCase((String) partner.get("status")))
							.map(partner -> (String) partner.get("partnerID")).collect(Collectors.toList());
				}
			}
		} catch (RestServiceException | IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "getPartnerIds", e.getMessage());
		}
		return Collections.emptyList();
	}

	/**
	 * Send UIN event to IDA.
	 *
	 * @param uin the uin
	 * @param expiryTimestamp the expiry timestamp
	 * @param status the status
	 * @param vidInfoDtos the vid info dtos
	 * @param partnerIds the partner ids
	 * @param txnId the txn id
	 * @param getIdHashFunction the get id hash function
	 * @param idaEventModelConsumer 
	 */
	private void sendUINEventToIDA(String uin, LocalDateTime expiryTimestamp, String status, List<VidInfoDTO> vidInfoDtos,
			List<String> partnerIds, String txnId, UnaryOperator<String> getIdHashFunction, Consumer<EventModel> idaEventModelConsumer) {
		List<EventModel> eventList = new ArrayList<>();
		EventType eventType = BLOCKED.equals(status) ? IDAEventType.REMOVE_ID : IDAEventType.DEACTIVATE_ID;
		eventList.addAll(createIdaEventModel(eventType, uin, expiryTimestamp, null, partnerIds, txnId, getIdHashFunction.apply(uin))
				.collect(Collectors.toList()));

		if (vidInfoDtos != null) {
			List<EventModel> idaEvents = vidInfoDtos.stream()
					.flatMap(vidInfoDTO -> createIdaEventModel(eventType, vidInfoDTO.getVid(), expiryTimestamp,
							vidInfoDTO.getTransactionLimit(), partnerIds, txnId,
							vidInfoDTO.getHashAttributes().get(ID_HASH)))
					.collect(Collectors.toList());
			eventList.addAll(idaEvents);
		}

		sendEventsToIDA(eventList, eventType, idaEventModelConsumer);
	}

	private void sendEventsToIDA(List<EventModel> eventList, EventType eventType, Consumer<EventModel> idaEventModelConsumer) {
		eventList.forEach(eventDto -> {
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "notify",
					"notifying IDA for event" + eventType.toString());
			sendEventToIDA(eventDto, idaEventModelConsumer);
			mosipLogger.info(IdRepoSecurityManager.getUser(),  this.getClass().getCanonicalName(), "notify",
					"notified IDA for event" + eventType.toString());
		});
	}
	
	/**
	 * Send VID events to IDA.
	 *
	 * @param status the status
	 * @param vids the vids
	 * @param partnerIds the partner ids
	 * @param idaEventModelConsumer 
	 */
	private void sendVIDEventsToIDA(String status,  List<VidInfoDTO> vids, List<String> partnerIds, Consumer<EventModel> idaEventModelConsumer) {
		EventType eventType;
		if (env.getProperty(VID_ACTIVE_STATUS).equals(status)) {
			eventType = IDAEventType.ACTIVATE_ID;
		} else if (REVOKED.equals(status)) {
			eventType = IDAEventType.REMOVE_ID;
		} else {
			eventType = IDAEventType.DEACTIVATE_ID;
		}
		String transactionId = "";//TODO
		List<EventModel> eventDtos = vids.stream()
				.flatMap(vid -> createIdaEventModel(eventType, 
						vid.getVid(),
						eventType.equals(IDAEventType.ACTIVATE_ID) ? vid.getExpiryTimestamp() : DateUtils.getUTCCurrentDateTime(),
								vid.getTransactionLimit(), partnerIds, transactionId,
								vid.getHashAttributes().get(ID_HASH)))
				.collect(Collectors.toList());
		sendEventsToIDA(eventDtos, eventType, idaEventModelConsumer);

	}

	/**
	 * Creates the ida event model.
	 *
	 * @param eventType the event type
	 * @param id the id
	 * @param expiryTimestamp the expiry timestamp
	 * @param transactionLimit the transaction limit
	 * @param partnerIds the partner ids
	 * @param transactionId the transaction id
	 * @param idHash the id hash
	 * @return the stream
	 */
	private Stream<EventModel> createIdaEventModel(EventType eventType, String id, LocalDateTime expiryTimestamp,
			Integer transactionLimit, List<String> partnerIds, String transactionId, String idHash) {
		return partnerIds.stream().map(partner -> createEventModel(eventType, id, expiryTimestamp, transactionLimit,
				transactionId, partner, idHash));
	}

	/**
	 * Creates the event model.
	 *
	 * @param eventType the event type
	 * @param id the id
	 * @param expiryTimestamp the expiry timestamp
	 * @param transactionLimit the transaction limit
	 * @param transactionId the transaction id
	 * @param partner the partner
	 * @param idHash the id hash
	 * @return the event model
	 */
	private EventModel createEventModel(EventType eventType, String id, LocalDateTime expiryTimestamp,
			Integer transactionLimit, String transactionId, String partner, Object idHash) {
		EventModel model = new EventModel();
		model.setPublisher(ID_REPO);
		String dateTime = DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime());
		model.setPublishedOn(dateTime);
		Event event = new Event();
		event.setTimestamp(dateTime);
		String eventId = UUID.randomUUID().toString();
		event.setId(eventId);
		event.setTransactionId(transactionId);
		Type type = new Type();
		type.setNamespace(idaEventTypeNamespace);
		type.setName(idaEventTypeName);
		event.setType(type);
		Map<String, Object> data = new HashMap<>();
		data.put(ID_HASH, idHash);
		if (eventType.equals(IDAEventType.DEACTIVATE_ID)) {
			data.put(EXPIRY_TIMESTAMP, DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		} else {
			if (expiryTimestamp != null) {
				data.put(EXPIRY_TIMESTAMP, DateUtils.formatToISOString(expiryTimestamp));
			}
		}
		data.put(TRANSACTION_LIMIT, transactionLimit);
		event.setData(data);
		model.setEvent(event);
		model.setTopic(partner + "/" + eventType.toString());
		return model;
	}

	/**
	 * Send event to IDA.
	 *
	 * @param model the model
	 */
	private void sendEventToIDA(EventModel model, Consumer<EventModel> idaEventModelConsumer) {
		try {
			mosipLogger.info(IdRepoSecurityManager.getUser(),  this.getClass().getCanonicalName(), "sendEventToIDA",
					"Trying registering topic: " + model.getTopic());
			pb.registerTopic(model.getTopic(), env.getProperty(WEB_SUB_PUBLISH_URL));
		} catch (Exception e) {
			// Exception will be there if topic already registered. Ignore that
			mosipLogger.warn(IdRepoSecurityManager.getUser(),  this.getClass().getCanonicalName(), "sendEventToIDA",
					"Error in registering topic: " + model.getTopic() + " : " + e.getMessage());
		}
		mosipLogger.info(IdRepoSecurityManager.getUser(),  this.getClass().getCanonicalName(), "sendEventToIDA",
				"Publising event to topic: " + model.getTopic());
		pb.publishUpdate(model.getTopic(), model, MediaType.APPLICATION_JSON_VALUE, null,
				env.getProperty(WEB_SUB_PUBLISH_URL));
		
		if(idaEventModelConsumer != null) {
			idaEventModelConsumer.accept(model);
		}
	}

	/**
	 * Send uin events to cred service.
	 *
	 * @param uin the uin
	 * @param expiryTimestamp the expiry timestamp
	 * @param isUpdate the is update
	 * @param vidInfoDtos the vid info dtos
	 * @param partnerIds the partner ids
	 * @param saltRetreivalFunction the salt retreival function
	 * @param credentialResponseConsumer the credential response consumer
	 */
	private void sendUinEventsToCredService(String uin, LocalDateTime expiryTimestamp, boolean isUpdate,
			List<VidInfoDTO> vidInfoDtos, List<String> partnerIds, IntFunction<String> saltRetreivalFunction, BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {
		List<CredentialIssueRequestDto> eventRequestsList = new ArrayList<>();
		eventRequestsList.addAll(partnerIds.stream().map(partnerId -> {
			String token = tokenIDGenerator.generateTokenID(uin, partnerId);
			return createCredReqDto(uin, partnerId, expiryTimestamp, null, token, IdType.UIN.getIdType(),
					securityManager.getIdHashAndAttributes(uin, saltRetreivalFunction));
		}).collect(Collectors.toList()));

		if (vidInfoDtos != null) {
			List<CredentialIssueRequestDto> vidRequests = vidInfoDtos.stream().flatMap(vidInfoDTO -> {
				LocalDateTime vidExpiryTime = Objects.isNull(expiryTimestamp) ? vidInfoDTO.getExpiryTimestamp()
						: expiryTimestamp;
				return partnerIds.stream().map(partnerId -> {
					String token = tokenIDGenerator.generateTokenID(uin, partnerId);
					return createCredReqDto(vidInfoDTO.getVid(), partnerId, vidExpiryTime,
							vidInfoDTO.getTransactionLimit(), token, IdType.VID.getIdType(),
							vidInfoDTO.getHashAttributes());
				});
			}).collect(Collectors.toList());
			eventRequestsList.addAll(vidRequests);
		}

		sendRequestToCredService(eventRequestsList, isUpdate, credentialRequestResponseConsumer);
	}
	
	/**
	 * Send vid events to cred service.
	 *
	 * @param uin the uin
	 * @param status the status
	 * @param vids the vids
	 * @param isUpdate the is update
	 * @param partnerIds the partner ids
	 * @param saltRetreivalFunction the salt retreival function
	 * @param credentialResponseConsumer the credential response consumer
	 */
	private void sendVidEventsToCredService(String uin, String status, List<VidInfoDTO> vids, boolean isUpdate, List<String> partnerIds, IntFunction<String> saltRetreivalFunction, BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {
		List<CredentialIssueRequestDto> eventRequestsList = vids.stream()
					.flatMap(vid -> {
						LocalDateTime expiryTimestamp = status.equals(env.getProperty(VID_ACTIVE_STATUS)) ? vid.getExpiryTimestamp() : DateUtils.getUTCCurrentDateTime();
						return partnerIds.stream().map(partnerId -> {
							String token = tokenIDGenerator.generateTokenID(uin, partnerId);
							return createCredReqDto(vid.getVid(), partnerId,
									expiryTimestamp, vid.getTransactionLimit(), token, IdType.VID.getIdType(), securityManager.getIdHashAndAttributes(vid.getVid(), saltRetreivalFunction));
						});
					})
					.collect(Collectors.toList());
		
		sendRequestToCredService(eventRequestsList, isUpdate, credentialRequestResponseConsumer);

	}

	/**
	 * Send request to cred service.
	 *
	 * @param eventRequestsList the event requests list
	 * @param isUpdate the is update
	 * @param credentialResponseConsumer the credential response consumer
	 */
	public void sendRequestToCredService(List<CredentialIssueRequestDto> eventRequestsList, boolean isUpdate, BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {
		eventRequestsList.forEach(reqDto -> {
			CredentialIssueRequestWrapperDto requestWrapper = new CredentialIssueRequestWrapperDto();
			requestWrapper.setRequest(reqDto);
			requestWrapper.setRequesttime(DateUtils.getUTCCurrentDateTime());
			String eventTypeDisplayName = isUpdate ? "Update ID" : "Create ID";
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "notify",
					"notifying Credential Service for event " + eventTypeDisplayName);
			sendRequestToCredService(requestWrapper, credentialRequestResponseConsumer);
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "notify",
					"notified Credential Service for event" + eventTypeDisplayName);
		});
	}

	/**
	 * Send request to cred service.
	 *
	 * @param requestWrapper the request wrapper
	 * @param credentialRequestResponseConsumer the credential response consumer
	 */
	private void sendRequestToCredService(CredentialIssueRequestWrapperDto requestWrapper, BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {
		try {
			Map<String, Object> response = restHelper.requestSync(restBuilder
					.buildRequest(RestServicesConstants.CREDENTIAL_REQUEST_SERVICE, requestWrapper, Map.class));
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "sendRequestToCredService",
					"Response of Credential Request: " + mapper.writeValueAsString(response));
			if(credentialRequestResponseConsumer != null) {
				credentialRequestResponseConsumer.accept(requestWrapper, response);
			}
		} catch (RestServiceException | IdRepoDataValidationException | JsonProcessingException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "sendRequestToCredService",
					e.getMessage());
		}
	}

	/**
	 * Creates the cred req dto.
	 *
	 * @param id the id
	 * @param partnerId the partner id
	 * @param expiryTimestamp the expiry timestamp
	 * @param transactionLimit the transaction limit
	 * @param token the token
	 * @param idType the id type
	 * @param idHashAttributes the id hash attributes
	 * @return the credential issue request dto
	 */
	private CredentialIssueRequestDto createCredReqDto(String id, String partnerId, LocalDateTime expiryTimestamp,
			Integer transactionLimit, String token, String idType,
			Map<? extends String, ? extends Object> idHashAttributes) {
		Map<String, Object> data = new HashMap<>();
		data.putAll(idHashAttributes);
		data.put(EXPIRY_TIMESTAMP, Optional.ofNullable(expiryTimestamp).map(DateUtils::formatToISOString).orElse(null));
		data.put(TRANSACTION_LIMIT, transactionLimit);
		data.put(TOKEN, token);

		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setId(id);
		credentialIssueRequestDto.setCredentialType(credentialType);
		credentialIssueRequestDto.setIssuer(partnerId);
		credentialIssueRequestDto.setRecepiant(credentialRecepiant);
		credentialIssueRequestDto.setUser(IdRepoSecurityManager.getUser());
		credentialIssueRequestDto.setAdditionalData(data);
		return credentialIssueRequestDto;
	}

}
