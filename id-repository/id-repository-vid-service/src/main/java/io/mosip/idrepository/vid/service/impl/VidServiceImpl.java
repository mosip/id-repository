package io.mosip.idrepository.vid.service.impl;

import static io.mosip.idrepository.core.constant.IdRepoConstants.ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.APPLICATION_VERSION_VID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDA_NOTIFY_REQ_ID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.IDA_NOTIFY_REQ_VER;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_DEACTIVATED;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_REGENERATE_ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_REGENERATE_ALLOWED_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_UNLIMITED_TRANSACTION_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.WEB_SUB_PUBLISH_URL;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DATABASE_ACCESS_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_UIN;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_VID;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.NO_RECORD_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UIN_HASH_MISMATCH;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UIN_RETRIEVAL_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.VID_GENERATION_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.VID_POLICY_FAILED;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.EventType;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.dto.VidInfoDTO;
import io.mosip.idrepository.core.dto.VidPolicy;
import io.mosip.idrepository.core.dto.VidRequestDTO;
import io.mosip.idrepository.core.dto.VidResponseDTO;
import io.mosip.idrepository.core.dto.VidsInfosDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.VidService;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.idrepository.vid.entity.Vid;
import io.mosip.idrepository.vid.provider.VidPolicyProvider;
import io.mosip.idrepository.vid.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.vid.repository.UinHashSaltRepo;
import io.mosip.idrepository.vid.repository.VidRepo;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.UUIDUtils;
import io.mosip.kernel.core.websub.model.Event;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.model.Type;
import io.mosip.kernel.core.websub.spi.PublisherClient;

/**
 * The Class VidServiceImpl - service implementation for {@code VidService}.
 *
 * @author Manoj SP
 * @author Prem Kumar
 */
@Component
@Transactional
public class VidServiceImpl implements VidService<VidRequestDTO, ResponseWrapper<VidResponseDTO>, ResponseWrapper<List<VidInfoDTO>>> {
	
	private static final String ID_TYPE = "idType";

	private static final String TOKEN = "TOKEN";

	private static final String SALT = "SALT";

	private static final String MODULO = "MODULO";
	
	private static final String ID_HASH = "id_hash";

	private static final String EXPIRY_TIMESTAMP = "expiry_timestamp";

	private static final String TRANSACTION_LIMIT = "transaction_limit";

	private static final String IDA = "IDA";

	private static final String AUTH = "auth";

	private static final String REVOKED = "REVOKED";

	/** The Constant VID. */
	private static final String VID = "vid";

	/** The Constant REACTIVATE. */
	private static final String REACTIVATE = "reactivate";

	/** The Constant DEACTIVATE. */
	private static final String DEACTIVATE = "deactivate";

	/** The Constant REGENERATE_VID. */
	private static final String REGENERATE_VID = "regenerateVid";

	/** The Constant UPDATE_VID. */
	private static final String UPDATE_VID = "updateVid";

	/** The Constant CREATE_VID. */
	private static final String CREATE_VID = "createVid";

	/** The Constant RETRIEVE_UIN_BY_VID. */
	private static final String RETRIEVE_UIN_BY_VID = "retrieveUinByVid";

	/** The mosip logger. */
	private Logger mosipLogger = IdRepoLogger.getLogger(VidServiceImpl.class);

	/** The Constant EXPIRED. */
	private static final String EXPIRED = "EXPIRED";

	/** The Constant ID_REPO_VID_SERVICE. */
	private static final String ID_REPO_VID_SERVICE = "VidService";
	
	private static final String PARNER_ACTIVE_STATUS = "Active";
	
	@Value("${mosip.idrepo.crypto.refId.uin}")
	private String uinRefId;

	/** The env. */
	@Autowired
	private Environment env;

	/** The vid repo. */
	@Autowired
	private VidRepo vidRepo;

	/** The rest builder. */
	@Autowired
	private RestRequestBuilder restBuilder;

	/** The rest helper. */
	@Autowired
	private RestHelper restHelper;

	/** The policy provider. */
	@Autowired
	private VidPolicyProvider policyProvider;

	/** The security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;

	/** The Uin Hash Salt Repo. */
	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;

	/** The Uin Encrypt Salt Repo. */
	@Autowired
	private UinEncryptSaltRepo uinEncryptSaltRepo;

	/** The id. */
	@Resource
	private Map<String, String> id;
	
	@Value("${id-repo-ida-event-type-namespace:mosip}")
	private  String idaEventTypeNamespace;
	
	@Value("${id-repo-ida-event-type-name:ida}")
	private  String idaEventTypeName;
	
	@Value("${" + WEB_SUB_PUBLISH_URL + "}")
	private String webSubPublishUrl;
	
	@Value("${id-repo-ida-credential-type:" + AUTH + "}")
	private String credentialType;
	
	@Value("${id-repo-ida-credential-recepiant:" + IDA + "}")
	private String credentialRecepiant;

	@Autowired
	private PublisherClient<String, EventModel, HttpHeaders> pb;
	
	@Autowired
	private TokenIDGenerator tokenIDGenerator;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.idrepository.core.spi.VidService#createVid(java.lang.Object)
	 */
	@Override
	public ResponseWrapper<VidResponseDTO> generateVid(VidRequestDTO vidRequest) throws IdRepoAppException {
		String uin = vidRequest.getUin();
		try {
			Vid vid = generateVid(uin, vidRequest.getVidType());
			VidResponseDTO responseDTO = new VidResponseDTO();
			responseDTO.setVid(vid.getVid());
			responseDTO.setVidStatus(vid.getStatusCode());
			return buildResponse(responseDTO, id.get("create"));
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, CREATE_VID,
					"\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, CREATE_VID, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	/**
	 * This method will generate Vid and send back the Vid Object as Response.
	 *
	 * @param uin
	 *            the uin
	 * @param vidType
	 *            the vid type
	 * @return the vid
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private Vid generateVid(String uin, String vidType) throws IdRepoAppException {
		checkUinStatus(uin);
		int saltId = securityManager.getSaltKeyForId(uin);
		String encryptSalt = uinEncryptSaltRepo.retrieveSaltById(saltId);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(saltId);
		String uinToEncrypt = saltId + SPLITTER + uin + SPLITTER + encryptSalt;
		String uinHash = String.valueOf(saltId) + SPLITTER
				+ securityManager.hashwithSalt(uin.getBytes(), CryptoUtil.decodeBase64(hashSalt));
		LocalDateTime currentTime = DateUtils.getUTCCurrentDateTime();
		List<Vid> vidDetails = vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(uinHash,
				env.getProperty(VID_ACTIVE_STATUS), vidType, currentTime);
		Collections.sort(vidDetails);
		VidPolicy policy = policyProvider.getPolicy(vidType);
		if (Objects.isNull(vidDetails) || vidDetails.isEmpty() || vidDetails.size() < policy.getAllowedInstances()) {
			String vidRefId = UUIDUtils
					.getUUID(UUIDUtils.NAMESPACE_OID, uin + SPLITTER + DateUtils.getUTCCurrentDateTime()).toString();
			Vid vidEntity = new Vid(vidRefId, generateVid(), uinHash, uinToEncrypt, vidType, currentTime,
					Objects.nonNull(policy.getValidForInMinutes())
							? DateUtils.getUTCCurrentDateTime().plusMinutes(policy.getValidForInMinutes())
							: LocalDateTime.MAX.withYear(9999),
					env.getProperty(VID_ACTIVE_STATUS), IdRepoSecurityManager.getUser(), currentTime, null, null, false,
					null);
			// Get the salted ID Hash before modifiying the vid entity, otherwise result in
			// onFlushDirty call in the interceptor resulting in inconsistently encrypted
			// UIN value in VID entity
			notify(uin, env.getProperty(VID_ACTIVE_STATUS), Collections.singletonList(createVidInfo(vidEntity, getIdHashAndAttributes(vidEntity.getVid()))), false);
			return vidRepo.save(vidEntity);
		} else if (vidDetails.size() == policy.getAllowedInstances() && policy.getAutoRestoreAllowed()) {
			Vid vidObject = vidDetails.get(0);
			Map<String, String> idHashAndAttributes = getIdHashAndAttributes(vidObject.getVid());
			vidObject.setStatusCode(policy.getRestoreOnAction());
			vidObject.setUpdatedBy(IdRepoSecurityManager.getUser());
			vidObject.setUpdatedDTimes(DateUtils.getUTCCurrentDateTime());
			vidObject.setUin(uinToEncrypt);
			vidRepo.saveAndFlush(vidObject);
			// Get the salted ID Hash before modifiying the vid entity, otherwise result in
			// onFlushDirty call in the interceptor resulting in inconsistently encrypted
			// UIN value in VID entity
			notify(uin, env.getProperty(VID_DEACTIVATED), Collections.singletonList(createVidInfo(vidObject, idHashAndAttributes)), true);
			return generateVid(uin, vidType);
		} else {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, CREATE_VID,
					"throwing vid creation failed");
			throw new IdRepoAppException(VID_POLICY_FAILED);
		}
	}

	/**
	 * Generate vid.
	 *
	 * @return the vid
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String generateVid() throws IdRepoAppException {
		try {
			ResponseWrapper response = restHelper.requestSync(
					restBuilder.buildRequest(RestServicesConstants.VID_GENERATOR_SERVICE, null, ResponseWrapper.class));
			return ((Map<String, String>) response.getResponse()).get(VID);
		} catch (IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "generateVID",
					"\n" + e.getMessage());
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorText());
		} catch (RestServiceException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, CREATE_VID, e.getErrorText());
			throw new IdRepoAppException(VID_GENERATION_FAILED);
		}
	}

	/**
	 * Fetch details of the provided uin from Id Repository identity service and
	 * check if the uin is active or not. If not, Exception will be thrown.
	 *
	 * @param uin
	 *            the uin
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private void checkUinStatus(String uin) throws IdRepoAppException {
		try {
			RestRequestDTO request = restBuilder.buildRequest(RestServicesConstants.IDREPO_IDENTITY_SERVICE, null,
					IdResponseDTO.class);
			request.setPathVariables(Collections.singletonMap("uin", uin));
			IdResponseDTO identityResponse = restHelper.requestSync(request);
			String uinStatus = identityResponse.getResponse().getStatus();
			if (!uinStatus.equals(env.getProperty(ACTIVE_STATUS))) {
				throw new IdRepoAppException(INVALID_UIN.getErrorCode(),
						String.format(INVALID_UIN.getErrorMessage(), uinStatus));
			}
		} catch (RestServiceException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "checkUinStatus",
					"\n" + e.getMessage());
			List<ServiceError> errorList = ExceptionUtils.getServiceErrorList(
					e.getResponseBodyAsString().isPresent() ? e.getResponseBodyAsString().get() : null);
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "checkUinStatus", "\n" + errorList);
			if (Objects.nonNull(errorList) && !errorList.isEmpty()
					&& errorList.get(0).getErrorCode().equals(NO_RECORD_FOUND.getErrorCode())) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "checkUinStatus",
						"throwing no record found");
				throw new IdRepoAppException(NO_RECORD_FOUND);
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "checkUinStatus",
						"throwing UIN_RETRIEVAL_FAILED");
				throw new IdRepoAppException(UIN_RETRIEVAL_FAILED);
			}
		} catch (IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "checkUinStatus",
					"\n" + e.getMessage());
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorText());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.idrepository.core.spi.VidService#retrieveUinByVid(java.lang.String)
	 */
	@Override
	public ResponseWrapper<VidResponseDTO> retrieveUinByVid(String vid) throws IdRepoAppException {
		try {
			Vid vidObject = retrieveVidEntity(vid);
			if (vidObject != null) {
				String decryptedUin = decryptUin(vidObject.getUin(), vidObject.getUinHash());
				List<String> uinList = Arrays.asList(decryptedUin.split(SPLITTER));
				checkExpiry(vidObject.getExpiryDTimes());
				checkStatus(vidObject.getStatusCode());
				checkUinStatus(uinList.get(1));
				VidResponseDTO resDTO = new VidResponseDTO();
				resDTO.setUin(uinList.get(1));
				return buildResponse(resDTO, id.get("read"));
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, RETRIEVE_UIN_BY_VID,
						"throwing NO_RECORD_FOUND_VID");
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, RETRIEVE_UIN_BY_VID,
					"\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, RETRIEVE_UIN_BY_VID,
					e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.idrepository.core.spi.VidService#retrieveUinByVid(java.lang.String)
	 */
	@Override
	public VidsInfosDTO retrieveVidsByUin(String uin) throws IdRepoAppException {
		try {
			int saltId = securityManager.getSaltKeyForId(uin);
			String hashSalt = uinHashSaltRepo.retrieveSaltById(saltId);
			String uinHash = String.valueOf(saltId) + SPLITTER
					+ securityManager.hashwithSalt(uin.getBytes(), CryptoUtil.decodeBase64(hashSalt));
			List<Vid> vidList = vidRepo.findByUinHashAndStatusCodeAndExpiryDTimesAfter(uinHash,
					env.getProperty(VID_ACTIVE_STATUS), DateUtils.getUTCCurrentDateTime());
			// Get the salted ID Hash before modifiying the vid entity, otherwise result in
			// onFlushDirty call in the interceptor resulting in inconsistently encrypted
			// UIN value in VID entity
			List<VidInfoDTO> vidInfos = vidList.stream()
					.map(vid -> createVidInfo(vid, getIdHashAndAttributes(vid.getVid())))
					.collect(Collectors.toList());
			VidsInfosDTO response = new VidsInfosDTO();
			response.setResponse(vidInfos);
			return response;
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, RETRIEVE_UIN_BY_VID,
					"\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, RETRIEVE_UIN_BY_VID,
					e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.idrepository.core.spi.VidService#updateVid(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public ResponseWrapper<VidResponseDTO> updateVid(String vid, VidRequestDTO request) throws IdRepoAppException {
		try {
			String vidStatus = request.getVidStatus();
			Vid vidObject = retrieveVidEntity(vid);
			if (Objects.isNull(vidObject)) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, UPDATE_VID,
						"throwing NO_RECORD_FOUND_VID");
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			checkStatus(vidObject.getStatusCode());
			checkExpiry(vidObject.getExpiryDTimes());
			String decryptedUin = decryptUin(vidObject.getUin(), vidObject.getUinHash());
			VidPolicy policy = policyProvider.getPolicy(vidObject.getVidTypeCode());
			VidResponseDTO response = updateVidStatus(vidStatus, vidObject, decryptedUin, policy);
			return buildResponse(response, id.get("update"));
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, UPDATE_VID, "\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, UPDATE_VID, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	/**
	 * This method will update the status and VidResponseDTO will be sent back as
	 * response.
	 *
	 * @param vidStatus
	 *            the vid status
	 * @param vidObject
	 *            the vid object
	 * @param decryptedUin
	 *            the decrypted uin
	 * @param policy
	 *            the policy
	 * @return VidResponseDTO
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private VidResponseDTO updateVidStatus(String vidStatus, Vid vidObject, String decryptedUin, VidPolicy policy)
			throws IdRepoAppException {
		String uin = Arrays.asList(decryptedUin.split(SPLITTER)).get(1);
		// Get the salted ID Hash before modifiying the vid entity, otherwise result in
		// onFlushDirty call in the interceptor resulting in inconsistently encrypted
		// UIN value in VID entity
		Map<String, String> idHashAndAttributes = getIdHashAndAttributes(vidObject.getVid());
		if (!(vidStatus.equals(env.getProperty(VID_UNLIMITED_TRANSACTION_STATUS))
				&& Objects.isNull(policy.getAllowedTransactions()))) {
			vidObject.setStatusCode(vidStatus);
			vidObject.setUpdatedBy(IdRepoSecurityManager.getUser());
			vidObject.setUpdatedDTimes(DateUtils.getUTCCurrentDateTime());
			vidObject.setUin(decryptedUin);
			vidRepo.saveAndFlush(vidObject);
			VidInfoDTO vidInfo = createVidInfo(vidObject, idHashAndAttributes);
			notify(decryptedUin, vidStatus, Collections.singletonList(vidInfo), true);
		}
		VidResponseDTO response = new VidResponseDTO();
		response.setVidStatus(vidObject.getStatusCode());
		if (policy.getAutoRestoreAllowed() && policy.getRestoreOnAction().equals(vidStatus)) {
			Vid createVidResponse = generateVid(uin, vidObject.getVidTypeCode());
			VidResponseDTO restoredVidDTO = new VidResponseDTO();
			restoredVidDTO.setVid(createVidResponse.getVid());
			restoredVidDTO.setVidStatus(createVidResponse.getStatusCode());
			response.setRestoredVid(restoredVidDTO);
		}
		return response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.idrepository.core.spi.VidService#regenerateVid(java.lang.String)
	 */
	@Override
	public ResponseWrapper<VidResponseDTO> regenerateVid(String vid) throws IdRepoAppException {
		try {
			Vid vidObject = retrieveVidEntity(vid);
			if (Objects.isNull(vidObject)) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, REGENERATE_VID,
						"throwing NO_RECORD_FOUND_VID");
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			VidPolicy policy = policyProvider.getPolicy(vidObject.getVidTypeCode());
			if (policy.getAutoRestoreAllowed()) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, REGENERATE_VID,
						"throwing Vid Regeneration Failed");
				throw new IdRepoAppException(VID_POLICY_FAILED);
			}
			checkRegenerateStatus(vidObject.getStatusCode());
			String decryptedUin = decryptUin(vidObject.getUin(), vidObject.getUinHash());
			updateVidStatus(VID_REGENERATE_ACTIVE_STATUS, vidObject, decryptedUin, policy);
			List<String> uinList = Arrays.asList(decryptedUin.split(SPLITTER));
			VidResponseDTO response = new VidResponseDTO();
			Vid generateVidObject = generateVid(uinList.get(1), vidObject.getVidTypeCode());
			response.setVid(generateVidObject.getVid());
			response.setVidStatus(generateVidObject.getStatusCode());
			return buildResponse(response, id.get("regenerate"));
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, REGENERATE_VID,
					"\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, REGENERATE_VID, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.idrepository.core.spi.VidService#deactivateVIDsForUIN(java.lang.
	 * String)
	 */
	@Override
	public ResponseWrapper<VidResponseDTO> deactivateVIDsForUIN(String uin) throws IdRepoAppException {
		return applyVIDStatus(uin, env.getProperty(VID_DEACTIVATED), DEACTIVATE, env.getProperty(VID_ACTIVE_STATUS));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.mosip.idrepository.core.spi.VidService#reactivateVIDsForUIN(java.lang.
	 * String)
	 */
	@Override
	public ResponseWrapper<VidResponseDTO> reactivateVIDsForUIN(String uin) throws IdRepoAppException {
		return applyVIDStatus(uin, env.getProperty(VID_ACTIVE_STATUS), REACTIVATE, env.getProperty(VID_DEACTIVATED));
	}

	/**
	 * Apply VID status.
	 *
	 * @param uin
	 *            the uin
	 * @param status
	 *            the status
	 * @param idType
	 *            the id type
	 * @param vidStatusToRetrieveVIDList
	 *            the vid status to retrieve VID list
	 * @return the response wrapper
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private ResponseWrapper<VidResponseDTO> applyVIDStatus(String uin, String status, String idType,
			String vidStatusToRetrieveVIDList) throws IdRepoAppException {
		int saltId = securityManager.getSaltKeyForId(uin);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(saltId);
		String uinHash = String.valueOf(saltId) + SPLITTER
				+ securityManager.hashwithSalt(uin.getBytes(), CryptoUtil.decodeBase64(hashSalt));
		List<Vid> vidList = vidRepo.findByUinHashAndStatusCodeAndExpiryDTimesAfter(uinHash, vidStatusToRetrieveVIDList,
				DateUtils.getUTCCurrentDateTime());
		if (!vidList.isEmpty()) {
			String decryptedUin = decryptUin(vidList.get(0).getUin(), uinHash);
			List<VidInfoDTO> vidInfos = new ArrayList<>();
			// Get the salted ID Hash before modifiying the vid entity, otherwise result in
			// onFlushDirty call in the interceptor resulting in inconsistently encrypted
			// UIN value in VID entity
			Map<String, Map<String, String>> vidHashAttributesMap = vidList.stream().collect(Collectors.toMap(Vid::getVid, vid -> getIdHashAndAttributes(vid.getVid())));
			vidList.forEach(vid -> {
				Map<String, String> idHashAndAttributes = vidHashAttributesMap.get(vid.getVid());
				vid.setStatusCode(status);
				vid.setUpdatedBy(IdRepoSecurityManager.getUser());
				vid.setUpdatedDTimes(DateUtils.getUTCCurrentDateTime());
				vid.setUin(decryptedUin);
				vidInfos.add(createVidInfo(vid, idHashAndAttributes));
			});
			
			vidRepo.saveAll(vidList);
			if (idType.contentEquals(DEACTIVATE)) {
				notify(uin, status, vidInfos, true);
			} else {
				notify(uin, status, vidInfos, true);
			}
			VidResponseDTO response = new VidResponseDTO();
			response.setVidStatus(status);
			return buildResponse(response, id.get(idType));
		} else {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "deactivateVIDsForUIN",
					"throwing NO_RECORD_FOUND_VID");
			throw new IdRepoAppException(NO_RECORD_FOUND);
		}
	}

	private VidInfoDTO createVidInfo(Vid vid, Map<String, String> idHashAttributes) {
		return new VidInfoDTO(vid.getVid(), 
				vid.getVidTypeCode(),
				vid.getExpiryDTimes(), 
				policyProvider.getPolicy(vid.getVidTypeCode()).getAllowedTransactions(), 
				idHashAttributes);
	}

	/**
	 * This method will verify the status of vid from the allowed status available.
	 *
	 * @param statusCode
	 *            the status code
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private void checkRegenerateStatus(String statusCode) throws IdRepoAppException {
		String allowedStatus = env.getProperty(VID_REGENERATE_ALLOWED_STATUS);
		List<String> allowedStatusList = Arrays.asList(allowedStatus.split(","));
		if (!allowedStatusList.contains(statusCode)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "checkRegenerateStatus",
					"throwing " + statusCode + " VID");
			throw new IdRepoAppException(INVALID_VID.getErrorCode(),
					String.format(INVALID_VID.getErrorMessage(), statusCode));
		}
	}

	/**
	 * This Method will accepts vid as parameter and will return Vid Object from DB.
	 *
	 * @param vid
	 *            the vid
	 * @return the vid
	 */
	private Vid retrieveVidEntity(String vid) {
		return vidRepo.findByVid(vid);
	}

	/**
	 * This method will check expiry date of the vid, if vid is expired then it will
	 * throw IdRepoAppException.
	 *
	 * @param expiryDTimes
	 *            the expiry D times
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private void checkExpiry(LocalDateTime expiryDTimes) throws IdRepoAppException {
		if (!DateUtils.after(expiryDTimes, DateUtils.getUTCCurrentDateTime())) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "checkExpiry",
					"throwing Expired VID");
			throw new IdRepoAppException(INVALID_VID.getErrorCode(),
					String.format(INVALID_VID.getErrorMessage(), EXPIRED));
		}
	}

	/**
	 * This method will check Status of the vid.
	 *
	 * @param statusCode
	 *            the status code
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private void checkStatus(String statusCode) throws IdRepoAppException {
		if (!statusCode.equalsIgnoreCase(env.getProperty(VID_ACTIVE_STATUS))) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "checkStatus",
					"throwing INVALID_VID with status - " + statusCode);
			throw new IdRepoAppException(INVALID_VID.getErrorCode(),
					String.format(INVALID_VID.getErrorMessage(), statusCode));
		}
	}

	/**
	 * This Method is used to decrypt the UIN stored in DB.
	 *
	 * @param uin
	 *            the uin
	 * @param uinHash
	 *            the uin hash
	 * @return the string
	 * @throws IdRepoAppException
	 *             the id repo app exception
	 */
	private String decryptUin(String uin, String uinHash) throws IdRepoAppException {
		List<String> uinDetails = Arrays.stream(uin.split(SPLITTER)).collect(Collectors.toList());
		String decryptSalt = uinEncryptSaltRepo.retrieveSaltById(Integer.parseInt(uinDetails.get(0)));
		String hashSalt = uinHashSaltRepo.retrieveSaltById(Integer.parseInt(uinDetails.get(0)));
		String encryptedUin = uin.substring(uinDetails.get(0).length() + 1, uin.length());
		String decryptedUin = new String(securityManager.decryptWithSalt(CryptoUtil.decodeBase64(encryptedUin),
				CryptoUtil.decodeBase64(decryptSalt), uinRefId));
		String uinHashWithSalt = uinDetails.get(0) + SPLITTER
				+ securityManager.hashwithSalt(decryptedUin.getBytes(), CryptoUtil.decodeBase64(hashSalt));
		if (!MessageDigest.isEqual(uinHashWithSalt.getBytes(), uinHash.getBytes())) {
			throw new IdRepoAppUncheckedException(UIN_HASH_MISMATCH);
		}
		return uinDetails.get(0) + SPLITTER + decryptedUin + SPLITTER + decryptSalt;
	}

	/**
	 * This Method will build the Vid Response.
	 *
	 * @param response
	 *            the response
	 * @param id
	 *            the id
	 * @return the response wrapper
	 */
	private ResponseWrapper<VidResponseDTO> buildResponse(VidResponseDTO response, String id) {
		ResponseWrapper<VidResponseDTO> responseDto = new ResponseWrapper<>();
		responseDto.setId(id);
		responseDto.setVersion(env.getProperty(APPLICATION_VERSION_VID));
		responseDto.setResponse(response);
		return responseDto;
	}
	
	@Transactional(propagation = Propagation.NEVER)
	private void notify(String uin, String status, List<VidInfoDTO> vids, boolean isUpdated) {
		try {
			List<String> partnerIds = getPartnerIds();
			if (isUpdated) {
				sendEventsToIDA(status, vids, partnerIds);
			} else {
				sendEventsToCredService(uin, status, vids, isUpdated, partnerIds);
			}
		} catch (Exception e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "getPartnerIds",
					e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	private List<String> getPartnerIds() {
		try {
			Map<String, Object> responseWrapperMap = restHelper.requestSync(restBuilder.buildRequest(RestServicesConstants.PARTNER_SERVICE, null, Map.class));
			Object response = responseWrapperMap.get("response");
			if(response instanceof Map) {
				Object partners = ((Map<String, ?>)response).get("partners");
				if(partners instanceof List) {
					List<Map<String, Object>> partnersList = (List<Map<String, Object>>) partners;
					return partnersList.stream()
								.filter(partner -> PARNER_ACTIVE_STATUS.equalsIgnoreCase((String)partner.get("status")))
								.map(partner -> (String)partner.get("partnerID"))
								.collect(Collectors.toList());
				}
			}
		} catch (RestServiceException | IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "getPartnerIds", e.getMessage());
		}
		return Collections.emptyList();
	}

	private void sendEventsToCredService(String uin, String status, List<VidInfoDTO> vids, boolean isUpdated, List<String> partnerIds) {
		List<CredentialIssueRequestDto> eventRequestsList = vids.stream()
					.flatMap(vid -> {
						LocalDateTime expiryTimestamp = status.equals(env.getProperty(VID_ACTIVE_STATUS)) ? vid.getExpiryTimestamp() : DateUtils.getUTCCurrentDateTime();
						return partnerIds.stream().map(partnerId -> {
							String token = tokenIDGenerator.generateTokenID(uin, partnerId);
							return createCredReqDto(vid.getVid(), partnerId,
									expiryTimestamp, vid.getTransactionLimit(), token, IdType.VID.getIdType());
						});
					})
					.collect(Collectors.toList());
		
		eventRequestsList.forEach(reqDto -> {
			CredentialIssueRequestWrapperDto requestWrapper = new CredentialIssueRequestWrapperDto();
			requestWrapper.setRequest(reqDto);
			requestWrapper.setRequesttime(DateUtils.getUTCCurrentDateTime());
			requestWrapper.setId(env.getProperty(IDA_NOTIFY_REQ_ID));
			requestWrapper.setVersion(env.getProperty(IDA_NOTIFY_REQ_VER));
			String eventTypeDisplayName = isUpdated? "Update ID" : "Create ID";
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "notify", "notifying Credential Service for event " + eventTypeDisplayName);
			sendEventToCredService(requestWrapper);
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "notify", "notified Credential Service for event" + eventTypeDisplayName);
		});
	}
	
	private void sendEventToCredService(CredentialIssueRequestWrapperDto requestWrapper) {
		try {
			restHelper.requestSync(restBuilder.buildRequest(RestServicesConstants.CREDENTIAL_REQUEST_SERVICE, requestWrapper, Map.class));
		} catch (RestServiceException | IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "sendRequestToCredService", e.getMessage());
		}		
	}
	
	private CredentialIssueRequestDto createCredReqDto(String id, String partnerId, LocalDateTime expiryTimestamp, Integer transactionLimit, String token, String idType) {
		Map<String, Object> data = new HashMap<>();
		data.putAll(getIdHashAndAttributes(id));
		data.put(EXPIRY_TIMESTAMP, Optional.ofNullable(expiryTimestamp).map(DateUtils::formatToISOString).orElse(null));
		data.put(TRANSACTION_LIMIT, transactionLimit);
		data.put(TOKEN, token);
		data.put(ID_TYPE, idType);

		CredentialIssueRequestDto credentialIssueRequestDto = new CredentialIssueRequestDto();
		credentialIssueRequestDto.setId(id);
		credentialIssueRequestDto.setCredentialType(credentialType);
		credentialIssueRequestDto.setIssuer(partnerId);
		credentialIssueRequestDto.setRecepiant(credentialRecepiant);
		credentialIssueRequestDto.setUser(IdRepoSecurityManager.getUser());
		credentialIssueRequestDto.setAdditionalData(data);
		return credentialIssueRequestDto;
	}
	
	private Map<String, String> getIdHashAndAttributes(String id) {
		Map<String, String> hashWithAttributes = new HashMap<>();
		int saltId = securityManager.getSaltKeyForId(id);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(saltId);
		String hash = securityManager.hashwithSalt(id.getBytes(), hashSalt.getBytes());
		hashWithAttributes.put(ID_HASH, hash);
		hashWithAttributes.put(MODULO, String.valueOf(saltId));
		hashWithAttributes.put(SALT, hashSalt);
		return hashWithAttributes;
	}

	private void sendEventsToIDA(String status,  List<VidInfoDTO> vids, List<String> partnerIds) {
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
		eventDtos.forEach(eventDto -> sendEventToIDA(eventDto));
	}
	
	private void sendEventToIDA(EventModel model) {
		try {
			mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "sendEventToIDA", "Trying registering topic: " + model.getTopic());
			pb.registerTopic(model.getTopic(), webSubPublishUrl);
		} catch (Exception e) {
			//Exception will be there if topic already registered. Ignore that
			mosipLogger.warn(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "sendEventToIDA", "Error in registering topic: " + model.getTopic() + " : " + e.getMessage() );
		}
		mosipLogger.info(IdRepoSecurityManager.getUser(), this.getClass().getSimpleName(), "sendEventToIDA", "Publising event to topic: " + model.getTopic());
		pb.publishUpdate(model.getTopic(), model, MediaType.APPLICATION_JSON_VALUE, null, webSubPublishUrl);
	}

	private Stream<EventModel> createIdaEventModel(EventType eventType, String id, LocalDateTime expiryTimestamp, Integer transactionLimit, List<String> partnerIds, String transactionId, String idHash) {
		return partnerIds.stream().map(partner -> createEventModel(eventType, id, expiryTimestamp, transactionLimit, transactionId, partner, idHash));
	}

	private EventModel createEventModel(EventType eventType, String id, LocalDateTime expiryTimestamp, Integer transactionLimit, String transactionId, String partner, Object idHash) {
		EventModel model = new EventModel();
		model.setPublisher(ID_REPO_VID_SERVICE);
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
		if(eventType.equals(IDAEventType.DEACTIVATE_ID)) {
			data.put(EXPIRY_TIMESTAMP, DateUtils.formatToISOString(DateUtils.getUTCCurrentDateTime()));
		} else {
			if(expiryTimestamp != null) {
				data.put(EXPIRY_TIMESTAMP, DateUtils.formatToISOString(expiryTimestamp));
			}
		}	
		data.put(TRANSACTION_LIMIT, transactionLimit);
		event.setData(data);
		model.setEvent(event);
		model.setTopic(partner + "/" + eventType.toString());
		return model;
	}
	
}