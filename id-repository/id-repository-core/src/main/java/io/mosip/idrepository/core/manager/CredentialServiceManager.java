package io.mosip.idrepository.core.manager;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import io.mosip.idrepository.core.constant.*;
import io.mosip.idrepository.core.dto.*;
import io.mosip.idrepository.core.entity.Handle;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.repository.HandleRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.kernel.core.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.manager.partner.PartnerServiceManager;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.core.util.SupplierWithException;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.HMACUtils2;
import io.mosip.kernel.core.websub.model.EventModel;

import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UIN_REFID;
import static io.mosip.idrepository.core.security.IdRepoSecurityManager.*;

/**
 * The Class CredentialServiceManager.
 * 
 * @author Loganathan Sekar  
 * @author Nagarjuna
 */
public class CredentialServiceManager {

	private static final String RID = "rid";

	private static final String SEND_REQUEST_TO_CRED_SERVICE = "sendRequestToCredService";

	private static final String GET_PARTNER_IDS = "getPartnerIds";

	private static final String NOTIFY = "notify";

	private static final boolean DEFAULT_SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS = false;

	private static final String PROP_SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS = "skip-requesting-existing-credentials-for-partners";
	
	/** The Constant mosipLogger. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(CredentialServiceManager.class);

	/** The Constant IDA. */
	private static final String IDA = "IDA";

	/** The Constant AUTH. */
	private static final String AUTH = "auth";

	/** The Constant ACTIVE. */
	private static final String ACTIVATED = "ACTIVATED";

	/** The Constant BLOCKED. */
	private static final String BLOCKED = "BLOCKED";

	/** The Constant REVOKED. */
	private static final String REVOKED = "REVOKED";

	/** The mapper. */
	@Autowired
	private ObjectMapper mapper;

	/** The rest helper. */
	private RestHelper restHelper;

	/** The rest builder. */
	@Autowired
	private RestRequestBuilder restBuilder;

	/** The security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	/** The credential type. */
	@Value("${id-repo-ida-credential-type:" + AUTH + "}")
	private String credentialType;

	/** The credential recepiant. */
	@Value("${id-repo-ida-credential-recepiant:" + IDA + "}")
	private String credentialRecepiant;
	
	@Value("${mosip.idrepo.vid.active-status}")
	private String vidActiveStatus;

	@Value("${mosip.idrepo.vid.disable-support:false}")
	private boolean vidSupportDisabled;

	@Value("${" + UIN_REFID + "}")
	private String uinRefId;

	/** The token ID generator. */
	@Autowired
	private TokenIDGenerator tokenIDGenerator;

	@Autowired
	private IdRepoWebSubHelper websubHelper;

	@Autowired
	private DummyPartnerCheckUtil dummyCheck;
	
	@Autowired
	private ApplicationContext ctx;
	
	@Autowired
	private PartnerServiceManager partnerServiceManager;

	@Autowired(required = false)
	private HandleRepo handleRepo;

	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;

	@Autowired
	private UinEncryptSaltRepo uinEncryptSaltRepo;

	
	@Value("${" + PROP_SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS + ":"
			+ DEFAULT_SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS + "}")
	private boolean skipExistingCredentialsForPartners;
	
	public CredentialServiceManager(RestHelper restHelper) {
		this.restHelper = restHelper;
	}
	
	@PostConstruct
	public void init() {
		if (Objects.isNull(restHelper))
			this.restHelper = ctx.getBean(RestHelper.class);
	}

	@Async
	public void triggerEventNotifications(String uin, LocalDateTime expiryTimestamp, String status, boolean isUpdate,
			String txnId, IntFunction<String> saltRetreivalFunction, String requestId) {
		this.notifyUinCredential(uin, expiryTimestamp, status, isUpdate, txnId, saltRetreivalFunction, null, null,
				partnerServiceManager.getOLVPartnerIds(),requestId);
	}

	/**
	 * Notify uin credential.
	 *
	 * @param uin                        the uin
	 * @param expiryTimestamp            the expiry timestamp
	 * @param status                     the status
	 * @param isUpdate                   the is update
	 * @param txnId                      the txn id
	 * @param saltRetreivalFunction      the salt retreival function
	 * @param credentialRequestResponseConsumer the credential response consumer
	 * @param idaEventModelConsumer
	 * @param requestId 
	 */
	public void notifyUinCredential(String uin, LocalDateTime expiryTimestamp, String status, boolean isUpdate,
			String txnId, IntFunction<String> saltRetreivalFunction,
			BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer,
			Consumer<EventModel> idaEventModelConsumer, List<String> partnerIds, String requestId) {
		try {
			List<VidInfoDTO> vidInfoDtos = null;
			if (isUpdate && !vidSupportDisabled) {
				RestRequestDTO restRequest = restBuilder.buildRequest(RestServicesConstants.RETRIEVE_VIDS_BY_UIN, null,
						VidsInfosDTO.class);
				restRequest.setUri(restRequest.getUri().replace("{uin}", uin));
				VidsInfosDTO response = restHelper.requestSync(restRequest);
				vidInfoDtos = response.getResponse();
			}
			
			if (partnerIds.isEmpty() || (partnerIds.size() == 1 && dummyCheck.isDummyOLVPartner(partnerIds.get(0)))) {
				partnerIds = partnerServiceManager.getOLVPartnerIds();
			}

			if ((status != null && isUpdate) && (!ACTIVATED.equals(status) || expiryTimestamp != null)) {
				// Event to be sent to IDA for deactivation/blocked uin state
				sendUINEventToIDA(uin, expiryTimestamp, status, vidInfoDtos, partnerIds, txnId,
						id -> securityManager.getIdHashWithSaltModuloByPlainIdHash(id, saltRetreivalFunction), idaEventModelConsumer);
			} else {
				// For create uin, or update uin with null expiry (active status), send event to
				// credential service.
				sendUinEventsToCredService(uin, expiryTimestamp, isUpdate, vidInfoDtos, getHandles(uin, saltRetreivalFunction), partnerIds,
						saltRetreivalFunction, credentialRequestResponseConsumer,requestId);
			}

		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), NOTIFY,
					e.getMessage());
		}
	}

	/**
	 * Notify VID credential.
	 *
	 * @param uin                        the uin
	 * @param status                     the status
	 * @param vids                       the vids
	 * @param isUpdated                  the is updated
	 * @param saltRetreivalFunction      the salt retreival function
	 * @param credentialRequestResponseConsumer the credential response consumer
	 * @param idaEventModelConsumer
	 */
	@Async
	public void notifyVIDCredential(String uin, String status, List<VidInfoDTO> vids, boolean isUpdated,
			IntFunction<String> saltRetreivalFunction,
			BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer,
			Consumer<EventModel> idaEventModelConsumer) {
		try {
			List<String> partnerIds = partnerServiceManager.getOLVPartnerIds();
			if (isUpdated) {
				sendVIDEventsToIDA(status, vids, partnerIds, idaEventModelConsumer);
			} else {
				sendVidEventsToCredService(uin, status, vids, isUpdated, partnerIds, saltRetreivalFunction,
						credentialRequestResponseConsumer);
			}
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), GET_PARTNER_IDS, e.getMessage());
		}
	}

	

	/**
	 * Send UIN event to IDA.
	 *
	 * @param uin                   the uin
	 * @param expiryTimestamp       the expiry timestamp
	 * @param status                the status
	 * @param vidInfoDtos           the vid info dtos
	 * @param partnerIds            the partner ids
	 * @param txnId                 the txn id
	 * @param getIdHashFunction     the get id hash function
	 * @param idaEventModelConsumer
	 */
	private void sendUINEventToIDA(String uin, LocalDateTime expiryTimestamp, String status, List<VidInfoDTO> vidInfoDtos,
			List<String> partnerIds, String txnId, UnaryOperator<String> getIdHashFunction,
			Consumer<EventModel> idaEventModelConsumer) {
		List<EventModel> eventList = new ArrayList<>();
		EventType eventType = BLOCKED.equals(status) ? IDAEventType.REMOVE_ID : IDAEventType.DEACTIVATE_ID;
		eventList.addAll(
				createIdaEventModel(eventType, expiryTimestamp, null, partnerIds, txnId, getIdHashFunction.apply(uin))
						.collect(Collectors.toList()));

		if (vidInfoDtos != null) {
			List<EventModel> idaEvents = vidInfoDtos.stream()
					.flatMap(vidInfoDTO -> createIdaEventModel(eventType, expiryTimestamp,
							vidInfoDTO.getTransactionLimit(), partnerIds, txnId, vidInfoDTO.getHashAttributes().get(IdRepoConstants.ID_HASH)))
					.collect(Collectors.toList());
			eventList.addAll(idaEvents);
		}

		sendEventsToIDA(eventList, eventType, idaEventModelConsumer);
	}

	private void sendEventsToIDA(List<EventModel> eventList, EventType eventType, Consumer<EventModel> idaEventModelConsumer) {
		eventList.forEach(eventDto -> {
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), NOTIFY,
					"notifying IDA for event" + eventType.toString());
			websubHelper.sendEventToIDA(eventDto, idaEventModelConsumer);
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), NOTIFY,
					"notified IDA for event" + eventType.toString());
		});
	}

	/**
	 * Send VID events to IDA.
	 *
	 * @param status                the status
	 * @param vids                  the vids
	 * @param partnerIds            the partner ids
	 * @param idaEventModelConsumer
	 */
	private void sendVIDEventsToIDA(String status, List<VidInfoDTO> vids, List<String> partnerIds,
			Consumer<EventModel> idaEventModelConsumer) {
		EventType eventType;
		if (vidActiveStatus.equals(status)) {
			eventType = IDAEventType.ACTIVATE_ID;
		} else if (REVOKED.equals(status)) {
			eventType = IDAEventType.REMOVE_ID;
		} else {
			eventType = IDAEventType.DEACTIVATE_ID;
		}
		String transactionId = "";// TODO
		List<EventModel> eventDtos = vids.stream()
				.flatMap(vid -> createIdaEventModel(eventType, eventType.equals(IDAEventType.ACTIVATE_ID) ? vid.getExpiryTimestamp() : DateUtils.getUTCCurrentDateTime(),
						vid.getTransactionLimit(), partnerIds, transactionId, vid.getHashAttributes().get(IdRepoConstants.ID_HASH)))
				.collect(Collectors.toList());
		sendEventsToIDA(eventDtos, eventType, idaEventModelConsumer);

	}

	/**
	 * Creates the ida event model.
	 *
	 * @param eventType        the event type
	 * @param expiryTimestamp  the expiry timestamp
	 * @param transactionLimit the transaction limit
	 * @param partnerIds       the partner ids
	 * @param transactionId    the transaction id
	 * @param idHash           the id hash
	 * @return the stream
	 */
	private Stream<EventModel> createIdaEventModel(EventType eventType, LocalDateTime expiryTimestamp,
			Integer transactionLimit, List<String> partnerIds, String transactionId, String idHash) {
		return partnerIds.stream().map(partner -> SupplierWithException.execute(() -> websubHelper
				.createEventModel(eventType, expiryTimestamp, transactionLimit, transactionId, partner, idHash).get()));
	}

	/**
	 * Send uin events to cred service.
	 *
	 * @param uin                        the uin
	 * @param expiryTimestamp            the expiry timestamp
	 * @param isUpdate                   the is update
	 * @param vidInfoDtos                the vid info dtos
	 * @param handleList                 Handle dto list
	 * @param partnerIds                 the partner ids
	 * @param saltRetreivalFunction      the salt retreival function
	 * @param credentialRequestResponseConsumer the credential response consumer
	 */
	public void  sendUinEventsToCredService(String uin, LocalDateTime expiryTimestamp, boolean isUpdate,
				List<VidInfoDTO> vidInfoDtos, List<HandleInfoDTO> handleList, List<String> partnerIds, IntFunction<String> saltRetreivalFunction,
				BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {
		
		sendUinEventsToCredService( uin,  expiryTimestamp,  isUpdate,
				 vidInfoDtos, handleList,  partnerIds,  saltRetreivalFunction,
				 credentialRequestResponseConsumer,null);
		
	}
	
	public void sendUinEventsToCredService(String uin, LocalDateTime expiryTimestamp, boolean isUpdate,
			List<VidInfoDTO> vidInfoDtos, List<HandleInfoDTO> handleList, List<String> partnerIds, IntFunction<String> saltRetreivalFunction,
			BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer,String requestId) {
		List<CredentialIssueRequestDto> eventRequestsList = new ArrayList<>();

		eventRequestsList.addAll(partnerIds.stream().map(partnerId -> {
			String token = tokenIDGenerator.generateTokenID(uin, partnerId);
			return createCredReqDto(uin, partnerId, expiryTimestamp, null, token,
					securityManager.getIdHashAndAttributesWithSaltModuloByPlainIdHash(uin, saltRetreivalFunction), requestId);
		}).collect(Collectors.toList()));

		if (vidInfoDtos != null) {
			List<CredentialIssueRequestDto> vidRequests = vidInfoDtos.stream().flatMap(vidInfoDTO -> {
				LocalDateTime vidExpiryTime = Objects.isNull(expiryTimestamp) ? vidInfoDTO.getExpiryTimestamp() : expiryTimestamp;
				return partnerIds.stream().map(partnerId -> {
					String token = tokenIDGenerator.generateTokenID(uin, partnerId);
					return createCredReqDto(vidInfoDTO.getVid(), partnerId, vidExpiryTime, vidInfoDTO.getTransactionLimit(),
							token, vidInfoDTO.getHashAttributes());
				});
			}).collect(Collectors.toList());
			eventRequestsList.addAll(vidRequests);
		}

		if(handleList != null && !handleList.isEmpty()) {
			mosipLogger.debug(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "sendUinEventsToCredService",
					"Number of handles identified >> " + handleList.size());
			List<CredentialIssueRequestDto> handleRequests = handleList.stream().flatMap(handleInfoDTO -> {
				return partnerIds.stream().map(partnerId -> {
					String token = tokenIDGenerator.generateTokenID(uin, partnerId);
					//Given requestId and the handle value is hashed together to generate a unique requestId for handle credential.
					//Credential issuance status check systems should generate the handle requestId in the same way to get latest issuance status.
					String handleRequestId = requestId.concat(handleInfoDTO.getHandle());
					return createCredReqDto(handleInfoDTO.getHandle(), partnerId, null, null,
							token, handleInfoDTO.getAdditionalData(),
							securityManager.hash(handleRequestId.getBytes(StandardCharsets.UTF_8)));
				});
			}).collect(Collectors.toList());
			eventRequestsList.addAll(handleRequests);
		}

		sendRequestToCredService(eventRequestsList, isUpdate, credentialRequestResponseConsumer);
	}

	/**
	 * Send vid events to cred service.
	 *
	 * @param uin                        the uin
	 * @param status                     the status
	 * @param vids                       the vids
	 * @param isUpdate                   the is update
	 * @param partnerIds                 the partner ids
	 * @param saltRetreivalFunction      the salt retreival function
	 * @param credentialRequestResponseConsumer the credential response consumer
	 */
	public void sendVidEventsToCredService(String uin, String status, List<VidInfoDTO> vids, boolean isUpdate,
			List<String> partnerIds, IntFunction<String> saltRetreivalFunction,
			BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {
		List<CredentialIssueRequestDto> eventRequestsList = vids.stream().flatMap(vid -> {
			LocalDateTime expiryTimestamp = status.equals(EnvUtil.getVidActiveStatus()) ? vid.getExpiryTimestamp()
					: DateUtils.getUTCCurrentDateTime();
			return partnerIds.stream().map(partnerId -> {
				String token = tokenIDGenerator.generateTokenID(uin, partnerId);
				return createCredReqDto(vid.getVid(), partnerId, expiryTimestamp, vid.getTransactionLimit(), token,
						securityManager.getIdHashAndAttributesWithSaltModuloByPlainIdHash(vid.getVid(), saltRetreivalFunction));
			});
		}).collect(Collectors.toList());

		sendRequestToCredService(eventRequestsList, isUpdate, credentialRequestResponseConsumer);

	}

	/**
	 * Send request to cred service.
	 *
	 * @param eventRequestsList          the event requests list
	 * @param isUpdate                   the is update
	 * @param credentialRequestResponseConsumer the credential response consumer
	 */
	public void sendRequestToCredService(List<CredentialIssueRequestDto> eventRequestsList, boolean isUpdate,
			BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {
		eventRequestsList.forEach(reqDto -> {
			CredentialIssueRequestWrapperDto requestWrapper = new CredentialIssueRequestWrapperDto();
			requestWrapper.setRequest(reqDto);
			requestWrapper.setRequesttime(DateUtils.getUTCCurrentDateTime());
			String eventTypeDisplayName = isUpdate ? "Update ID" : "Create ID";
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), NOTIFY,
					"notifying Credential Service for event " + eventTypeDisplayName);
			sendRequestToCredService(reqDto.getIssuer(), requestWrapper, credentialRequestResponseConsumer);
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), NOTIFY,
					"notified Credential Service for event" + eventTypeDisplayName);
		});
	}

	/**
	 * Send request to cred service.
	 *
	 * @param requestWrapper                    the request wrapper
	 * @param credentialRequestResponseConsumer the credential response consumer
	 */
	private void sendRequestToCredService(String partnerId, CredentialIssueRequestWrapperDto requestWrapper,
			BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {
		try {

			Map<String, Object> response = Map.of();
			RestServicesConstants restServicesConstants = requestWrapper.getRequest().getRequestId() != null
					&& !requestWrapper.getRequest().getRequestId().isEmpty()
					? RestServicesConstants.CREDENTIAL_REQUEST_SERVICE_V2
					: RestServicesConstants.CREDENTIAL_REQUEST_SERVICE;
			Map<String, String> pathParam = requestWrapper.getRequest().getRequestId() != null
					&& !requestWrapper.getRequest().getRequestId().isEmpty()
					? Map.of(RID, requestWrapper.getRequest().getRequestId())
					: Map.of();
			response = restHelper
					.requestSync(restBuilder.buildRequest(restServicesConstants, pathParam, requestWrapper, Map.class));
			mosipLogger.debug(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(),
					SEND_REQUEST_TO_CRED_SERVICE,
					"Response of Credential Request: " + mapper.writeValueAsString(response));

			if (credentialRequestResponseConsumer != null) {
				credentialRequestResponseConsumer.accept(requestWrapper, response);
			}

		} catch (RestServiceException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), SEND_REQUEST_TO_CRED_SERVICE,
					e.getResponseBodyAsString().orElseGet(() -> ExceptionUtils.getStackTrace(e)));
		} catch (IdRepoDataValidationException | JsonProcessingException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), SEND_REQUEST_TO_CRED_SERVICE,
					ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Creates the cred req dto.
	 *
	 * @param id               the id
	 * @param partnerId        the partner id
	 * @param expiryTimestamp  the expiry timestamp
	 * @param transactionLimit the transaction limit
	 * @param token            the token
	 * @param idHashAttributes the id hash attributes
	 * @return the credential issue request dto
	 */
	
	public CredentialIssueRequestDto createCredReqDto(String id, String partnerId, LocalDateTime expiryTimestamp,
			Integer transactionLimit, String token, Map<? extends String, ? extends Object> idHashAttributes
			) {
		return createCredReqDto(id,partnerId,expiryTimestamp,transactionLimit,token,idHashAttributes,null);
	}

	public CredentialIssueRequestDto createCredReqDto(String id, String partnerId, LocalDateTime expiryTimestamp,
			Integer transactionLimit, String token, Map<? extends String, ? extends Object> idHashAttributes,String requestId
			) {
		Map<String, Object> data = new HashMap<>();
		data.putAll(idHashAttributes);
		data.put(IdRepoConstants.EXPIRY_TIMESTAMP,
				Optional.ofNullable(expiryTimestamp).map(DateUtils::formatToISOString).orElse(null));
		data.put(IdRepoConstants.TRANSACTION_LIMIT, transactionLimit);
		data.put(IdRepoConstants.TOKEN, token);

		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setId(id);
		credentialIssueRequestDto.setRequestId(requestId);
		credentialIssueRequestDto.setCredentialType(credentialType);
		credentialIssueRequestDto.setIssuer(partnerId);
		credentialIssueRequestDto.setRecepiant(credentialRecepiant);
		credentialIssueRequestDto.setUser(IdRepoSecurityManager.getUser());
		credentialIssueRequestDto.setAdditionalData(data);
		return credentialIssueRequestDto;
	}
	
	public void sendEventsToCredService(List<? extends CredentialRequestStatus> requestEntities,
			List<String> partnerIds,
			BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer,
			Predicate<? super CredentialIssueRequestDto> additionalFilterCondition,
			IntFunction<String> saltRetreivalFunction, String requestId) {
		if (requestEntities != null) {
			Predicate<CredentialRequestStatus> isExpiredCondition = this::isExpired;
			List<CredentialIssueRequestDto> requests = requestEntities
					.stream()
					.filter(isExpiredCondition.negate())
					.flatMap(entity -> {
				Predicate<? super String> skipExistingCredentialsForPartnersCondition = partnerId -> skipExistingCredentialsForPartners
						&& partnerId.equals(entity.getPartnerId());
				Predicate<? super CredentialIssueRequestDto> additionalPredicate = additionalFilterCondition == null ? t -> true: additionalFilterCondition;
				return partnerIds.stream()
						.filter(skipExistingCredentialsForPartnersCondition.negate())
						.map(partnerId -> createCredReqDto(entity.getIndividualId(), partnerId, entity.getIdExpiryTimestamp(),
								entity.getIdTransactionLimit(), entity.getTokenId(),
								securityManager.getIdHashAndAttributesWithSaltModuloByPlainIdHash(entity.getIndividualId(), saltRetreivalFunction),requestId))
						.filter(additionalPredicate);
			}).collect(Collectors.toList());
			
			sendRequestToCredService(requests, false, credentialRequestResponseConsumer);
			
		}
		
	}

	private boolean isExpired(CredentialRequestStatus entity) {
		return entity.getIdExpiryTimestamp() != null && !DateUtils.getUTCCurrentDateTime().isAfter(entity.getIdExpiryTimestamp());
	}

	/**
	 * Updates the event processing status
	 * @param requestId
	 * @param status
	 * @param eventTopic
	 */
	public void updateEventProcessingStatus(String requestId, String status, String eventTopic) {
		CredentialStatusUpdateEvent credentialStatusUpdateEvent = createCredentialStatusUpdateEvent(requestId, status);
		websubHelper.publishEvent(eventTopic,createEventModel(eventTopic, credentialStatusUpdateEvent));
	}
	
	/**
	 * Creates the event model.
	 *
	 * @param <T> the generic type
	 * @param topic the topic
	 * @param event the event
	 * @return the event model
	 */
	public <T> io.mosip.idrepository.core.dto.EventModel<T> createEventModel(String topic, T event) {
		io.mosip.idrepository.core.dto.EventModel<T> eventModel = new io.mosip.idrepository.core.dto.EventModel<>();
		eventModel.setEvent(event);
		eventModel.setPublisher(IdRepoConstants.ID_REPO);
		eventModel.setPublishedOn(DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		eventModel.setTopic(topic);
		return eventModel;
	}
	
	/**
	 * Creates the credential status update event.
	 *
	 * @param requestId the request id
	 * @param status the status
	 * @return the credential status update event
	 */
	private CredentialStatusUpdateEvent createCredentialStatusUpdateEvent(String requestId, String status) {
		CredentialStatusUpdateEvent credentialStatusUpdateEvent = new CredentialStatusUpdateEvent();
		credentialStatusUpdateEvent.setStatus(status);
		credentialStatusUpdateEvent.setRequestId(requestId);
		credentialStatusUpdateEvent.setTimestamp(DateUtils.formatToISOString(LocalDateTime.now()));
		return credentialStatusUpdateEvent;
	}

	private List<HandleInfoDTO> getHandles(String uin, IntFunction<String> saltRetreivalFunction) {
		if(handleRepo == null) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), "getHandles",
					"HandleRepo is NULL");
			return List.of();
		}

		int modResult = securityManager.getSaltKeyForId(uin);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(modResult);
		String uinHash = modResult + SPLITTER + securityManager.hashwithSalt(uin.getBytes(), hashSalt.getBytes());
		List<Handle> list = handleRepo.findByUinHash(uinHash);

		List<HandleInfoDTO> handleInfoDTOS = new ArrayList<>();
		for(Handle entity : list) {
			HandleInfoDTO handleInfoDTO = new HandleInfoDTO();
			String encryptSalt = uinEncryptSaltRepo
					.retrieveSaltById(Integer.valueOf(io.mosip.kernel.core.util.StringUtils.substringBefore(entity.getHandle(), SPLITTER)));
			try {
				handleInfoDTO.setHandle(new String(securityManager.decryptWithSalt(
						CryptoUtil.decodeURLSafeBase64(io.mosip.kernel.core.util.StringUtils.substringAfter(entity.getHandle(), SPLITTER)),
						CryptoUtil.decodePlainBase64(encryptSalt), uinRefId)));

				handleInfoDTO.setAdditionalData(securityManager.getIdHashAndAttributesWithSaltModuloByPlainIdHash(handleInfoDTO.getHandle(),
						saltRetreivalFunction));
				handleInfoDTO.getAdditionalData().put("idType", IdType.HANDLE.getIdType());
				handleInfoDTOS.add(handleInfoDTO);
			} catch (IdRepoAppException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), SEND_REQUEST_TO_CRED_SERVICE, "getHandles",
						"\n Failed to decrypt handle due to " + e.getMessage());
			}
		}
		return handleInfoDTOS;
	}
}
