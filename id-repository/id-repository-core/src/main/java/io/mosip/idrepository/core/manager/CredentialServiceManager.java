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

import jakarta.annotation.PostConstruct;

import io.mosip.idrepository.core.constant.*;
import io.mosip.idrepository.core.dto.*;
import io.mosip.idrepository.core.entity.Handle;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.repository.HandleRepo;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.HMACUtils2;
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
import io.mosip.kernel.core.util.DateUtils2;
import io.mosip.kernel.core.websub.model.EventModel;

import javax.validation.Valid;

import static io.mosip.idrepository.core.constant.IdRepoConstants.*;
import static io.mosip.idrepository.core.security.IdRepoSecurityManager.ID_TYPE;

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

	@Value("${mosip.idrepo.identity.disable-uin-based-credential-request:false}")
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

		System.out.println("==== notifyUinCredential START ====");

		try {
			System.out.println("uin: " + uin);
			System.out.println("expiryTimestamp: " + expiryTimestamp);
			System.out.println("status: " + status);
			System.out.println("isUpdate: " + isUpdate);
			System.out.println("txnId: " + txnId);
			System.out.println("requestId: " + requestId);
			System.out.println("Initial partnerIds: " + partnerIds);

			List<VidInfoDTO> vidInfoDtos = null;

			if (isUpdate && !vidSupportDisabled) {
				System.out.println("Fetching VIDs for UIN...");

				RestRequestDTO restRequest = restBuilder.buildRequest(
						RestServicesConstants.RETRIEVE_VIDS_BY_UIN, null, VidsInfosDTO.class);

				String finalUri = restRequest.getUri().replace("{uin}", uin);
				restRequest.setUri(finalUri);

				System.out.println("VID Fetch URI: " + finalUri);

				try {
					VidsInfosDTO response = restHelper.requestSync(restRequest);
					vidInfoDtos = response.getResponse();

					System.out.println("VIDs fetched successfully. Count: "
							+ (vidInfoDtos != null ? vidInfoDtos.size() : 0));

				} catch (RestServiceException e) {
					System.out.println("Error while fetching VIDs: " + e.getMessage());

					mosipLogger.error(IdRepoSecurityManager.getUser(),
							this.getClass().getCanonicalName(),
							"notifyUinCredential",
							"Error retrieving vids for uin " + e.getMessage(), e);
				}
			} else {
				System.out.println("Skipping VID fetch. isUpdate=" + isUpdate + ", vidSupportDisabled=" + vidSupportDisabled);
			}

			// Partner handling
			if (partnerIds.isEmpty() ||
					(partnerIds.size() == 1 && dummyCheck.isDummyOLVPartner(partnerIds.get(0)))) {

				System.out.println("PartnerIds empty or dummy. Fetching real OLV partners...");
				partnerIds = partnerServiceManager.getOLVPartnerIds();
				System.out.println("Fetched PartnerIds: " + partnerIds);
			} else {
				System.out.println("Using provided PartnerIds: " + partnerIds);
			}

			boolean conditionForIDA = (status != null && isUpdate)
					&& (!ACTIVATED.equals(status) || expiryTimestamp != null);

			System.out.println("Condition for IDA event: " + conditionForIDA);

			if (conditionForIDA) {
				System.out.println("Calling sendUINEventToIDA...");

				sendUINEventToIDA(
						uin,
						expiryTimestamp,
						status,
						vidInfoDtos,
						partnerIds,
						txnId,
						id -> securityManager.getIdHashWithSaltModuloByPlainIdHash(id, saltRetreivalFunction),
						idaEventModelConsumer
				);

				System.out.println("sendUINEventToIDA completed");

			} else {
				System.out.println("Calling sendUinEventsToCredService...");

				List<HandleInfoDTO> handles = getHandles(uin, saltRetreivalFunction);
				System.out.println("Handles generated: " + handles);

				sendUinEventsToCredService(
						uin,
						expiryTimestamp,
						isUpdate,
						vidInfoDtos,
						handles,
						partnerIds,
						saltRetreivalFunction,
						credentialRequestResponseConsumer,
						requestId
				);

				System.out.println("sendUinEventsToCredService completed");
			}

		} catch (Exception e) {
			System.out.println("Exception in notifyUinCredential");
			System.out.println("Message: " + e.getMessage());
			e.printStackTrace();

			mosipLogger.error(
					IdRepoSecurityManager.getUser(),
					this.getClass().getCanonicalName(),
					NOTIFY,
					ExceptionUtils.getStackTrace(e)
			);
		}

		System.out.println("==== notifyUinCredential END ====");
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
	                                       List<VidInfoDTO> vidInfoDtos, List<HandleInfoDTO> handleList, List<String> partnerIds,
	                                       IntFunction<String> saltRetreivalFunction,
	                                       BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer,
	                                       String requestId) {

		System.out.println("==== sendUinEventsToCredService START ====");

		System.out.println("uin: " + uin);
		System.out.println("expiryTimestamp: " + expiryTimestamp);
		System.out.println("isUpdate: " + isUpdate);
		System.out.println("requestId: " + requestId);
		System.out.println("partnerIds: " + partnerIds);
		System.out.println("vidInfoDtos count: " + (vidInfoDtos != null ? vidInfoDtos.size() : "null"));
		System.out.println("handleList count: " + (handleList != null ? handleList.size() : "null"));

		List<CredentialIssueRequestDto> eventRequestsList = new ArrayList<>();

		// UIN आधारित credential request
		if (!disableUINBasedCredentialRequest) {
			System.out.println("Processing UIN-based credential requests...");

			List<CredentialIssueRequestDto> uinRequests = partnerIds.stream().map(partnerId -> {

				System.out.println("Processing partnerId (UIN): " + partnerId);

				String token = tokenIDGenerator.generateTokenID(uin, partnerId);
				System.out.println("Generated token: " + token);

				Map<? extends String, ?> hashAttributes = securityManager
						.getIdHashAndAttributesWithSaltModuloByPlainIdHash(uin, saltRetreivalFunction);

				System.out.println("Hash attributes generated for UIN");

				return createCredReqDto(
						uin,
						partnerId,
						expiryTimestamp,
						null,
						token,
						hashAttributes,
						requestId
				);

			}).collect(Collectors.toList());

			eventRequestsList.addAll(uinRequests);
			System.out.println("UIN request count added: " + uinRequests.size());
		} else {
			System.out.println("UIN-based credential request is disabled");
		}

		// VID आधारित credential request
		if (vidInfoDtos != null) {
			System.out.println("Processing VID-based credential requests...");

			List<CredentialIssueRequestDto> vidRequests = vidInfoDtos.stream().flatMap(vidInfoDTO -> {

				System.out.println("VID: " + vidInfoDTO.getVid());
				System.out.println("VID Expiry: " + vidInfoDTO.getExpiryTimestamp());
				System.out.println("VID TransactionLimit: " + vidInfoDTO.getTransactionLimit());

				LocalDateTime vidExpiryTime = Objects.isNull(expiryTimestamp)
						? vidInfoDTO.getExpiryTimestamp()
						: expiryTimestamp;

				return partnerIds.stream().map(partnerId -> {

					System.out.println("Processing partnerId (VID): " + partnerId);

					String token = tokenIDGenerator.generateTokenID(uin, partnerId);
					System.out.println("Generated token for VID: " + token);

					return createCredReqDto(
							vidInfoDTO.getVid(),
							partnerId,
							vidExpiryTime,
							vidInfoDTO.getTransactionLimit(),
							token,
							vidInfoDTO.getHashAttributes()
					);
				});

			}).collect(Collectors.toList());

			eventRequestsList.addAll(vidRequests);
			System.out.println("VID request count added: " + vidRequests.size());
		} else {
			System.out.println("No VID data available");
		}

		// Handle आधारित credential request
		if (handleList != null && !handleList.isEmpty()) {
			System.out.println("Processing HANDLE-based credential requests...");
			System.out.println("Handle count: " + handleList.size());

			List<CredentialIssueRequestDto> handleRequests = handleList.stream().flatMap(handleInfoDTO -> {

				System.out.println("Handle: " + handleInfoDTO.getHandle());

				return partnerIds.stream().map(partnerId -> {

					System.out.println("Processing partnerId (HANDLE): " + partnerId);

					String token = tokenIDGenerator.generateTokenID(uin, partnerId);
					System.out.println("Generated token for HANDLE: " + token);

					String handleRequestId = requestId.concat(handleInfoDTO.getHandle());
					System.out.println("Generated handleRequestId: " + handleRequestId);

					String hashedHandleRequestId = securityManager.hash(
							handleRequestId.getBytes(StandardCharsets.UTF_8));

					System.out.println("Hashed handleRequestId: " + hashedHandleRequestId);

					return createCredReqDto(
							handleInfoDTO.getHandle(),
							partnerId,
							null,
							null,
							token,
							handleInfoDTO.getAdditionalData(),
							hashedHandleRequestId
					);
				});

			}).collect(Collectors.toList());

			eventRequestsList.addAll(handleRequests);
			System.out.println("Handle request count added: " + handleRequests.size());
		} else {
			System.out.println("No handle data available");
		}

		System.out.println("Total eventRequestsList size: " + eventRequestsList.size());

		System.out.println("Calling sendRequestToCredService...");
		sendRequestToCredService(eventRequestsList, isUpdate, credentialRequestResponseConsumer);
		System.out.println("sendRequestToCredService completed");

		System.out.println("==== sendUinEventsToCredService END ====");
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
	 * Send request to cred service.
	 *
	 * @param eventRequestsList          the event requests list
	 * @param isUpdate                   the is update
	 * @param credentialRequestResponseConsumer the credential response consumer
	 */
	public void sendRequestToCredService(List<CredentialIssueRequestDto> eventRequestsList, boolean isUpdate,
	                                     BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {

		System.out.println("==== sendRequestToCredService START ====");

		System.out.println("isUpdate: " + isUpdate);
		System.out.println("Total requests received: " + (eventRequestsList != null ? eventRequestsList.size() : "null"));

		eventRequestsList.forEach(reqDto -> {

			try {
				System.out.println("---- Processing Request ----");

				System.out.println("Issuer: " + reqDto.getIssuer());
				System.out.println("ID (UIN/VID/Handle): " + reqDto.getId());
//				System.out.println("Expiry: " + reqDto.getAdditionalData().
				System.out.println("additional data: " + reqDto.getAdditionalData());
//				System.out.println("Token (if available): " + reqDto.getToken());

				CredentialIssueRequestWrapperDto requestWrapper = new CredentialIssueRequestWrapperDto();
				requestWrapper.setRequest(reqDto);

				LocalDateTime requestTime = DateUtils2.getUTCCurrentDateTime();
				requestWrapper.setRequesttime(requestTime);

				System.out.println("RequestTime set: " + requestTime);

				String eventTypeDisplayName = isUpdate ? "Update ID" : "Create ID";
				System.out.println("Event Type: " + eventTypeDisplayName);

				mosipLogger.info(
						IdRepoSecurityManager.getUser(),
						this.getClass().getCanonicalName(),
						NOTIFY,
						"notifying Credential Service for event " + eventTypeDisplayName
				);

				System.out.println("Calling downstream sendRequestToCredService...");

				sendRequestToCredService(
						reqDto.getIssuer(),
						requestWrapper,
						credentialRequestResponseConsumer
				);

				System.out.println("Downstream call completed for Issuer: " + reqDto.getIssuer());

				mosipLogger.info(
						IdRepoSecurityManager.getUser(),
						this.getClass().getCanonicalName(),
						NOTIFY,
						"notified Credential Service for event " + eventTypeDisplayName
				);

				System.out.println("Request processed successfully");

			} catch (Exception ex) {
				System.out.println("Exception while processing request for issuer: " + reqDto.getIssuer());
				System.out.println("Error Message: " + ex.getMessage());
				ex.printStackTrace();
			}
		});

		System.out.println("==== sendRequestToCredService END ====");
	}

	/**
	 * Send request to cred service.
	 *
	 * @param requestWrapper                    the request wrapper
	 * @param credentialRequestResponseConsumer the credential response consumer
	 */
	private void sendRequestToCredService(String partnerId,
	                                      CredentialIssueRequestWrapperDto requestWrapper,
	                                      BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {

		System.out.println("==== sendRequestToCredService (FINAL CALL) START ====");

		Map<String, Object> response = Map.of();

		try {
			System.out.println("PartnerId: " + partnerId);

			if (requestWrapper != null && requestWrapper.getRequest() != null) {
				System.out.println("RequestId: " + requestWrapper.getRequest().getRequestId());
				System.out.println("UIN/VID/Handle: " + requestWrapper.getRequest().getId());
				System.out.println("Issuer: " + requestWrapper.getRequest().getIssuer());
//				System.out.println("ExpiryTimestamp: " + requestWrapper.getRequest().getExpiryTimestamp());
//				System.out.println("TransactionLimit: " + requestWrapper.getRequest().getTransactionLimit());
//				System.out.println("Token: " + requestWrapper.getRequest().getToken());
			} else {
				System.out.println("WARNING: requestWrapper or request is NULL");
			}

			boolean hasRequestId = requestWrapper.getRequest().getRequestId() != null
					&& !requestWrapper.getRequest().getRequestId().isEmpty();

			System.out.println("Has RequestId: " + hasRequestId);

			RestServicesConstants restServicesConstants = hasRequestId
					? RestServicesConstants.CREDENTIAL_REQUEST_SERVICE_V2
					: RestServicesConstants.CREDENTIAL_REQUEST_SERVICE;

			System.out.println("Selected API: " + restServicesConstants);

			Map<String, String> pathParam = hasRequestId
					? Map.of(RID, requestWrapper.getRequest().getRequestId())
					: Map.of();

			System.out.println("Path Params: " + pathParam);

			System.out.println("Building REST request...");

			@Valid RestRequestDTO restRequest = restBuilder.buildRequest(
					restServicesConstants,
					pathParam,
					requestWrapper,
					Map.class
			);

			System.out.println("REST request built successfully");
			System.out.println("Request URI: " + restRequest.getUri());
			System.out.println("Request Method: " + restRequest.getRequestBody());
			System.out.println("whole request: " + restRequest);

			System.out.println("Calling Credential Service API...");

			response = restHelper.requestSync(restRequest);

			System.out.println("Response received from Credential Service");
			System.out.println("Response: " + response);

			mosipLogger.debug("Errors in response of Credential Request: {}" + response);

		} catch (RestServiceException e) {
			System.out.println("RestServiceException occurred!");
			System.out.println("Error Response: " +
					e.getResponseBodyAsString().orElse("No response body"));

			e.printStackTrace();

			mosipLogger.error(
					IdRepoSecurityManager.getUser(),
					this.getClass().getCanonicalName(),
					SEND_REQUEST_TO_CRED_SERVICE,
					e.getResponseBodyAsString().orElseGet(() -> ExceptionUtils.getStackTrace(e))
			);

		} catch (IdRepoDataValidationException e) {
			System.out.println("IdRepoDataValidationException occurred!");
			System.out.println("Message: " + e.getMessage());

			e.printStackTrace();

			mosipLogger.error(
					IdRepoSecurityManager.getUser(),
					this.getClass().getCanonicalName(),
					SEND_REQUEST_TO_CRED_SERVICE,
					ExceptionUtils.getStackTrace(e)
			);

		} catch (Exception e) {
			System.out.println("Unexpected Exception occurred!");
			System.out.println("Message: " + e.getMessage());

			e.printStackTrace();
		} finally {
			System.out.println("Entering finally block...");

			if (credentialRequestResponseConsumer != null) {
				System.out.println("Calling credentialRequestResponseConsumer...");
				credentialRequestResponseConsumer.accept(requestWrapper, response);
				System.out.println("Consumer executed successfully");
			} else {
				System.out.println("credentialRequestResponseConsumer is NULL");
			}
		}

		System.out.println("==== sendRequestToCredService (FINAL CALL) END ====");
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
