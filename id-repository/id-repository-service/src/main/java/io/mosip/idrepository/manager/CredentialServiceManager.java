package io.mosip.idrepository.manager;

import io.mosip.kernel.core.util.DateUtils2;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;

import io.mosip.idrepository.core.constant.*;
import io.mosip.idrepository.core.dto.*;
import io.mosip.idrepository.core.entity.Handle;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.repository.HandleRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.kernel.core.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.manager.partner.PartnerServiceManager;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.DummyPartnerCheckUtil;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.core.util.SupplierWithException;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.idrepository.pipeline.CredentialPipelineContext;
import io.mosip.idrepository.pipeline.InProcessCredentialRequestClient;
import io.mosip.idrepository.pipeline.InProcessVidClient;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.model.EventModel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static io.mosip.idrepository.core.constant.IdRepoConstants.*;

/**
 * Orchestrates credential issuance and IDA WebSub event notifications.
 * <p>
 * Central coordinator for the credential pipeline: resolves OLV partner IDs via
 * {@link PartnerServiceManager}, builds {@link CredentialIssueRequestDto} payloads,
 * sends requests to credential-request service (v1 or v2), publishes IDA WebSub
 * events for deactivation/blocking, and publishes credential status update events.
 * </p>
 *
 * @see CredentialStatusManager
 * @see PartnerServiceManager
 * @see IdRepoWebSubHelper
 * @see RestServicesConstants#CREDENTIAL_REQUEST_SERVICE
 * @see RestServicesConstants#CREDENTIAL_REQUEST_SERVICE_V2
 *
 * @author Loganathan Sekar
 * @author Nagarjuna
 */
public class CredentialServiceManager {

	private static final String SEND_REQUEST_TO_CRED_SERVICE = "sendRequestToCredService";

	private static final String GET_PARTNER_IDS = "getPartnerIds";

	private static final String NOTIFY = "notify";

	private static final boolean DEFAULT_SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS = false;

	/** The Constant mosipLogger. */
	private static final Logger mosipLogger = IdRepoLogger.getLogger(CredentialServiceManager.class);

	/** The Constant ACTIVATED. */
	private static final String ACTIVATED = "ACTIVATED";

	/** The Constant BLOCKED. */
	private static final String BLOCKED = "BLOCKED";

	/** The Constant REVOKED. */
	private static final String REVOKED = "REVOKED";

	/** The rest helper. */
	private RestHelper restHelper;

	/** The rest builder. */
	@Autowired
	private RestRequestBuilder restBuilder;

	/** The security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	/** The credential type. */
	@Value("${" + IDA_CREDENTIAL_TYPE + ":" + IDA_CREDENTIAL_TYPE_DEFAULT + "}")
	private String credentialType;

	/** The credential recepiant. */
	@Value("${" + IDA_CREDENTIAL_RECIPIENT + ":" + IDA_CREDENTIAL_RECIPIENT_DEFAULT + "}")
	private String credentialRecepiant;
	
	@Value("${" + VID_ACTIVE_STATUS + "}")
	private String vidActiveStatus;

	@Value("${" + VID_DISABLE_SUPPORT + ":false}")
	private boolean vidSupportDisabled;

	@Value("${" + UIN_REFID + "}")
	private String uinRefId;

	@Value("${" + DISABLE_UIN_BASED_CREDENTIAL_REQUEST + ":false}")
	private boolean disableUINBasedCredentialRequest;

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

	@Autowired
	@Lazy
	private InProcessCredentialRequestClient inProcessCredentialRequestClient;

	@Autowired
	private InProcessVidClient inProcessVidClient;

	/**
	 * Security-context executor for parallel partner credential issuance. Optional for unit tests.
	 */
	@Autowired(required = false)
	@Qualifier("withSecurityContext")
	private Executor securityContextExecutor;

	
	@Value("${" + SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS + ":"
			+ DEFAULT_SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS + "}")
	private boolean skipExistingCredentialsForPartners;
	
	/**
	 * Creates a credential service manager with an explicit {@link RestHelper}.
	 *
	 * @param restHelper REST client for outbound credreq and VID service calls
	 */
	public CredentialServiceManager(RestHelper restHelper) {
		this.restHelper = restHelper;
	}

	/**
	 * Resolves the {@link RestHelper} bean from the application context when not constructor-injected.
	 */
	@PostConstruct
	public void init() {
		if (Objects.isNull(restHelper))
			this.restHelper = ctx.getBean(RestHelper.class);
	}

	/**
	 * Asynchronously triggers UIN credential notifications for all OLV partners.
	 * <p>
	 * Convenience wrapper around {@link #notifyUinCredential} with default partner list
	 * from {@link PartnerServiceManager#getOLVPartnerIds()}.
	 * </p>
	 *
	 * @param uin                   plain UIN
	 * @param expiryTimestamp       UIN expiry time, or {@code null} if active
	 * @param status                identity status (e.g. ACTIVATED, BLOCKED)
	 * @param isUpdate              {@code true} for identity update events
	 * @param txnId                 correlation transaction ID
	 * @param saltRetreivalFunction hash salt lookup function
	 * @param requestId             optional credential request ID for v2 API
	 */
	@Async
	public void triggerEventNotifications(String uin, LocalDateTime expiryTimestamp, String status, boolean isUpdate,
			String txnId, IntFunction<String> saltRetreivalFunction, String requestId) {
		this.notifyUinCredential(uin, expiryTimestamp, status, isUpdate, txnId, saltRetreivalFunction, null, null,
				partnerServiceManager.getOLVPartnerIds(),requestId);
	}

	/**
	 * Notifies credential-request service or IDA for a UIN lifecycle event.
	 * <p>
	 * Routes to IDA WebSub ({@link #sendUINEventToIDA}) for deactivation/blocking,
	 * or to credreq service ({@link #sendUinEventsToCredService}) for create/active updates.
	 * Retrieves associated VIDs on update when VID support is enabled.
	 * </p>
	 *
	 * @param uin                               plain UIN
	 * @param expiryTimestamp                   UIN expiry time
	 * @param status                            identity status string
	 * @param isUpdate                          {@code true} for update events
	 * @param txnId                             correlation transaction ID
	 * @param saltRetreivalFunction             hash salt lookup function
	 * @param credentialRequestResponseConsumer callback after credreq response (may be {@code null})
	 * @param idaEventModelConsumer             callback after IDA event publish (may be {@code null})
	 * @param partnerIds                        target partner IDs; falls back to all OLV partners if empty/dummy
	 * @param requestId                         optional request ID for credreq v2 API
	 */
	public void notifyUinCredential(String uin, LocalDateTime expiryTimestamp, String status, boolean isUpdate,
			String txnId, IntFunction<String> saltRetreivalFunction,
			BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer,
			Consumer<EventModel> idaEventModelConsumer, List<String> partnerIds, String requestId) {
		try {
			List<VidInfoDTO> vidInfoDtos = null;
			if (isUpdate && !vidSupportDisabled) {
				try {
					vidInfoDtos = inProcessVidClient.retrieveVidsByUin(uin).getResponse();
				} catch (Exception e) {
					mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), NOTIFY,
							"Error retrieving vids for uin " + e.getMessage(), e);
				}
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
					ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Asynchronously notifies credential-request service or IDA for VID lifecycle events.
	 *
	 * @param uin                               parent UIN for token generation
	 * @param status                            VID status (active, revoked, etc.)
	 * @param vids                              list of affected VID info DTOs
	 * @param isUpdated                         {@code true} for VID update (routes to IDA)
	 * @param saltRetreivalFunction             hash salt lookup function
	 * @param credentialRequestResponseConsumer callback after credreq response
	 * @param idaEventModelConsumer             callback after IDA event publish
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
					"notifying IDA for event " + eventType.toString());
			websubHelper.sendEventToIDA(eventDto, idaEventModelConsumer);
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), NOTIFY,
					"notified IDA for event " + eventType.toString());
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
				.flatMap(vid -> createIdaEventModel(eventType, eventType.equals(IDAEventType.ACTIVATE_ID) ? vid.getExpiryTimestamp() : DateUtils2.getUTCCurrentDateTime(),
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
	 * Builds and sends UIN/VID/handle credential issue requests to credreq service.
	 * <p>
	 * Delegates to the overload with {@code requestId = null}.
	 * </p>
	 *
	 * @param uin                               plain UIN
	 * @param expiryTimestamp                   UIN expiry time
	 * @param isUpdate                          {@code true} for update events
	 * @param vidInfoDtos                       associated VIDs (may be {@code null})
	 * @param handleList                        associated handles (may be {@code null})
	 * @param partnerIds                        target credential partner IDs
	 * @param saltRetreivalFunction             hash salt lookup function
	 * @param credentialRequestResponseConsumer response callback
	 */
	public void  sendUinEventsToCredService(String uin, LocalDateTime expiryTimestamp, boolean isUpdate,
				List<VidInfoDTO> vidInfoDtos, List<HandleInfoDTO> handleList, List<String> partnerIds, IntFunction<String> saltRetreivalFunction,
				BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {
		
		sendUinEventsToCredService( uin,  expiryTimestamp,  isUpdate,
				 vidInfoDtos, handleList,  partnerIds,  saltRetreivalFunction,
				 credentialRequestResponseConsumer,null);
		
	}
	
	/**
	 * Builds and sends UIN/VID/handle credential issue requests to credreq service.
	 * <p>
	 * Generates per-partner {@link CredentialIssueRequestDto} entries for UIN (unless disabled),
	 * associated VIDs, and handles. Handle request IDs are derived by hashing
	 * {@code requestId + handleValue}.
	 * </p>
	 *
	 * @param uin                               plain UIN
	 * @param expiryTimestamp                   UIN expiry time
	 * @param isUpdate                          {@code true} for update events
	 * @param vidInfoDtos                       associated VIDs (may be {@code null})
	 * @param handleList                        associated handles (may be {@code null})
	 * @param partnerIds                        target credential partner IDs
	 * @param saltRetreivalFunction             hash salt lookup function
	 * @param credentialRequestResponseConsumer response callback
	 * @param requestId                         optional request ID for credreq v2 API
	 */
	public void sendUinEventsToCredService(String uin, LocalDateTime expiryTimestamp, boolean isUpdate,
			List<VidInfoDTO> vidInfoDtos, List<HandleInfoDTO> handleList, List<String> partnerIds, IntFunction<String> saltRetreivalFunction,
			BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer,String requestId) {
		List<CredentialIssueRequestDto> eventRequestsList = new ArrayList<>();

		if(!disableUINBasedCredentialRequest) {
			eventRequestsList.addAll(partnerIds.stream().map(partnerId -> {
				String token = tokenIDGenerator.generateTokenID(uin, partnerId);
				return createCredReqDto(uin, partnerId, expiryTimestamp, null, token,
						securityManager.getIdHashAndAttributesWithSaltModuloByPlainIdHash(uin, saltRetreivalFunction), requestId);
			}).collect(Collectors.toList()));
		}

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
	 * Builds and sends VID credential issue requests to credreq service for all OLV partners.
	 *
	 * @param uin                               parent UIN for token generation
	 * @param status                            VID status determining expiry timestamp
	 * @param vids                              list of VID info DTOs to issue credentials for
	 * @param isUpdate                          {@code true} for update events
	 * @param partnerIds                        target credential partner IDs
	 * @param saltRetreivalFunction             hash salt lookup function
	 * @param credentialRequestResponseConsumer response callback
	 */
	public void sendVidEventsToCredService(String uin, String status, List<VidInfoDTO> vids, boolean isUpdate,
			List<String> partnerIds, IntFunction<String> saltRetreivalFunction,
			BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {
		List<CredentialIssueRequestDto> eventRequestsList = vids.stream().flatMap(vid -> {
			LocalDateTime expiryTimestamp = status.equals(EnvUtil.getVidActiveStatus()) ? vid.getExpiryTimestamp()
					: DateUtils2.getUTCCurrentDateTime();
			return partnerIds.stream().map(partnerId -> {
				String token = tokenIDGenerator.generateTokenID(uin, partnerId);
				return createCredReqDto(vid.getVid(), partnerId, expiryTimestamp, vid.getTransactionLimit(), token,
						securityManager.getIdHashAndAttributesWithSaltModuloByPlainIdHash(vid.getVid(), saltRetreivalFunction));
			});
		}).collect(Collectors.toList());

		sendRequestToCredService(eventRequestsList, isUpdate, credentialRequestResponseConsumer);

	}

	/**
	 * Sends a batch of credential issue requests to credreq service.
	 * <p>
	 * When more than one request is present and {@code withSecurityContext} is available,
	 * partners are issued in parallel (shared {@link CredentialPipelineContext} identity cache),
	 * then status callbacks run serially on the caller thread. HTTP still waits until all finish.
	 * </p>
	 *
	 * @param eventRequestsList                 list of credential issue request DTOs
	 * @param isUpdate                          {@code true} labels events as "Update ID"
	 * @param credentialRequestResponseConsumer callback invoked after each credreq response
	 */
	public void sendRequestToCredService(List<CredentialIssueRequestDto> eventRequestsList, boolean isUpdate,
			BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {
		if (eventRequestsList == null || eventRequestsList.isEmpty()) {
			return;
		}
		String eventTypeDisplayName = isUpdate ? "Update ID" : "Create ID";
		if (eventRequestsList.size() == 1 || securityContextExecutor == null) {
			eventRequestsList.forEach(reqDto -> notifySinglePartner(reqDto, eventTypeDisplayName,
					credentialRequestResponseConsumer));
			return;
		}
		CredentialPipelineContext.State pipelineState = CredentialPipelineContext.get();
		List<CompletableFuture<PartnerIssueResult>> futures = new ArrayList<>(eventRequestsList.size());
		for (CredentialIssueRequestDto reqDto : eventRequestsList) {
			futures.add(CompletableFuture.supplyAsync(
					() -> issuePartnerOnWorker(reqDto, eventTypeDisplayName, pipelineState),
					securityContextExecutor));
		}
		List<PartnerIssueResult> results = new ArrayList<>(futures.size());
		for (CompletableFuture<PartnerIssueResult> future : futures) {
			try {
				results.add(future.join());
			} catch (CompletionException e) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(),
						SEND_REQUEST_TO_CRED_SERVICE, ExceptionUtils.getStackTrace(e));
			}
		}
		// Status-row updates stay serial on the calling thread (transaction-safe).
		for (PartnerIssueResult result : results) {
			if (credentialRequestResponseConsumer != null && result != null) {
				credentialRequestResponseConsumer.accept(result.requestWrapper(), result.response());
			}
		}
	}

	private PartnerIssueResult issuePartnerOnWorker(CredentialIssueRequestDto reqDto, String eventTypeDisplayName,
			CredentialPipelineContext.State pipelineState) {
		CredentialPipelineContext.attach(pipelineState);
		try {
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), NOTIFY,
					"notifying Credential Service for event " + eventTypeDisplayName);
			CredentialIssueRequestWrapperDto requestWrapper = new CredentialIssueRequestWrapperDto();
			requestWrapper.setRequest(reqDto);
			requestWrapper.setRequesttime(DateUtils2.getUTCCurrentDateTime());
			Map<String, Object> response = queueCredentialRequest(requestWrapper);
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), NOTIFY,
					"notified Credential Service for event " + eventTypeDisplayName);
			return new PartnerIssueResult(requestWrapper, response);
		} finally {
			CredentialPipelineContext.clear();
		}
	}

	private void notifySinglePartner(CredentialIssueRequestDto reqDto, String eventTypeDisplayName,
			BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {
		mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), NOTIFY,
				"notifying Credential Service for event " + eventTypeDisplayName);
		CredentialIssueRequestWrapperDto requestWrapper = new CredentialIssueRequestWrapperDto();
		requestWrapper.setRequest(reqDto);
		requestWrapper.setRequesttime(DateUtils2.getUTCCurrentDateTime());
		Map<String, Object> response = queueCredentialRequest(requestWrapper);
		mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(), NOTIFY,
				"notified Credential Service for event " + eventTypeDisplayName);
		if (credentialRequestResponseConsumer != null) {
			credentialRequestResponseConsumer.accept(requestWrapper, response);
		}
	}

	private Map<String, Object> queueCredentialRequest(CredentialIssueRequestWrapperDto requestWrapper) {
		Map<String, Object> response = Map.of();
		try {
			response = inProcessCredentialRequestClient.queueRequest(requestWrapper.getRequest());
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getCanonicalName(),
					SEND_REQUEST_TO_CRED_SERVICE, ExceptionUtils.getStackTrace(e));
		}
		return response;
	}

	private record PartnerIssueResult(CredentialIssueRequestWrapperDto requestWrapper,
			Map<String, Object> response) {
	}

	/**
	 * Creates a {@link CredentialIssueRequestDto} without an explicit request ID.
	 * <p>
	 * Delegates to {@link #createCredReqDto(String, String, LocalDateTime, Integer, String, Map, String)}
	 * with {@code requestId = null}.
	 * </p>
	 *
	 * @param id               subject ID (UIN, VID, or handle)
	 * @param partnerId        credential partner / issuer identifier
	 * @param expiryTimestamp  ID expiry time
	 * @param transactionLimit optional transaction limit (VID)
	 * @param token            partner-scoped token ID
	 * @param idHashAttributes map containing {@code id_hash}, {@code salt}, {@code modulo}
	 * @return populated credential issue request DTO
	 */
	public CredentialIssueRequestDto createCredReqDto(String id, String partnerId, LocalDateTime expiryTimestamp,
			Integer transactionLimit, String token, Map<? extends String, ? extends Object> idHashAttributes
			) {
		return createCredReqDto(id,partnerId,expiryTimestamp,transactionLimit,token,idHashAttributes,null);
	}

	/**
	 * Creates a fully populated {@link CredentialIssueRequestDto} for credreq service.
	 * <p>
	 * Embeds hash attributes, expiry, transaction limit, and token in {@code additionalData}.
	 * Uses configured credential type and recipient ({@code id-repo-ida-credential-type/recepiant}).
	 * </p>
	 *
	 * @param id               subject ID (UIN, VID, or handle)
	 * @param partnerId        credential partner / issuer identifier
	 * @param expiryTimestamp  ID expiry time
	 * @param transactionLimit optional transaction limit (VID)
	 * @param token            partner-scoped token ID
	 * @param idHashAttributes map containing hash/salt/modulo attributes
	 * @param requestId        optional request ID for credreq v2 API tracking
	 * @return populated credential issue request DTO
	 */
	public CredentialIssueRequestDto createCredReqDto(String id, String partnerId, LocalDateTime expiryTimestamp,
			Integer transactionLimit, String token, Map<? extends String, ? extends Object> idHashAttributes,String requestId
			) {
		Map<String, Object> data = new HashMap<>();
		data.putAll(idHashAttributes);
		data.put(IdRepoConstants.EXPIRY_TIMESTAMP,
				Optional.ofNullable(expiryTimestamp).map(DateUtils2::formatToISOString).orElse(null));
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

	/*public void sendEventsToCredService(List<? extends CredentialRequestStatus> requestEntities,
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

	}*/

	private boolean isExpired(CredentialRequestStatus entity) {
		return entity.getIdExpiryTimestamp() != null && !DateUtils2.getUTCCurrentDateTime().isAfter(entity.getIdExpiryTimestamp());
	}

	/**
	 * Publishes a credential status update WebSub event (e.g. INVALID on cancellation).
	 *
	 * @param requestId  credential request ID to update
	 * @param status     new lifecycle status (e.g. {@code INVALID})
	 * @param eventTopic WebSub topic for credential status updates
	 */
	public void updateEventProcessingStatus(String requestId, String status, String eventTopic) {
		CredentialStatusUpdateEvent credentialStatusUpdateEvent = createCredentialStatusUpdateEvent(requestId, status);
		websubHelper.publishEvent(eventTopic,createEventModel(eventTopic, credentialStatusUpdateEvent));
	}
	
	/**
	 * Wraps a payload in a MOSIP {@link io.mosip.idrepository.core.dto.EventModel} for WebSub publishing.
	 *
	 * @param <T>   event payload type
	 * @param topic WebSub destination topic
	 * @param event serializable event payload
	 * @return event model with publisher, timestamp, and topic set
	 */
	public <T> io.mosip.idrepository.core.dto.EventModel<T> createEventModel(String topic, T event) {
		io.mosip.idrepository.core.dto.EventModel<T> eventModel = new io.mosip.idrepository.core.dto.EventModel<>();
		eventModel.setEvent(event);
		eventModel.setPublisher(IdRepoConstants.ID_REPO);
		eventModel.setPublishedOn(DateUtils2.formatToISOString(DateUtils2.getUTCCurrentDateTime()));
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
		credentialStatusUpdateEvent.setTimestamp(DateUtils2.formatToISOString(LocalDateTime.now()));
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
				handleInfoDTO.getAdditionalData().put(IdRepoConstants.ENCRYPTED_ID, entity.getHandle());
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