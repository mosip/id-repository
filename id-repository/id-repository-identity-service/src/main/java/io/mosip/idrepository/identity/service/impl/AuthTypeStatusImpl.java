package io.mosip.idrepository.identity.service.impl;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.ResponseDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.AuthtypeStatusService;
import io.mosip.idrepository.core.util.EnvUtil;
import io.mosip.idrepository.identity.entity.AuthtypeLock;
import io.mosip.idrepository.identity.repository.AuthLockRepository;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;

/**
 * The Class AuthtypeStatusImpl - implementation of
 * {@link AuthTypeStatusService}.
 *
 * @author Manoj SP
 */

@Component
public class AuthTypeStatusImpl implements AuthtypeStatusService {

	private static final String UNLOCK_EXP_TIMESTAMP = "unlockExpiryTimestamp";

	private static final String PARTNER_ACTIVE_STATUS = "Active";

	private static final String AUTH_TYPE_STATUS_IMPL = "AuthTypeStatusImpl";

	/** The mosip logger. */
	Logger mosipLogger = IdRepoLogger.getLogger(AuthTypeStatusImpl.class);

	/** The Constant HYPHEN. */
	private static final String HYPHEN = "-";

	/** The auth lock repository. */
	@Autowired
	AuthLockRepository authLockRepository;

	@Autowired
	private IdRepoSecurityManager securityManager;

	/** The rest helper. */
	@Autowired
	private RestHelper restHelper;

	/** The rest builder. */
	@Autowired
	private RestRequestBuilder restBuilder;

	@Autowired
	private IdRepoWebSubHelper webSubHelper;

	@Autowired
	private IdRepoProxyServiceImpl idRepoProxyServiceImpl;

	@Autowired
	private IdRepoServiceImpl idRepoServiceImpl;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.authentication.core.spi.authtype.status.service.
	 * AuthtypeStatusService#fetchAuthtypeStatus(io.mosip.authentication.core.
	 * authtype.dto.AuthtypeRequestDto)
	 */
	@Override
	public List<AuthtypeStatus> fetchAuthTypeStatus(String individualId, IdType idType) throws IdRepoAppException {
		List<AuthtypeLock> authTypeLockList;
		List<Object[]> authTypeLockObjectsList = fetchAuthTypeStatusRecords(individualId, idType);
		authTypeLockList = authTypeLockObjectsList.stream().map(obj -> new AuthtypeLock((String) obj[0], (String) obj[1],
				Objects.nonNull(obj[2]) ? ((Timestamp) obj[2]).toLocalDateTime() : null)).collect(Collectors.toList());
		return processAuthtypeList(authTypeLockList);
	}

	private List<Object[]> fetchAuthTypeStatusRecords(String individualId, IdType idType) throws IdRepoAppException {
		if (idType == IdType.UIN) {
			String uinHash = idRepoProxyServiceImpl.retrieveUinHash(individualId);
			idRepoServiceImpl.retrieveIdentity(uinHash, IdType.UIN, null, null);
		} else if (idType == IdType.VID) {
			individualId = idRepoProxyServiceImpl.getUinByVid(individualId);
		}
		String idHash = securityManager.hash(individualId.getBytes());
		return authLockRepository.findByUinHash(idHash);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.authentication.core.spi.authtype.status.service.
	 * UpdateAuthtypeStatusService#updateAuthtypeStatus(io.mosip.authentication.core
	 * .spi.authtype.status.service.AuthTypeStatusDto)
	 * 
	 * If unlockForMinutes is specified, AuthType has been unlocked for certain
	 * duration. After that duration, auth type will be re-locked.
	 */
	@Override
	public IdResponseDTO updateAuthTypeStatus(String individualId, IdType idType, List<AuthtypeStatus> authTypeStatusList)
			throws IdRepoAppException {
		authTypeStatusList.stream().filter(
				status -> !status.getLocked() && Objects.nonNull(status.getUnlockForSeconds()) && status.getUnlockForSeconds() > 0)
				.forEach(status -> {
					status.setLocked(true);
					status.setMetadata(Collections.singletonMap(UNLOCK_EXP_TIMESTAMP, DateUtils
							.formatToISOString(DateUtils.getUTCCurrentDateTime().plusSeconds(status.getUnlockForSeconds()))));
				});
		String uin = idType == IdType.VID ? idRepoProxyServiceImpl.getUinByVid(individualId) : individualId;
		IdResponseDTO updateAuthTypeStatus = doUpdateAuthTypeStatus(uin, authTypeStatusList);

		List<String> partnerIds = getPartnerIds();
		partnerIds.forEach(partnerId -> {
			String topic = partnerId + "/" + IDAEventType.AUTH_TYPE_STATUS_UPDATE.name();
			webSubHelper.publishAuthTypeStatusUpdateEvent(uin, authTypeStatusList, topic, partnerId);
		});

		return updateAuthTypeStatus;
	}

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
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSuperclass().getSimpleName(), "getPartnerIds",
					e.getMessage());
		}
		return Collections.emptyList();
	}

	private IdResponseDTO doUpdateAuthTypeStatus(String individualId, List<AuthtypeStatus> authTypeStatusList) {
		String uinHash = securityManager.hash(individualId.getBytes());
		List<AuthtypeLock> entities = authTypeStatusList.stream()
				.map(authtypeStatus -> this.putAuthTypeStatus(authtypeStatus, uinHash)).collect(Collectors.toList());
		authLockRepository.saveAll(entities);

		return buildResponse();
	}

	/**
	 * Put auth type status.
	 *
	 * @param authtypeStatus the authtype status
	 * @param uin            the uin
	 * @param reqTime        the req time
	 * @return the authtype lock
	 */
	private AuthtypeLock putAuthTypeStatus(AuthtypeStatus authtypeStatus, String uinHash) {
		AuthtypeLock authtypeLock = new AuthtypeLock();
		authtypeLock.setHashedUin(uinHash);
		String authType = authtypeStatus.getAuthType();
		if (authType.equalsIgnoreCase("bio") || authType.equalsIgnoreCase("otp")) {
			authType = authType + "-" + authtypeStatus.getAuthSubType();
		}
		authtypeLock.setAuthtypecode(authType);
		authtypeLock.setCrDTimes(DateUtils.getUTCCurrentDateTime());
		authtypeLock.setLockrequestDTtimes(DateUtils.getUTCCurrentDateTime());
		authtypeLock.setLockstartDTtimes(DateUtils.getUTCCurrentDateTime());
		if (Objects.nonNull(authtypeStatus.getMetadata()) && authtypeStatus.getMetadata().containsKey(UNLOCK_EXP_TIMESTAMP)) {
			authtypeLock.setUnlockExpiryDTtimes(
					DateUtils.parseToLocalDateTime((String) authtypeStatus.getMetadata().get(UNLOCK_EXP_TIMESTAMP)));
		}
		authtypeLock.setStatuscode(Boolean.toString(authtypeStatus.getLocked()));
		authtypeLock.setCreatedBy(EnvUtil.getAppId());
		authtypeLock.setCrDTimes(DateUtils.getUTCCurrentDateTime());
		authtypeLock.setLangCode("");
		return authtypeLock;
	}

	/**
	 * Builds the response.
	 *
	 * @return the update authtype status response dto
	 */
	private IdResponseDTO buildResponse() {
		IdResponseDTO authtypeStatusResponseDto = new IdResponseDTO();
		authtypeStatusResponseDto.setResponsetime(DateUtils.getUTCCurrentDateTime());
		ResponseDTO response = new ResponseDTO();
		response.setStatus("Success");
		authtypeStatusResponseDto.setResponse(response);
		authtypeStatusResponseDto.setErrors(null);
		return authtypeStatusResponseDto;
	}

	/**
	 * Process authtype list.
	 *
	 * @param authtypelockList the authtypelock list
	 * @return the list
	 */
	private List<AuthtypeStatus> processAuthtypeList(List<AuthtypeLock> authtypelockList) {
		return authtypelockList.stream().map(this::getAuthTypeStatus).collect(Collectors.toList());
	}

	/**
	 * Gets the auth type status.
	 *
	 * @param authtypeLock the authtype lock
	 * @return the auth type status
	 */
	private AuthtypeStatus getAuthTypeStatus(AuthtypeLock authtypeLock) {
		AuthtypeStatus authtypeStatus = new AuthtypeStatus();
		String authtypecode = authtypeLock.getAuthtypecode();
		if (authtypecode.contains(HYPHEN)) {
			String[] authcode = authtypecode.split(HYPHEN);
			authtypeStatus.setAuthType(authcode[0]);
			authtypeStatus.setAuthSubType(authcode[1]);
		} else {
			authtypeStatus.setAuthType(authtypecode);
			authtypeStatus.setAuthSubType(null);
		}
		boolean isLocked = authtypeLock.getStatuscode().equalsIgnoreCase(Boolean.TRUE.toString());
		boolean isAuthTypeUnlockedTemporarily = isLocked && Objects.nonNull(authtypeLock.getUnlockExpiryDTtimes())
				&& authtypeLock.getUnlockExpiryDTtimes().isAfter(DateUtils.getUTCCurrentDateTime());
		authtypeStatus.setLocked(!isAuthTypeUnlockedTemporarily && isLocked);
		authtypeStatus.setUnlockForSeconds(isAuthTypeUnlockedTemporarily
				? ChronoUnit.SECONDS.between(DateUtils.getUTCCurrentDateTime(), authtypeLock.getUnlockExpiryDTtimes())
				: null);
		return authtypeStatus;
	}

}
