package io.mosip.credentialstore.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.mosip.credentialstore.constants.CredentialConstants;
import io.mosip.credentialstore.constants.CredentialFormatter;
import io.mosip.credentialstore.constants.CredentialServiceErrorCodes;
import io.mosip.credentialstore.constants.JsonConstants;
import io.mosip.credentialstore.dto.AllowedKycDto;
import io.mosip.credentialstore.dto.CredentialTypeResponse;
import io.mosip.credentialstore.dto.DataProviderResponse;
import io.mosip.credentialstore.dto.DataShare;
import io.mosip.credentialstore.dto.PartnerCredentialTypePolicyDto;
import io.mosip.credentialstore.dto.PartnerExtractor;
import io.mosip.credentialstore.dto.PartnerExtractorResponse;
import io.mosip.credentialstore.exception.ApiNotAccessibleException;
import io.mosip.credentialstore.exception.CredentialFormatterException;
import io.mosip.credentialstore.exception.DataShareException;
import io.mosip.credentialstore.exception.IdRepoException;
import io.mosip.credentialstore.exception.PartnerException;
import io.mosip.credentialstore.exception.PolicyException;
import io.mosip.credentialstore.exception.SignatureException;
import io.mosip.credentialstore.provider.CredentialProvider;
import io.mosip.credentialstore.service.CredentialStoreService;
import io.mosip.credentialstore.util.DataShareUtil;
import io.mosip.credentialstore.util.DigitalSignatureUtil;
import io.mosip.credentialstore.util.EncryptionUtil;
import io.mosip.credentialstore.util.IdrepositaryUtil;
import io.mosip.credentialstore.util.JsonUtil;
import io.mosip.credentialstore.util.PolicyUtil;
import io.mosip.credentialstore.util.Utilities;
import io.mosip.credentialstore.util.WebSubUtil;
import io.mosip.idrepository.core.constant.AuditEvents;
import io.mosip.idrepository.core.constant.AuditModules;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.CredentialServiceResponse;
import io.mosip.idrepository.core.dto.CredentialServiceResponseDto;
import io.mosip.idrepository.core.dto.ErrorDTO;
import io.mosip.idrepository.core.dto.Event;
import io.mosip.idrepository.core.dto.EventModel;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.Type;
import io.mosip.idrepository.core.helper.AuditHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.websub.api.exception.WebSubClientException;

/**
 * The Class CredentialStoreServiceImpl.
 * 
 * @author Sowmya
 */
@Component
public class CredentialStoreServiceImpl implements CredentialStoreService {

	/** The policy util. */
	@Autowired
	private PolicyUtil policyUtil;

	/** The idrepositary util. */
	@Autowired
	private IdrepositaryUtil idrepositaryUtil;

	/** The Constant VALUE. */
	private static final String VALUE = "value";

	/** The id auth provider. */
	@Autowired(required = true)
	@Qualifier("idauth")
	CredentialProvider idAuthProvider;

	/** The default provider. */
	@Autowired(required = true)
	@Qualifier("default")
	CredentialProvider credentialDefaultProvider;

	/** The default provider. */
	@Autowired(required = true)
	@Qualifier("qrcode")
	CredentialProvider qrCodeProvider;

	/** The data share util. */
	@Autowired
	private DataShareUtil dataShareUtil;

	/** The web sub util. */
	@Autowired
	private WebSubUtil webSubUtil;

	/** The config server file storage URL. */
	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;

	@Value("${credential.service.credentialtype.file}")
	private String credentialTypefile;

	@Autowired
	Utilities utilities;

	/** The env. */
	@Autowired
	private Environment env;

	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.credential.service.datetime.pattern";

	/** The Constant CREDENTIAL_SERVICE_SERVICE_ID. */
	private static final String CREDENTIAL_SERVICE_SERVICE_ID = "mosip.credential.service.service.id";

	/** The Constant CREDENTIAL_SERVICE_SERVICE_VERSION. */
	private static final String CREDENTIAL_SERVICE_SERVICE_VERSION = "mosip.credential.service.service.version";

	private static final Logger LOGGER = IdRepoLogger.getLogger(CredentialStoreServiceImpl.class);

	private static final String CREATE_CRDENTIAL = "createCredentialIssuance";

	private static final String CREDENTIAL_STORE = "CredentialStoreServiceImpl";

	private static final String CREDENTIAL_SERVICE_TYPE_NAME = "mosip.credential.service.type.name";

	private static final String CREDENTIAL_SERVICE_TYPE_NAMESPACE = "mosip.credential.service.type.namespace";

	private static final String DATASHARE = "Data Share";

	@Autowired
	private AuditHelper auditHelper;

	@Autowired
	private DigitalSignatureUtil digitalSignatureUtil;

	@Autowired
	EncryptionUtil encryptionUtil;
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.credentialstore.service.CredentialStoreService#
	 * createCredentialIssuance(io.mosip.credentialstore.dto.
	 * CredentialServiceRequestDto)
	 */
	public CredentialServiceResponseDto createCredentialIssuance(
			CredentialServiceRequestDto credentialServiceRequestDto) {
		LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
				"started creating credential");
		List<ErrorDTO> errorList = new ArrayList<>();
		CredentialServiceResponseDto credentialIssueResponseDto = new CredentialServiceResponseDto();
		CredentialServiceResponse credentialServiceResponse = null;
		CredentialProvider credentialProvider;

		try {

			PartnerCredentialTypePolicyDto policyDetailResponseDto = policyUtil.getPolicyDetail(
					credentialServiceRequestDto.getCredentialType(),
					credentialServiceRequestDto.getIssuer());

			if (credentialServiceRequestDto.getAdditionalData() == null) {
				Map<String, Object> additionalData = new HashMap<>();
				credentialServiceRequestDto.setAdditionalData(additionalData);
			}

			Map<String, String> bioAttributeFormatterMap = getFormatters(policyDetailResponseDto,
					credentialServiceRequestDto.getIssuer());
			IdResponseDTO idResponseDto = idrepositaryUtil.getData(credentialServiceRequestDto,
					bioAttributeFormatterMap);


			credentialProvider = getProvider(credentialServiceRequestDto.getCredentialType());

			Map<AllowedKycDto, Object> shrableAttributesMap = credentialProvider.prepareSharableAttributes(
					idResponseDto, policyDetailResponseDto,
					credentialServiceRequestDto);
			DataProviderResponse dataProviderResponse = credentialProvider
					.getFormattedCredentialData(
					credentialServiceRequestDto, shrableAttributesMap);
			credentialServiceResponse = new CredentialServiceResponse();
			DataShare dataShare = null;
			String jsonData=null;
			String signature = null;
			String encodedData = null;
			jsonData = JsonUtil.objectMapperObjectToJson(dataProviderResponse.getJSON());
			encodedData = CryptoUtil.encodeBase64(jsonData.getBytes());
			if (policyDetailResponseDto.getPolicies().getDataSharePolicies().getTypeOfShare()
					.equalsIgnoreCase(DATASHARE)) {

				dataShare = dataShareUtil.getDataShare(jsonData.getBytes(), policyDetailResponseDto.getPolicyId(),
						credentialServiceRequestDto.getIssuer(),
						policyDetailResponseDto.getPolicies().getDataSharePolicies().getShareDomain());
				credentialServiceResponse.setDataShareUrl(dataShare.getUrl());

			} else {

				jsonData = encryptionUtil.encryptData(encodedData, credentialServiceRequestDto.getIssuer());

			}
			signature = digitalSignatureUtil.sign(encodedData);
			EventModel eventModel = getEventModel(dataShare, credentialServiceRequestDto,
					jsonData, signature);
			webSubUtil.publishSuccess(credentialServiceRequestDto.getIssuer(), eventModel);
			credentialServiceResponse.setSignature(signature);
			credentialServiceResponse.setStatus("ISSUED");
			credentialServiceResponse.setCredentialId(dataProviderResponse.getCredentialId());


			credentialServiceResponse.setIssuanceDate(dataProviderResponse.getIssuanceDate());
			LOGGER.debug(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					"ended creating credential");

		} catch (ApiNotAccessibleException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		} catch (IdRepoException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.IPREPO_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		} catch (CredentialFormatterException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.CREDENTIAL_FORMATTER_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.IO_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.IO_EXCEPTION.getErrorMessage());
			errorList.add(error);

			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		} catch (WebSubClientException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.WEBSUB_FAIL_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.WEBSUB_FAIL_EXCEPTION.getErrorMessage());
			errorList.add(error);

			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		} catch (DataShareException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage());
			errorList.add(error);

			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		} catch (PolicyException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.POLICY_EXCEPTION.getErrorMessage());
			errorList.add(error);

			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		} catch (SignatureException e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.SIGNATURE_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.SIGNATURE_EXCEPTION.getErrorMessage());
			errorList.add(error);

			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			auditHelper.auditError(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getId(), IdType.ID, e);
			ErrorDTO error = new ErrorDTO();
			error.setErrorCode(CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorCode());
			error.setMessage(CredentialServiceErrorCodes.UNKNOWN_EXCEPTION.getErrorMessage());
			errorList.add(error);
			LOGGER.error(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
					ExceptionUtils.getStackTrace(e));

		} finally {

			credentialIssueResponseDto.setId(CREDENTIAL_SERVICE_SERVICE_ID);
			credentialIssueResponseDto
					.setResponsetime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
			credentialIssueResponseDto.setVersion(env.getProperty(CREDENTIAL_SERVICE_SERVICE_VERSION));

			if (!errorList.isEmpty()) {
				credentialIssueResponseDto.setErrors(errorList);
			} else {
				credentialIssueResponseDto.setResponse(credentialServiceResponse);
			}
			auditHelper.audit(AuditModules.ID_REPO_CREDENTIAL_SERVICE, AuditEvents.CREATE_CREDENTIAL,
					credentialServiceRequestDto.getId(), IdType.ID, "create credential requested");
		}
		return credentialIssueResponseDto;
	}



	@SuppressWarnings("unchecked")
	private EventModel getEventModel(DataShare dataShare, CredentialServiceRequestDto credentialServiceRequestDto,
			String credentialData, String signature) throws IOException, ApiNotAccessibleException, SignatureException {
		Map<String, Object> map = credentialServiceRequestDto.getAdditionalData();

		EventModel eventModel = new EventModel();
		DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
		LocalDateTime localdatetime = LocalDateTime
				.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
		eventModel.setPublishedOn(DateUtils.toISOString(localdatetime));
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
		event.setTimestamp(DateUtils.toISOString(localdatetime));

		String eventId = utilities.generateId();
		LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL, "event id" + eventId);
		event.setId(eventId);
		event.setTransactionId(credentialServiceRequestDto.getRequestId());
		Type type = new Type();
		type.setName(env.getProperty(CREDENTIAL_SERVICE_TYPE_NAME));
		type.setNamespace(env.getProperty(CREDENTIAL_SERVICE_TYPE_NAMESPACE));
		event.setType(type);
		eventModel.setEvent(event);
		LOGGER.info(IdRepoSecurityManager.getUser(), CREDENTIAL_STORE, CREATE_CRDENTIAL,
				"event json" + JsonUtil.objectMapperObjectToJson(eventModel));
		return eventModel;
	}

	private Map<String, String> getFormatters(PartnerCredentialTypePolicyDto policyResponseDto, String partnerId)
			throws ApiNotAccessibleException, PartnerException {
		Map<String, String> formatterMap = new HashMap<>();
		List<AllowedKycDto> sharableAttributeList = policyResponseDto.getPolicies().getShareableAttributes();
		PartnerExtractorResponse partnerExtractorResponse = policyUtil
				.getPartnerExtractorFormat(policyResponseDto.getPolicyId(),
				partnerId);
		if (partnerExtractorResponse != null) {
		List<PartnerExtractor> partnerExtractorList = partnerExtractorResponse.getExtractors();

		sharableAttributeList.forEach(dto -> {
			if (dto.getGroup() != null && dto.getGroup().equalsIgnoreCase(CredentialConstants.CBEFF)
				&& dto.getFormat().equalsIgnoreCase(CredentialConstants.EXTRACTION)) {
				partnerExtractorList.forEach(partnerExtractorDto -> {
					if (partnerExtractorDto.getAttributeName().equalsIgnoreCase(dto.getAttributeName())) {
						if(partnerExtractorDto.getBiometric().contains(CredentialConstants.FACE)){
							formatterMap.put(CredentialConstants.FACE,
									partnerExtractorDto.getExtractor().getProvider());
						} else if (partnerExtractorDto.getBiometric().contains(CredentialConstants.IRIS)) {
							formatterMap.put(CredentialConstants.IRIS,
									partnerExtractorDto.getExtractor().getProvider());
						} else if (partnerExtractorDto.getBiometric().contains(CredentialConstants.FINGER)) {
							formatterMap.put(CredentialConstants.FINGER,
									partnerExtractorDto.getExtractor().getProvider());
		               }
					}


				});

				}

});
		}

		return formatterMap;
	}

	/**
	 * Gets the provider.
	 *
	 * @param credentialType the credential type
	 * @return the provider
	 */
	private CredentialProvider getProvider(String credentialType) {

		String provider = env.getProperty("credentialType.formatter." + credentialType.toUpperCase());
		if (provider == null) {
			return credentialDefaultProvider;
		} else if (provider.equalsIgnoreCase(CredentialFormatter.IdAuthProvider.name())) {
			return idAuthProvider;
		} else if (provider.equalsIgnoreCase(CredentialFormatter.QrCodeProvider.name())) {
			return qrCodeProvider;
		} else {
			return credentialDefaultProvider;
		}

	}

	private String getPolicyId(String credentialType) {
		String policyId = env.getProperty("credentialType.policyid." + credentialType.toUpperCase());
		return policyId;
	}


	@Override
	public CredentialTypeResponse getCredentialTypes() {
		List<io.mosip.credentialstore.dto.Type> credentialTypes = utilities.getTypes(configServerFileStorageURL,
				credentialTypefile);
		CredentialTypeResponse CredentialTypeResponse = new CredentialTypeResponse();
		CredentialTypeResponse.setCredentialTypes(credentialTypes);
		return CredentialTypeResponse;
	}

}
