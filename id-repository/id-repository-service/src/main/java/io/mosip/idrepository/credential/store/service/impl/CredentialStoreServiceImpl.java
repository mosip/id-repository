package io.mosip.idrepository.credential.store.service.impl;

import io.mosip.kernel.core.util.DateUtils2;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.idrepository.credential.store.constant.CredentialConstants;
import io.mosip.idrepository.credential.store.constant.CredentialFormatter;
import io.mosip.idrepository.credential.store.constant.CredentialServiceErrorCodes;
import io.mosip.idrepository.credential.store.constant.JsonConstants;
import io.mosip.idrepository.common.constant.LoggerFileConstant;
import io.mosip.idrepository.credential.store.dto.AllowedKycDto;
import io.mosip.idrepository.credential.store.dto.CredentialTypeResponse;
import io.mosip.idrepository.credential.store.dto.DataProviderResponse;
import io.mosip.idrepository.credential.store.dto.DataShare;
import io.mosip.idrepository.credential.store.dto.PartnerCredentialTypePolicyDto;
import io.mosip.idrepository.credential.store.dto.PartnerExtractor;
import io.mosip.idrepository.credential.store.dto.PartnerExtractorResponse;
import io.mosip.idrepository.credential.store.dto.PolicyAttributesDto;
import io.mosip.idrepository.credential.store.exception.ApiNotAccessibleException;
import io.mosip.idrepository.credential.store.exception.CredentialFormatterException;
import io.mosip.idrepository.credential.store.exception.DataShareException;
import io.mosip.idrepository.credential.store.exception.IdRepoException;
import io.mosip.idrepository.credential.store.exception.PartnerException;
import io.mosip.idrepository.credential.store.exception.PolicyException;
import io.mosip.idrepository.credential.store.exception.SignatureException;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.credential.store.provider.CredentialProvider;
import io.mosip.idrepository.credential.store.provider.impl.IdAuthProvider;
import io.mosip.idrepository.credential.store.provider.impl.QrCodeProvider;
import io.mosip.idrepository.credential.store.provider.impl.VerCredProvider;
import io.mosip.idrepository.credential.store.service.CredentialStoreService;
import io.mosip.idrepository.credential.store.util.DataShareUtil;
import io.mosip.idrepository.credential.store.util.DigitalSignatureUtil;
import io.mosip.idrepository.credential.store.util.EncryptionUtil;
import io.mosip.idrepository.credential.store.util.IdrepositaryUtil;
import io.mosip.idrepository.credential.store.util.JsonUtil;
import io.mosip.idrepository.credential.store.util.PolicyUtil;
import io.mosip.idrepository.credential.store.util.Utilities;
import io.mosip.idrepository.credential.store.util.WebSubUtil;
import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponse;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;
import io.mosip.idrepository.core.dto.ErrorDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.websub.model.Event;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.model.Type;
import io.mosip.kernel.websub.api.exception.WebSubClientException;

/**
 * Default implementation of {@link CredentialStoreService}.
 * <p>
 * Role in the MOSIP credential pipeline: terminal issuance step invoked by the credential-request
 * batch ({@code POST /v1/credentialservice/issue}) or in-process via
 * {@link io.mosip.idrepository.pipeline.InProcessCredentialClient}. Resolves partner policy,
 * retrieves identity data, delegates formatting to a type-specific {@link CredentialProvider},
 * encrypts or data-shares the payload, signs it, and publishes a {@code CREDENTIAL_ISSUED} WebSub event.
 * </p>
 *
 * @see CredentialProvider
 * @see IdAuthProvider
 * @see QrCodeProvider
 * @see VerCredProvider
 * @see PolicyUtil
 * @see WebSubUtil
 *
 * @author Sowmya
 */
@Component
public class CredentialStoreServiceImpl implements CredentialStoreService {

	/** Separator between biometric extractor provider name and version in formatter map keys. */
	private static final String FORMAT_VERSION_SEPARATOR = "-";

	/** Label for data-share delivery type in partner policy. */
	private static final String DATASHARE = "Data Share";

	/** Resolves partner credential-type policies from PMS. */
	@Autowired
	private PolicyUtil policyUtil;

	/** Retrieves identity (UIN/VID) data for sharable attribute extraction. */
	@Autowired
	private IdrepositaryUtil idrepositaryUtil;

	/** IDA credential formatter; bean name {@code idauth}. */
	@Autowired(required = true)
	@Qualifier("idauth")
	private CredentialProvider idAuthProvider;

	/** Default credential formatter when no type-specific mapping exists; bean name {@code default}. */
	@Autowired(required = true)
	@Qualifier("default")
	private CredentialProvider credentialDefaultProvider;

	/** QR-code (print) credential formatter; bean name {@code qrcode}. */
	@Autowired(required = true)
	@Qualifier("qrcode")
	private CredentialProvider qrCodeProvider;

	/** Verifiable Credential formatter; bean name {@code vercred}. */
	@Autowired(required = true)
	@Qualifier("vercred")
	private CredentialProvider verCredProvider;

	/** Uploads credential JSON to datashare when partner policy requires it. */
	@Autowired
	private DataShareUtil dataShareUtil;

	/** Registers WebSub topics and publishes credential-issued events. */
	@Autowired
	private WebSubUtil webSubUtil;

	/**
	 * Base URI of the Spring Cloud Config file storage.
	 * Config key: {@link IdRepoConstants#CONFIG_SERVER_FILE_STORAGE_URI}.
	 */
	@Value("${" + IdRepoConstants.CONFIG_SERVER_FILE_STORAGE_URI + "}")
	private String configServerFileStorageURL;

	/**
	 * Config-server path of the credential-type definition JSON file.
	 * Config key: {@link IdRepoConstants#CREDENTIAL_SERVICE_CREDENTIALTYPE_FILE}.
	 */
	@Value("${" + IdRepoConstants.CREDENTIAL_SERVICE_CREDENTIALTYPE_FILE + "}")
	private String credentialTypefile;

	/** Shared helpers for ID generation and config-backed file reads. */
	@Autowired
	private Utilities utilities;

	/** Environment accessor for dynamic {@code credentialType.formatter.*} property lookup. */
	@Autowired
	private EnvUtil env;

	/** JSON serializer for event and response payloads. */
	@Lazy
	@Autowired
	private ObjectMapper objectMapper;

	/** Class logger. */
	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialStoreServiceImpl.class);

	/** Records audit events for credential issuance success and failure. */
	@Autowired
	private AuditHelper auditHelper;

	/** Signs the base64-encoded credential JSON for the WebSub proof block. */
	@Autowired
	private DigitalSignatureUtil digitalSignatureUtil;

	/** Partner-key encryption when datashare policy is not used. */
	@Autowired
	private EncryptionUtil encryptionUtil;

	/**
	 * End-to-end credential issuance for {@code POST /issue}.
	 * <p>
	 * Pipeline: resolve policy → retrieve identity → select formatter → format credential →
	 * datashare or encrypt → sign → publish WebSub → return MOSIP response envelope.
	 * </p>
	 *
	 * @param credentialServiceRequestDto issuance request (credentialType, issuer, id, sharableAttributes)
	 * @return MOSIP response wrapper with {@link CredentialServiceResponse} or error list; never throws
	 */
	@Override
	public CredentialServiceResponseDto createCredentialIssuance(
			CredentialServiceRequestDto credentialServiceRequestDto) {
		LOGGER.debug(IdRepoSecurityManager.getUser(),
				LoggerFileConstant.REQUEST_ID.toString(),
				credentialServiceRequestDto.getRequestId(),
				"started creating credential");
		List<ErrorDTO> errorList = new ArrayList<>();
		CredentialServiceResponseDto credentialIssueResponseDto = new CredentialServiceResponseDto();
		CredentialServiceResponse credentialServiceResponse = null;
		CredentialProvider credentialProvider;

		try {
			boolean containsSharableAttributes = Optional.ofNullable(credentialServiceRequestDto.getSharableAttributes()).filter(list -> !list.isEmpty()).isPresent();
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					"CREDENTIAL_ISSUE START credentialType=" + credentialServiceRequestDto.getCredentialType()
							+ ", issuer=" + credentialServiceRequestDto.getIssuer()
							+ ", id=" + credentialServiceRequestDto.getId()
							+ ", sharableAttributesInRequest=" + containsSharableAttributes);
			PartnerCredentialTypePolicyDto policyDetailResponseDto = getPolicy(credentialServiceRequestDto, containsSharableAttributes);
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					"CREDENTIAL_ISSUE policy resolved policyId="
							+ (policyDetailResponseDto != null ? policyDetailResponseDto.getPolicyId() : "null"));

			if (credentialServiceRequestDto.getAdditionalData() == null) {
				Map<String, Object> additionalData = new HashMap<>();
				credentialServiceRequestDto.setAdditionalData(additionalData);
			}

			Map<String, String> bioAttributeFormatterMap = getFormatters(policyDetailResponseDto,
					credentialServiceRequestDto.getIssuer(), credentialServiceRequestDto.getRequestId());
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					"CREDENTIAL_ISSUE bio formatters resolved count=" + bioAttributeFormatterMap.size());

			long identityStart = System.currentTimeMillis();
			IdResponseDTO idResponseDto = idrepositaryUtil.getData(credentialServiceRequestDto,
					bioAttributeFormatterMap);
			long identityEnd = System.currentTimeMillis();
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					"CREDENTIAL_ISSUE identity retrieved for id=" + credentialServiceRequestDto.getId()
							+ " durationMs=" + (identityEnd - identityStart));

			credentialProvider = getProvider(credentialServiceRequestDto.getCredentialType());
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					"CREDENTIAL_ISSUE using formatter provider for credentialType="
							+ credentialServiceRequestDto.getCredentialType());

			long prepareStart = System.currentTimeMillis();
			Map<AllowedKycDto, Object> shrableAttributesMap = credentialProvider.prepareSharableAttributes(
					idResponseDto, policyDetailResponseDto,
					credentialServiceRequestDto);
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					"CREDENTIAL_ISSUE prepareSharableAttributes durationMs="
							+ (System.currentTimeMillis() - prepareStart));

			long formatStart = System.currentTimeMillis();
			DataProviderResponse dataProviderResponse = credentialProvider
					.getFormattedCredentialData(
					credentialServiceRequestDto, shrableAttributesMap);
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					"CREDENTIAL_ISSUE getFormattedCredentialData durationMs="
							+ (System.currentTimeMillis() - formatStart));

			credentialServiceResponse = new CredentialServiceResponse();
			DataShare dataShare = null;
			String jsonData=null;
			String signature = null;
			String encodedData = null;
			jsonData = JsonUtil.objectMapperObjectToJson(dataProviderResponse.getJSON());
			encodedData = CryptoUtil.encodeToURLSafeBase64(jsonData.getBytes());
			boolean useDataShare = policyDetailResponseDto.getPolicies() != null
					&& policyDetailResponseDto.getPolicies().getDataSharePolicies().getTypeOfShare()
					.equalsIgnoreCase(DATASHARE);
			long shareSignStart = System.currentTimeMillis();
			if (useDataShare) {
				// Datashare upload and JWT sign are independent — overlap remote RTTs.
				final String jsonForShare = jsonData;
				final String encodedForSign = encodedData;
				java.util.concurrent.CompletableFuture<DataShare> dataShareFuture =
						java.util.concurrent.CompletableFuture.supplyAsync(() -> {
							try {
								return dataShareUtil.getDataShare(jsonForShare.getBytes(),
										policyDetailResponseDto.getPolicyId(),
										credentialServiceRequestDto.getIssuer(),
										credentialServiceRequestDto.getRequestId());
							} catch (Exception e) {
								throw new java.util.concurrent.CompletionException(e);
							}
						});
				java.util.concurrent.CompletableFuture<String> signFuture =
						java.util.concurrent.CompletableFuture.supplyAsync(() -> {
							try {
								return digitalSignatureUtil.sign(encodedForSign,
										credentialServiceRequestDto.getRequestId());
							} catch (Exception e) {
								throw new java.util.concurrent.CompletionException(e);
							}
						});
				try {
					dataShare = dataShareFuture.join();
					signature = signFuture.join();
				} catch (java.util.concurrent.CompletionException e) {
					Throwable cause = e.getCause() != null ? e.getCause() : e;
					if (cause instanceof DataShareException dse) {
						throw dse;
					}
					if (cause instanceof ApiNotAccessibleException aae) {
						throw aae;
					}
					if (cause instanceof SignatureException se) {
						throw se;
					}
					if (cause instanceof IOException ioe) {
						throw ioe;
					}
					if (cause instanceof RuntimeException re) {
						throw re;
					}
					throw new CredentialFormatterException(cause);
				}
				credentialServiceResponse.setDataShareUrl(dataShare.getUrl());
			} else {
				jsonData = encryptionUtil.encryptData(encodedData, credentialServiceRequestDto.getIssuer(),
						credentialServiceRequestDto.getRequestId());
				signature = digitalSignatureUtil.sign(encodedData, credentialServiceRequestDto.getRequestId());
			}
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					"CREDENTIAL_ISSUE datashareOrEncrypt+sign durationMs="
							+ (System.currentTimeMillis() - shareSignStart)
							+ ", useDataShare=" + useDataShare);
			EventModel eventModel = getEventModel(dataShare, credentialServiceRequestDto,
					jsonData, signature);
			String topic = credentialServiceRequestDto.getIssuer() + "/" + IDAEventType.CREDENTIAL_ISSUED;
			webSubUtil.publishSuccess(topic, eventModel);
			credentialServiceResponse.setSignature(signature);
			credentialServiceResponse.setStatus("ISSUED");
			credentialServiceResponse.setCredentialId(dataProviderResponse.getCredentialId());
			credentialServiceResponse.setIssuanceDate(dataProviderResponse.getIssuanceDate());

			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					"CREDENTIAL_ISSUE completed status=ISSUED credentialId="
							+ dataProviderResponse.getCredentialId());
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					"ended creating credential");

		} catch (ApiNotAccessibleException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getRequestId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					ExceptionUtils.getStackTrace(e));
		} catch (IdRepoException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getRequestId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					ExceptionUtils.getStackTrace(e));
		} catch (CredentialFormatterException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getRequestId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getRequestId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.IO_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.IO_EXCEPTION.getErrorMessage());
			errorList.add(error);

			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					ExceptionUtils.getStackTrace(e));
		} catch (WebSubClientException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getRequestId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.WEBSUB_FAIL_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.WEBSUB_FAIL_EXCEPTION.getErrorMessage());
			errorList.add(error);

			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					ExceptionUtils.getStackTrace(e));
		} catch (DataShareException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getRequestId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage());
			errorList.add(error);

			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					ExceptionUtils.getStackTrace(e));
		} catch (PolicyException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getRequestId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorMessage());
			errorList.add(error);

			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					ExceptionUtils.getStackTrace(e));
		} catch (SignatureException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getRequestId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.SIGNATURE_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.SIGNATURE_EXCEPTION.getErrorMessage());
			errorList.add(error);

			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getRequestId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					credentialServiceRequestDto.getRequestId(),
					ExceptionUtils.getStackTrace(e));

		} finally {

			credentialIssueResponseDto.setId(EnvUtil.getCredServiceId());
			credentialIssueResponseDto
					.setResponsetime(DateUtils2.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()));
			credentialIssueResponseDto.setVersion(EnvUtil.getCredServiceVersion());

			if (!errorList.isEmpty()) {
				credentialIssueResponseDto.setErrors(errorList);
			} else {
				credentialIssueResponseDto.setResponse(credentialServiceResponse);
			}
			auditHelper.audit(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getRequestId(), IdType.ID, "Credentials Issued");
		}
		return credentialIssueResponseDto;
	}

	private PartnerCredentialTypePolicyDto getPolicy(CredentialServiceRequestDto credentialServiceRequestDto, boolean containsSharableAttributes)
			throws PolicyException, ApiNotAccessibleException {
		try {
			return policyUtil.getPolicyDetail(
					credentialServiceRequestDto.getCredentialType(),
					credentialServiceRequestDto.getIssuer(),
					credentialServiceRequestDto.getRequestId());
		} catch (PolicyException e) {
			if (containsSharableAttributes) {
				// Auth partner may not have a data-share policy. Return a dummy policy. So it
				// will use the sharable attributes in the request
				LOGGER.debug(
						"Auth partner may not have a data-share policy. Returning a dummy policy. "
						+ "Will use sharable attributes in the request");
				return createDummyPolicyResponse();
			} else {
				throw e;
			}
		}
	}

	private PartnerCredentialTypePolicyDto createDummyPolicyResponse() {
        return new PartnerCredentialTypePolicyDto();
	}

	@SuppressWarnings("unchecked")
	private EventModel getEventModel(DataShare dataShare, CredentialServiceRequestDto credentialServiceRequestDto,
			String credentialData, String signature) throws IOException, ApiNotAccessibleException, SignatureException {
		Map<String, Object> map = credentialServiceRequestDto.getAdditionalData();

		EventModel eventModel = new EventModel();
		DateTimeFormatter format = DateTimeFormatter.ofPattern(EnvUtil.getDateTimePattern());
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils2.getUTCCurrentDateTimeString(EnvUtil.getDateTimePattern()), format);
		eventModel.setPublishedOn(DateUtils2.toISOString(localdatetime));
		eventModel.setPublisher("CREDENTIAL_SERVICE");
		eventModel.setTopic(credentialServiceRequestDto.getIssuer() + "/" + IDAEventType.CREDENTIAL_ISSUED);
		Event event = new Event();

		if (dataShare == null) {

			map.put("credential", credentialData);

		} else {
			event.setDataShareUri(dataShare.getUrl());
		}

		JSONObject signaturejson = new JSONObject();
		signaturejson.put(JsonConstants.SIGNATURE, signature);
		map.put(JsonConstants.PROOF, signaturejson);
		map.put(JsonConstants.CREDENTIALTYPE, credentialServiceRequestDto.getCredentialType());
		map.put(JsonConstants.PROTECTIONKEY, credentialServiceRequestDto.getEncryptionKey());
		credentialServiceRequestDto.setAdditionalData(map);
		event.setData(credentialServiceRequestDto.getAdditionalData());
		event.setTimestamp(DateUtils2.toISOString(localdatetime));

		String eventId = utilities.generateId();
		LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
				credentialServiceRequestDto.getRequestId(), "event id" + eventId);
		event.setId(eventId);
		event.setTransactionId(credentialServiceRequestDto.getRequestId());
		Type type = new Type();
		type.setName(EnvUtil.getCredServiceTypeName());
		type.setNamespace(EnvUtil.getCredServiceTypeNamespace());
		event.setType(type);
		eventModel.setEvent(event);
		LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
				credentialServiceRequestDto.getRequestId(),	"Building Event JSON Completed.");
		return eventModel;
	}

	private Map<String, String> getFormatters(PartnerCredentialTypePolicyDto policyResponseDto, String partnerId,
			String requestId)
			throws ApiNotAccessibleException, PartnerException {
		Map<String, String> formatterMap = new HashMap<>();
		String policyId = policyResponseDto != null ? policyResponseDto.getPolicyId() : null;
		LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"CREDENTIAL_ISSUE getFormatters START partnerId=" + partnerId + ", policyId=" + policyId);
		PolicyAttributesDto policies = policyResponseDto != null ? policyResponseDto.getPolicies() : null;
		if (policies == null) {
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"CREDENTIAL_ISSUE getFormatters skip — no policy attributes");
			return formatterMap;
		}
		List<AllowedKycDto> sharableAttributeList = policies.getShareableAttributes();
		if (sharableAttributeList == null) {
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"CREDENTIAL_ISSUE getFormatters skip — no shareable attributes");
			return formatterMap;
		}
		LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"CREDENTIAL_ISSUE getFormatters shareableAttributes count=" + sharableAttributeList.size());
		PartnerExtractorResponse partnerExtractorResponse = policyUtil.getPartnerExtractorFormat(policyId, partnerId,
				requestId);
		if (partnerExtractorResponse == null) {
			LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"CREDENTIAL_ISSUE getFormatters no partner extractors configured");
			return formatterMap;
		}
		List<PartnerExtractor> partnerExtractorList = partnerExtractorResponse.getExtractors();
		LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"CREDENTIAL_ISSUE getFormatters partner extractors count="
						+ (partnerExtractorList != null ? partnerExtractorList.size() : 0));
		if (partnerExtractorList != null) {
			sharableAttributeList.forEach(dto -> {
				if (dto.getGroup() != null && dto.getGroup().equalsIgnoreCase(CredentialConstants.CBEFF)
						&& dto.getFormat().equalsIgnoreCase(CredentialConstants.EXTRACTION)) {
					partnerExtractorList.forEach(partnerExtractorDto -> {
						if (partnerExtractorDto.getAttributeName().equalsIgnoreCase(dto.getAttributeName())) {
							if (partnerExtractorDto.getBiometric().contains(CredentialConstants.FACE)) {
								formatterMap.put(CredentialConstants.FACE, getFormat(partnerExtractorDto));
							} else if (partnerExtractorDto.getBiometric().contains(CredentialConstants.IRIS)) {
								formatterMap.put(CredentialConstants.IRIS, getFormat(partnerExtractorDto));
							} else if (partnerExtractorDto.getBiometric().contains(CredentialConstants.FINGER)) {
								formatterMap.put(CredentialConstants.FINGER, getFormat(partnerExtractorDto));
							}
						}
					});
				}
			});
		}
		LOGGER.info(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
				"CREDENTIAL_ISSUE getFormatters done keys=" + formatterMap.keySet() + ", count=" + formatterMap.size());
		return formatterMap;
	}

	private String getFormat(PartnerExtractor partnerExtractorDto) {
		return partnerExtractorDto.getExtractor().getProvider() + FORMAT_VERSION_SEPARATOR + partnerExtractorDto.getExtractor().getVersion();
	}

	private CredentialProvider getProvider(String credentialType) {

		String provider = env.getProperty("credentialType.formatter." + credentialType.toUpperCase());
		if (provider == null) {
			return credentialDefaultProvider;
		} else if (provider.equalsIgnoreCase(CredentialFormatter.IdAuthProvider.name())) {
			return idAuthProvider;
		} else if (provider.equalsIgnoreCase(CredentialFormatter.QrCodeProvider.name())) {
			return qrCodeProvider;
		} else if (provider.equalsIgnoreCase(CredentialFormatter.VerCredProvider.name())) {
			return verCredProvider;
		} else {
			return credentialDefaultProvider;
		}

	}

	/**
	 * Returns credential types defined in the config-server credential-type JSON file.
	 *
	 * @return list of supported credential types loaded from config server
	 */
	@Override
	public CredentialTypeResponse getCredentialTypes() {
		List<io.mosip.idrepository.credential.store.dto.Type> credentialTypes = utilities.getTypes(configServerFileStorageURL,
				credentialTypefile);
		CredentialTypeResponse CredentialTypeResponse = new CredentialTypeResponse();
		CredentialTypeResponse.setCredentialTypes(credentialTypes);
		return CredentialTypeResponse;
	}

}
