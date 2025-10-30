package io.mosip.idrepository.vid.service.impl;

import static io.mosip.idrepository.core.constant.AuditModules.ID_REPO_VID_SERVICE;
import static io.mosip.idrepository.core.constant.IdRepoConstants.DRAFT_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.SPLITTER;
import static io.mosip.idrepository.core.constant.IdRepoConstants.UIN_REFID;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_EVENT_TOPIC;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_REGENERATE_ACTIVE_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoConstants.VID_REGENERATE_ALLOWED_STATUS;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.DATABASE_ACCESS_ERROR;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_INPUT_PARAMETER;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_UIN;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.INVALID_VID;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.NO_RECORD_FOUND;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UIN_HASH_MISMATCH;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.UIN_RETRIEVAL_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.VID_GENERATION_FAILED;
import static io.mosip.idrepository.core.constant.IdRepoErrorConstants.VID_POLICY_FAILED;

import java.io.Serializable;
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
import java.util.stream.Collectors;

import jakarta.annotation.Resource;

import org.hibernate.exception.JDBCConnectionException;
import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Transactional;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.RestServicesConstants;
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
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.repository.UinEncryptSaltRepo;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.VidService;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.vid.entity.Vid;
import io.mosip.idrepository.vid.provider.VidPolicyProvider;
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

/**
 * Optimized VidServiceImpl — synchronous, drop-in replacement.
 */
@Component
@Transactional
public class VidServiceImpl implements VidService<VidRequestDTO, ResponseWrapper<VidResponseDTO>, ResponseWrapper<List<VidInfoDTO>>> {

	private static final String THROWING_NO_RECORD_FOUND_VID = "throwing NO_RECORD_FOUND_VID";
	private static final String CHECK_UIN_STATUS = "checkUinStatus";
	private static final String VID = "vid";
	private static final String REACTIVATE = "reactivate";
	private static final String DEACTIVATE = "deactivate";
	private static final String REGENERATE_VID = "regenerateVid";
	private static final String UPDATE_VID = "updateVid";
	private static final String CREATE_VID = "createVid";
	private static final String RETRIEVE_UIN_BY_VID = "retrieveUinByVid";

	private Logger mosipLogger = IdRepoLogger.getLogger(VidServiceImpl.class);

	@Value("${" + UIN_REFID + "}")
	private String uinRefId;

	@Value("${" + VID_EVENT_TOPIC + "}")
	private String vidEventTopic;

	@Value("${" + VID_ACTIVE_STATUS + "}")
	private String vidActiveStatus;

	@Value("${" + VID_REGENERATE_ALLOWED_STATUS + "}")
	private String allowedStatus;

	@Autowired
	private VidRepo vidRepo;

	@Autowired
	private RestRequestBuilder restBuilder;

	@Autowired
	private RestHelper restHelper;

	@Autowired
	private VidPolicyProvider policyProvider;

	@Autowired
	private IdRepoSecurityManager securityManager;

	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;

	@Autowired
	private UinEncryptSaltRepo uinEncryptSaltRepo;

	@Resource
	private Map<String, String> id;

	@Autowired
	private CredentialServiceManager credentialServiceManager;

	@Autowired
	private IdRepoWebSubHelper websubHelper;

	@Autowired
	private Environment env;

	@Autowired(required = true)
	@Qualifier("mask")
	VariableResolverFactory functionFactory;

	@Value("${mosip.mask.function.identityAttributes}")
	private String identityAttribute;

	@Override
	public ResponseWrapper<VidResponseDTO> generateVid(VidRequestDTO vidRequest) throws IdRepoAppException {
		String uin = vidRequest.getUin();
		try {
			Vid vid;
			if (Objects.nonNull(vidRequest.getVidStatus()) && vidRequest.getVidStatus().contentEquals(DRAFT_STATUS)) {
				vid = generateVid(uin, vidRequest.getVidType(), DRAFT_STATUS);
			} else {
				vid = generateVidWithActiveUin(uin, vidRequest.getVidType());
			}
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

	private Vid generateVidWithActiveUin(String uin, String vidType) throws IdRepoAppException {
		checkUinStatus(uin);
		return generateVid(uin, vidType, EnvUtil.getVidActiveStatus());
	}

	private Vid generateVid(String uin, String vidType, String vidStatus) throws IdRepoAppException {
		int saltId = securityManager.getSaltKeyForId(uin);

		// cache salt lookups locally to avoid multiple repo calls
		String encryptSalt = uinEncryptSaltRepo.retrieveSaltById(saltId);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(saltId);

		String uinToEncrypt = saltId + SPLITTER + uin + SPLITTER + encryptSalt;
		String uinHash = String.valueOf(saltId) + SPLITTER
				+ securityManager.hashwithSalt(uin.getBytes(), CryptoUtil.decodePlainBase64(hashSalt));

		LocalDateTime currentTime = DateUtils.getUTCCurrentDateTime();

		// Find existing active/specified status VIDs
		List<Vid> vidDetails = vidRepo.findByUinHashAndStatusCodeAndVidTypeCodeAndExpiryDTimesAfter(uinHash, vidStatus,
				vidType, currentTime);

		// Get policy once
		VidPolicy policy = policyProvider.getPolicy(vidType);

		// if none or less than allowed instances -> create new
		if (vidDetails == null || vidDetails.isEmpty() || vidDetails.size() < policy.getAllowedInstances()) {
			String vidRefId = UUIDUtils.getUUID(UUIDUtils.NAMESPACE_OID, uin + SPLITTER + DateUtils.getUTCCurrentDateTime())
					.toString();
			Vid vidEntity = new Vid(vidRefId, generateVid(), uinHash, uinToEncrypt, vidType, currentTime,
					Objects.nonNull(policy.getValidForInMinutes())
							? DateUtils.getUTCCurrentDateTime().plusMinutes(policy.getValidForInMinutes())
							: LocalDateTime.MAX.withYear(9999),
					vidStatus, IdRepoSecurityManager.getUser(), currentTime, null, null, false, null);

			// notify only when not draft
			if (!vidStatus.contentEquals(DRAFT_STATUS)) {
				notify(uin, vidStatus,
						Collections.singletonList(createVidInfo(vidEntity, getIdHashAndAttributesForIDAEvent(vidEntity.getVid()))),
						false);
			}
			return vidRepo.save(vidEntity);
		}

		// if equal to allowedInstances and auto restore allowed -> restore oldest (or first)
		if (vidDetails.size() == policy.getAllowedInstances() && Boolean.TRUE.equals(policy.getAutoRestoreAllowed())) {
			// pick the oldest/generated earliest (to be consistent with previous sort behavior)
			Vid vidObject = vidDetails.stream()
					.min((v1, v2) -> v1.getGeneratedDTimes().compareTo(v2.getGeneratedDTimes()))
					.orElse(vidDetails.get(0));

			Map<String, String> idHashAndAttributes = getIdHashAndAttributesForIDAEvent(vidObject.getVid());

			vidObject.setStatusCode(policy.getRestoreOnAction());
			vidObject.setUpdatedBy(IdRepoSecurityManager.getUser());
			vidObject.setUpdatedDTimes(DateUtils.getUTCCurrentDateTime());
			vidObject.setUin(uinToEncrypt);
			vidRepo.saveAndFlush(vidObject);

			if (!vidStatus.contentEquals(DRAFT_STATUS)) {
				notify(uin, EnvUtil.getVidDeactivatedStatus(),
						Collections.singletonList(createVidInfo(vidObject, idHashAndAttributes)), true);
			}
			// regenerate new VID after restoring one
			return generateVid(uin, vidType, vidStatus);
		}

		// otherwise policy prevents creation
		mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, CREATE_VID, "throwing vid creation failed");
		throw new IdRepoAppException(VID_POLICY_FAILED);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String generateVid() throws IdRepoAppException {
		try {
			ResponseWrapper response = restHelper.requestSync(
					restBuilder.buildRequest(RestServicesConstants.VID_GENERATOR_SERVICE, null, ResponseWrapper.class));
			return ((Map<String, String>) response.getResponse()).get(VID);
		} catch (IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "generateVID", "\n" + e.getMessage());
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorText());
		} catch (RestServiceException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, CREATE_VID, e.getErrorText());
			throw new IdRepoAppException(VID_GENERATION_FAILED);
		}
	}

	private void checkUinStatus(String uin) throws IdRepoAppException {
		try {
			RestRequestDTO request = restBuilder.buildRequest(RestServicesConstants.IDREPO_IDENTITY_SERVICE, null,
					IdResponseDTO.class);
			request.setPathVariables(Collections.singletonMap("uin", uin));
			IdResponseDTO identityResponse = restHelper.requestSync(request);
			String uinStatus = identityResponse.getResponse().getStatus();
			if (!uinStatus.equals(EnvUtil.getUinActiveStatus())) {
				throw new IdRepoAppException(INVALID_UIN.getErrorCode(),
						String.format(INVALID_UIN.getErrorMessage(), uinStatus));
			}
		} catch (RestServiceException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, CHECK_UIN_STATUS, "\n" + e.getMessage());
			List<ServiceError> errorList = ExceptionUtils.getServiceErrorList(e.getResponseBodyAsString().orElse(null));
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, CHECK_UIN_STATUS, "\n" + errorList);
			if (Objects.nonNull(errorList) && !errorList.isEmpty()
					&& errorList.get(0).getErrorCode().equals(NO_RECORD_FOUND.getErrorCode())) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, CHECK_UIN_STATUS,
						"throwing no record found");
				throw new IdRepoAppException(NO_RECORD_FOUND);
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, CHECK_UIN_STATUS,
						"throwing UIN_RETRIEVAL_FAILED");
				throw new IdRepoAppException(UIN_RETRIEVAL_FAILED);
			}
		} catch (IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, CHECK_UIN_STATUS, "\n" + e.getMessage());
			throw new IdRepoAppException(INVALID_INPUT_PARAMETER.getErrorCode(), e.getErrorText());
		}
	}

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
						THROWING_NO_RECORD_FOUND_VID);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, RETRIEVE_UIN_BY_VID,
					"\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, RETRIEVE_UIN_BY_VID, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	@Override
	public VidsInfosDTO retrieveVidsByUin(String uin) throws IdRepoAppException {
		try {
			int saltId = securityManager.getSaltKeyForId(uin);
			String hashSalt = uinHashSaltRepo.retrieveSaltById(saltId);
			String uinHash = String.valueOf(saltId) + SPLITTER
					+ securityManager.hashwithSalt(uin.getBytes(), CryptoUtil.decodePlainBase64(hashSalt));

			List<Vid> vidList = vidRepo.findByUinHashAndStatusCodeAndExpiryDTimesAfter(uinHash,
					EnvUtil.getVidActiveStatus(), DateUtils.getUTCCurrentDateTime());

			if (!vidList.isEmpty()) {
				// Precompute idHash for each vid once
				Map<String, Map<String, String>> vidHashAttrMap = vidList.stream()
						.collect(Collectors.toMap(Vid::getVid, v -> getIdHashAndAttributesForIDAEvent(v.getVid())));

				List<VidInfoDTO> vidInfos = vidList.stream()
						.map(vid -> createVidInfo(vid, vidHashAttrMap.get(vid.getVid())))
						.collect(Collectors.toList());

				VidsInfosDTO response = new VidsInfosDTO();
				response.setResponse(vidInfos);
				return response;
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "retrieveVidsByUin",
						THROWING_NO_RECORD_FOUND_VID);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, RETRIEVE_UIN_BY_VID,
					"\n" + e.getMessage());
			throw new IdRepoAppException(e.getErrorCode(), e.getErrorText(), e);
		} catch (DataAccessException | TransactionException | JDBCConnectionException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, RETRIEVE_UIN_BY_VID, e.getMessage());
			throw new IdRepoAppException(DATABASE_ACCESS_ERROR, e);
		}
	}

	@Override
	public ResponseWrapper<VidResponseDTO> updateVid(String vid, VidRequestDTO request) throws IdRepoAppException {
		try {
			String vidStatus = request.getVidStatus();
			Vid vidObject = retrieveVidEntity(vid);
			if (Objects.isNull(vidObject)) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, UPDATE_VID,
						THROWING_NO_RECORD_FOUND_VID);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
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

	private VidResponseDTO updateVidStatus(String vidStatus, Vid vidObject, String decryptedUin, VidPolicy policy)
			throws IdRepoAppException {
		String uin = Arrays.asList(decryptedUin.split(SPLITTER)).get(1);

		Map<String, String> idHashAndAttributes = getIdHashAndAttributesForIDAEvent(vidObject.getVid());

		if (!(vidStatus.equals(EnvUtil.getVidUnlimitedTxnStatus())
				&& Objects.isNull(policy.getAllowedTransactions()))) {

			vidObject.setStatusCode(vidStatus);
			vidObject.setUpdatedBy(IdRepoSecurityManager.getUser());
			vidObject.setUpdatedDTimes(DateUtils.getUTCCurrentDateTime());
			vidObject.setUin(decryptedUin);
			vidRepo.saveAndFlush(vidObject);
			VidInfoDTO vidInfo = createVidInfo(vidObject, idHashAndAttributes);
			notify(decryptedUin, vidStatus, Collections.singletonList(vidInfo),
					!vidActiveStatus.contentEquals(vidStatus));
		}

		VidResponseDTO response = new VidResponseDTO();
		response.setVidStatus(vidObject.getStatusCode());
		if (Boolean.TRUE.equals(policy.getAutoRestoreAllowed()) && policy.getRestoreOnAction().equals(vidStatus)) {
			Vid createVidResponse = generateVidWithActiveUin(uin, vidObject.getVidTypeCode());
			VidResponseDTO restoredVidDTO = new VidResponseDTO();
			restoredVidDTO.setVid(createVidResponse.getVid());
			restoredVidDTO.setVidStatus(createVidResponse.getStatusCode());
			response.setRestoredVid(restoredVidDTO);
		}
		return response;
	}

	private Map<String, String> getIdHashAndAttributesForIDAEvent(String id) {
		return securityManager.getIdHashAndAttributesWithSaltModuloByPlainIdHash(id, uinHashSaltRepo::retrieveSaltById);
	}

	@Override
	public ResponseWrapper<VidResponseDTO> regenerateVid(String vid) throws IdRepoAppException {
		try {
			Vid vidObject = retrieveVidEntity(vid);
			if (Objects.isNull(vidObject)) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, REGENERATE_VID,
						THROWING_NO_RECORD_FOUND_VID);
				throw new IdRepoAppException(NO_RECORD_FOUND);
			}
			VidPolicy policy = policyProvider.getPolicy(vidObject.getVidTypeCode());
			if (Boolean.TRUE.equals(policy.getAutoRestoreAllowed())) {
				mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, REGENERATE_VID,
						"throwing Vid Regeneration Failed");
				throw new IdRepoAppException(VID_POLICY_FAILED);
			}
			checkRegenerateStatus(vidObject.getStatusCode());
			String decryptedUin = decryptUin(vidObject.getUin(), vidObject.getUinHash());
			updateVidStatus(VID_REGENERATE_ACTIVE_STATUS, vidObject, decryptedUin, policy);
			List<String> uinList = Arrays.asList(decryptedUin.split(SPLITTER));
			VidResponseDTO response = new VidResponseDTO();
			Vid generateVidObject = generateVidWithActiveUin(uinList.get(1), vidObject.getVidTypeCode());
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

	@Override
	public ResponseWrapper<VidResponseDTO> deactivateVIDsForUIN(String uin) throws IdRepoAppException {
		return applyVIDStatus(uin, EnvUtil.getVidDeactivatedStatus(), DEACTIVATE, EnvUtil.getVidActiveStatus());
	}

	@Override
	public ResponseWrapper<VidResponseDTO> reactivateVIDsForUIN(String uin) throws IdRepoAppException {
		return applyVIDStatus(uin, EnvUtil.getVidActiveStatus(), REACTIVATE, EnvUtil.getVidDeactivatedStatus());
	}

	private ResponseWrapper<VidResponseDTO> applyVIDStatus(String uin, String status, String idType,
														   String vidStatusToRetrieveVIDList) throws IdRepoAppException {

		int saltId = securityManager.getSaltKeyForId(uin);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(saltId);
		String uinHash = String.valueOf(saltId) + SPLITTER
				+ securityManager.hashwithSalt(uin.getBytes(), CryptoUtil.decodePlainBase64(hashSalt));

		List<Vid> vidList = vidRepo.findByUinHashAndStatusCodeAndExpiryDTimesAfter(uinHash, vidStatusToRetrieveVIDList,
				DateUtils.getUTCCurrentDateTime());

		if (!vidList.isEmpty()) {

			// decrypt once using first record's uin and uinHash
			String decryptedUin = decryptUin(vidList.get(0).getUin(), uinHash);

			// Precompute idHash map once
			Map<String, Map<String, String>> vidHashAttributesMap = vidList.stream()
					.collect(Collectors.toMap(Vid::getVid, vid -> getIdHashAndAttributesForIDAEvent(vid.getVid())));

			List<VidInfoDTO> vidInfos = new ArrayList<>(vidList.size());
			for (Vid v : vidList) {
				Map<String, String> idHashAndAttributes = vidHashAttributesMap.get(v.getVid());
				v.setStatusCode(status);
				v.setUpdatedBy(IdRepoSecurityManager.getUser());
				v.setUpdatedDTimes(DateUtils.getUTCCurrentDateTime());
				v.setUin(decryptedUin);
				vidInfos.add(createVidInfo(v, idHashAndAttributes));
			}

			vidRepo.saveAll(vidList); // one batch save
			notify(uin, status, vidInfos, idType.contentEquals(DEACTIVATE));
			VidResponseDTO response = new VidResponseDTO();
			response.setVidStatus(status);
			return buildResponse(response, id.get(idType));
		} else {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "deactivateVIDsForUIN",
					THROWING_NO_RECORD_FOUND_VID);
			throw new IdRepoAppException(NO_RECORD_FOUND);
		}
	}

	private VidInfoDTO createVidInfo(Vid vid, Map<String, String> idHashAttributes) {
		return new VidInfoDTO(vid.getVid(), vid.getVidTypeCode(), vid.getExpiryDTimes(), vid.getGeneratedDTimes(),
				policyProvider.getPolicy(vid.getVidTypeCode()).getAllowedTransactions(), idHashAttributes);
	}

	private void checkRegenerateStatus(String statusCode) throws IdRepoAppException {
		List<String> allowedStatusList = Arrays.asList(allowedStatus.split(","));
		if (!allowedStatusList.contains(statusCode)) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "checkRegenerateStatus",
					"throwing " + statusCode + " VID");
			throw new IdRepoAppException(INVALID_VID.getErrorCode(),
					String.format(INVALID_VID.getErrorMessage(), statusCode));
		}
	}

	private Vid retrieveVidEntity(String vid) {
		return vidRepo.findByVid(vid);
	}

	private void checkExpiry(LocalDateTime expiryDTimes) throws IdRepoAppException {
		if (!DateUtils.after(expiryDTimes, DateUtils.getUTCCurrentDateTime())) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "checkExpiry",
					"throwing Expired VID");
			throw new IdRepoAppException(INVALID_VID.getErrorCode(),
					String.format(INVALID_VID.getErrorMessage(), "EXPIRED"));
		}
	}

	private void checkStatus(String statusCode) throws IdRepoAppException {
		if (!(statusCode.equalsIgnoreCase(EnvUtil.getVidActiveStatus()) || statusCode.contentEquals(DRAFT_STATUS))) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_VID_SERVICE, "checkStatus",
					"throwing INVALID_VID with status - " + statusCode);
			throw new IdRepoAppException(INVALID_VID.getErrorCode(),
					String.format(INVALID_VID.getErrorMessage(), statusCode));
		}
	}

	private String decryptUin(String uin, String uinHash) throws IdRepoAppException {
		List<String> uinDetails = Arrays.stream(uin.split(SPLITTER)).collect(Collectors.toList());
		int saltId = Integer.parseInt(uinDetails.get(0));
		String decryptSalt = uinEncryptSaltRepo.retrieveSaltById(saltId);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(saltId);
		String encryptedUin = uin.substring(uinDetails.get(0).length() + 1);
		String decryptedUin = new String(securityManager.decryptWithSalt(CryptoUtil.decodeURLSafeBase64(encryptedUin),
				CryptoUtil.decodePlainBase64(decryptSalt), uinRefId));
		String uinHashWithSalt = uinDetails.get(0) + SPLITTER
				+ securityManager.hashwithSalt(decryptedUin.getBytes(), CryptoUtil.decodePlainBase64(hashSalt));
		if (!MessageDigest.isEqual(uinHashWithSalt.getBytes(), uinHash.getBytes())) {
			throw new IdRepoAppUncheckedException(UIN_HASH_MISMATCH);
		}
		return uinDetails.get(0) + SPLITTER + decryptedUin + SPLITTER + decryptSalt;
	}

	private ResponseWrapper<VidResponseDTO> buildResponse(VidResponseDTO response, String id) {
		ResponseWrapper<VidResponseDTO> responseDto = new ResponseWrapper<>();
		responseDto.setId(id);
		responseDto.setVersion(EnvUtil.getVidAppVersion());
		responseDto.setResponse(response);
		return responseDto;
	}

	private void notify(String uin, String status, List<VidInfoDTO> vids, boolean isUpdated) {
		credentialServiceManager.notifyVIDCredential(uin, status, vids, isUpdated, uinHashSaltRepo::retrieveSaltById,
				this::credentialRequestResponseConsumer, this::idaEventConsumer);
	}

	public void credentialRequestResponseConsumer(CredentialIssueRequestWrapperDto request, Map<String, Object> response) {
		EventModel eventModel = new EventModel();
		eventModel.setTopic(vidEventTopic);
		Event event = new Event();
		event.setData(Map.of("status", EnvUtil.getVidActiveStatus(), "request", request, "response", response));
		eventModel.setEvent(event);
		websubHelper.publishEvent(eventModel);
	}

	public void idaEventConsumer(EventModel idaEvent) {
		EventModel eventModel = new EventModel();
		eventModel.setTopic(vidEventTopic);
		Event event = new Event();
		event.setData(Map.of("status", "REVOKED", "idaEvent", idaEvent));
		eventModel.setEvent(event);
		websubHelper.publishEvent(eventModel);
	}

	private String maskData(String maskData) {
		Map<String, String> context = new HashMap<>();
		context.put("maskData", maskData);
		VariableResolverFactory myVarFactory = new MapVariableResolverFactory(context);
		myVarFactory.setNextFactory(functionFactory);
		Serializable serializable = MVEL.compileExpression(identityAttribute + "(maskData);");
		return MVEL.executeExpression(serializable, context, myVarFactory, String.class);
	}

}
