package io.mosip.idrepository.identity.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.constant.IdRepoConstants;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.dto.IDAEventDTO;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.spi.AuthtypeStatusService;
import io.mosip.idrepository.core.util.TokenIDGenerator;
import io.mosip.idrepository.identity.entity.AuthtypeLock;
import io.mosip.idrepository.identity.repository.AuthLockRepository;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;

/**
 * The Class AuthtypeStatusImpl - implementation of
 * {@link AuthTypeStatusService}.
 *
 * @author Manoj SP
 */

@Component
public class AuthTypeStatusImpl implements AuthtypeStatusService {
	
	private static final String PARTNER_ACTIVE_STATUS = "Active";

	private static final String AUTH_TYPE_STATUS_IMPL = "AuthTypeStatusImpl";

	/** The mosip logger. */
	Logger mosipLogger = IdRepoLogger.getLogger(AuthTypeStatusImpl.class);

	/** The Constant HYPHEN. */
	private static final String HYPHEN = "-";

	@Value("${" + IdRepoConstants.WEB_SUB_PUBLISH_URL + "}")
	public String publisherHubURL;

	/** The auth lock repository. */
	@Autowired
	AuthLockRepository authLockRepository;

	@Autowired
	private IdRepoSecurityManager securityManager;

	/** The environment. */
	@Autowired
	private Environment env;

	/** The rest helper. */
	@Autowired
	private RestHelper restHelper;

	/** The rest builder. */
	@Autowired
	private RestRequestBuilder restBuilder;

	@Autowired
	private PublisherClient<String, IDAEventDTO, HttpHeaders> publisher;

	@Autowired
	private TokenIDGenerator tokenIdGenerator;

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
		if (idType == IdType.VID) {
			individualId = getUin(individualId);
		}
		String idHash = securityManager.hash(individualId.getBytes());
		List<Object[]> authTypeLockObjectsList = authLockRepository.findByUinHash(idHash);
		authTypeLockList = authTypeLockObjectsList.stream()
				.map(obj -> new AuthtypeLock((String) obj[0], (String) obj[1])).collect(Collectors.toList());
		return processAuthtypeList(authTypeLockList);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.authentication.core.spi.authtype.status.service.
	 * UpdateAuthtypeStatusService#updateAuthtypeStatus(io.mosip.authentication.core
	 * .spi.authtype.status.service.AuthTypeStatusDto)
	 */
	@Override
	public IdResponseDTO updateAuthTypeStatus(String individualId, IdType idType,
			List<AuthtypeStatus> authTypeStatusList) throws IdRepoAppException {
		String uin = idType == IdType.VID ? getUin(individualId) : individualId;
		IdResponseDTO updateAuthTypeStatus = doUpdateAuthTypeStatus(uin, authTypeStatusList);
		
		List<String> partnerIds = getPartnerIds();
		partnerIds.forEach(partnerId -> {
			String topic =  partnerId + "/" + IDAEventType.AUTH_TYPE_STATUS_UPDATE.name();
			tryRegisteringTopic(topic);
			publishEvent(uin, authTypeStatusList, topic, partnerId);
		});
		
		return updateAuthTypeStatus;
	}
	
	@SuppressWarnings("unchecked")
	private List<String> getPartnerIds() {
		try {
			Map<String, Object> responseWrapperMap = restHelper.requestSync(restBuilder.buildRequest(RestServicesConstants.PARTNER_SERVICE, null, Map.class));
			Object response = responseWrapperMap.get("response");
			if(response instanceof Map) {
				Object partners = ((Map<String,?>)response).get("partners");
				if(partners instanceof List) {
					List<Map<String, Object>> partnersList = (List<Map<String, Object>>) partners;
					return partnersList.stream()
								.filter(partner -> PARTNER_ACTIVE_STATUS.equalsIgnoreCase((String)partner.get("status")))
								.map(partner -> (String)partner.get("partnerID"))
								.collect(Collectors.toList());
				}
			}
		} catch (RestServiceException | IdRepoDataValidationException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), this.getClass().getSuperclass().getSimpleName(), "getPartnerIds", e.getMessage());
		}
		return Collections.emptyList();
	}

	private void tryRegisteringTopic(String topic) {
		try {
			publisher.registerTopic(topic, publisherHubURL);
		} catch (WebSubClientException e) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), "IdRepoConfig", "init", e.getMessage().toUpperCase());
		} catch (IdRepoAppUncheckedException e) {
			mosipLogger.warn(IdRepoSecurityManager.getUser(), "IdRepoConfig", "init", e.getMessage().toUpperCase());
		}	
	}

	private void publishEvent(String individualId, List<AuthtypeStatus> authTypeStatusList, String topic, String partnerId) {
		IDAEventDTO event = new IDAEventDTO();
		event.setTokenId(tokenIdGenerator.generateTokenID(individualId, partnerId));
		event.setAuthTypeStatusList(authTypeStatusList);
		publisher.publishUpdate(topic, event,
				MediaType.APPLICATION_JSON_UTF8_VALUE, null, publisherHubURL);
	}

	private String getUin(String vid) throws IdRepoAppException {
		try {
			RestRequestDTO request = restBuilder.buildRequest(RestServicesConstants.RETRIEVE_UIN_BY_VID, null,
					ResponseWrapper.class);
			request.setUri(request.getUri().replace("{vid}", vid));
			ResponseWrapper<Map<String, String>> response = restHelper.requestSync(request);
			return response.getResponse().get("UIN");
		} catch (RestServiceException e) {
			if (e.getResponseBodyAsString().isPresent()) {
				List<ServiceError> errorList = ExceptionUtils.getServiceErrorList(e.getResponseBodyAsString().get());
				mosipLogger.error(IdRepoSecurityManager.getUser(), AUTH_TYPE_STATUS_IMPL, "getUin", "\n" + errorList);
				throw new IdRepoAppException(errorList.get(0).getErrorCode(), errorList.get(0).getMessage());
			} else {
				mosipLogger.error(IdRepoSecurityManager.getUser(), AUTH_TYPE_STATUS_IMPL, "getUin", e.getMessage());
				throw new IdRepoAppException(IdRepoErrorConstants.UNKNOWN_ERROR);
			}
		}
	}

	private IdResponseDTO doUpdateAuthTypeStatus(String individualId, List<AuthtypeStatus> authTypeStatusList)
			throws IdRepoAppException {
		List<AuthtypeLock> entities = authTypeStatusList.stream()
				.map(authtypeStatus -> this.putAuthTypeStatus(authtypeStatus, individualId))
				.collect(Collectors.toList());
		authLockRepository.saveAll(entities);

		return buildResponse();
	}

	/**
	 * Put auth type status.
	 *
	 * @param authtypeStatus
	 *            the authtype status
	 * @param uin
	 *            the uin
	 * @param reqTime
	 *            the req time
	 * @return the authtype lock
	 */
	private AuthtypeLock putAuthTypeStatus(AuthtypeStatus authtypeStatus, String uin) {
		AuthtypeLock authtypeLock = new AuthtypeLock();
		authtypeLock.setHashedUin(securityManager.hash(uin.getBytes()));
		String authType = authtypeStatus.getAuthType();
		if (authType.equalsIgnoreCase("bio")) {
			authType = authType + "-" + authtypeStatus.getAuthSubType();
		}
		authtypeLock.setAuthtypecode(authType);
		authtypeLock.setCrDTimes(DateUtils.getUTCCurrentDateTime());
		authtypeLock.setLockrequestDTtimes(DateUtils.getUTCCurrentDateTime());
		authtypeLock.setLockstartDTtimes(DateUtils.getUTCCurrentDateTime());
		authtypeLock.setStatuscode(Boolean.toString(authtypeStatus.getLocked()));
		authtypeLock.setCreatedBy(env.getProperty(IdRepoConstants.APPLICATION_ID));
		authtypeLock.setCrDTimes(DateUtils.getUTCCurrentDateTime());
		authtypeLock.setLangCode(env.getProperty(IdRepoConstants.MOSIP_PRIMARY_LANGUAGE));
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
		return authtypeStatusResponseDto;
	}

	/**
	 * Process authtype list.
	 *
	 * @param authtypelockList
	 *            the authtypelock list
	 * @return the list
	 */
	private List<AuthtypeStatus> processAuthtypeList(List<AuthtypeLock> authtypelockList) {
		return authtypelockList.stream().map(this::getAuthTypeStatus).collect(Collectors.toList());
	}

	/**
	 * Gets the auth type status.
	 *
	 * @param authtypeLock
	 *            the authtype lock
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
		authtypeStatus.setLocked(isLocked);
		return authtypeStatus;
	}

}