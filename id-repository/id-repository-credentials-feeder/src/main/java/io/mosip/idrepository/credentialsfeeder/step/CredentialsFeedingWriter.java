package io.mosip.idrepository.credentialsfeeder.step;

import static io.mosip.idrepository.credentialsfeeder.constant.Constants.MOSIP_IDREPO_IDENTITY_UIN_STATUS_REGISTERED;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.MOSIP_IDREPO_VID_ACTIVE_STATUS;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.PROP_ONLINE_VERIFICATION_PARTNER_IDS;
import static io.mosip.idrepository.credentialsfeeder.constant.Constants.UNLOCK_EXP_TIMESTAMP;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IDAEventType;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.AuthtypeStatus;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.dto.VidInfoDTO;
import io.mosip.idrepository.core.dto.VidsInfosDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.IdRepoWebSubHelper;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.manager.CredentialStatusManager;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.credentialsfeeder.entity.AuthtypeLock;
import io.mosip.idrepository.credentialsfeeder.entity.Uin;
import io.mosip.idrepository.credentialsfeeder.repository.AuthLockRepository;

/**
 * The Class CredentialsFeedingWriter - Class to feed credentials using
 * credential requests. Implements {@code ItemWriter}.
 *
 * @author Loganathan Sekar
 * @author Manoj SP
 */
@Component
public class CredentialsFeedingWriter implements ItemWriter<Uin> {

	@Value("${" + PROP_ONLINE_VERIFICATION_PARTNER_IDS + "}")
	private String[] onlineVerificationPartnerIds;

	@Value("${" + MOSIP_IDREPO_IDENTITY_UIN_STATUS_REGISTERED + "}")
	private String uinActiveStatus;

	@Value("${" + MOSIP_IDREPO_VID_ACTIVE_STATUS + "}")
	private String vidActiveStatus;

	/** The uin hash salt repo. */
	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;

	@Autowired
	private CredentialServiceManager credentialServiceManager;

	@Autowired
	private CredentialStatusManager credentialStatusManager;

	@Autowired
	private RestRequestBuilder restBuilder;

	@Autowired
	private RestHelper restHelper;
	
	@Autowired
	private IdRepoWebSubHelper webSubHelper;
	
	@Autowired
	private IdRepoSecurityManager securityManager;
	
	@Autowired
	private AuthLockRepository authLockRepo;

	/**
	 * For each Uin in the list, decrypt it, and then issue a credential for it
	 * 
	 * @param requestIdEntities The list of Uin objects that are to be processed.
	 */
	@Override
	public void write(List<? extends Uin> requestIdEntities) throws Exception {
		requestIdEntities.stream().map(this::decryptUin).forEach(this::issueCredential);
	}

	/**
	 * The function issues a UIN and VID credential to the user
	 * 
	 * @param uin The Aadhaar number of the resident.
	 */
	private void issueCredential(String uin) {
		issueUinCredential(uin);
		issueVidCredential(uin);
		publishAuthLock(uin);
	}

	/**
	 * This function is responsible for issuing credential to the partner
	 * 
	 * @param uin The UIN of the resident
	 */
	private void issueUinCredential(String uin) {
		credentialServiceManager.sendUinEventsToCredService(uin, null, false, null,
				Arrays.asList(onlineVerificationPartnerIds), uinHashSaltRepo::retrieveSaltById,
				credentialStatusManager::credentialRequestResponseConsumer);
	}

	/**
	 * It issues a VID credential.
	 * 
	 * @param uin The UIN of the resident
	 */
	private void issueVidCredential(String uin) {
		try {
			RestRequestDTO restRequest = restBuilder.buildRequest(RestServicesConstants.RETRIEVE_VIDS_BY_UIN, null,
					VidsInfosDTO.class);
			restRequest.setUri(restRequest.getUri().replace("{uin}", uin));
			VidsInfosDTO response = restHelper.requestSync(restRequest);
			List<VidInfoDTO> vidInfoDtos = response.getResponse();
			credentialServiceManager.sendVidEventsToCredService(uin, vidActiveStatus, vidInfoDtos, false,
					Arrays.asList(onlineVerificationPartnerIds), uinHashSaltRepo::retrieveSaltById,
					credentialStatusManager::credentialRequestResponseConsumer);
		} catch (RestServiceException | IdRepoDataValidationException e) {
			IdRepoLogger.getLogger(CredentialsFeedingWriter.class).error(ExceptionUtils.getStackTrace(e));
			throw new IdRepoAppUncheckedException(IdRepoErrorConstants.UNKNOWN_ERROR, e);
		}
	}
	
	/**
	 * This function finds the auth lock status details from DB based on UIN and
	 * publishes to web sub.
	 * 
	 * @param uin The UIN of the resident
	 */
	private void publishAuthLock(String uin) {
		String uinHash = securityManager.hash(uin.getBytes());
		List<AuthtypeLock> records = authLockRepo.findByHashedUin(uinHash);
		List<AuthtypeStatus> authTypeStatusList = records.stream()
				.map(authLock -> new AuthtypeStatus(authLock.getAuthtypecode(),
						Boolean.valueOf(authLock.getStatuscode()),
						Objects.isNull(authLock.getUnlockExpiryDTtimes()) ? null
								: Map.of(UNLOCK_EXP_TIMESTAMP, authLock.getUnlockExpiryDTtimes())))
				.collect(Collectors.toList());
		Stream.of(onlineVerificationPartnerIds).filter(partnerId -> !authTypeStatusList.isEmpty())
				.forEach(partnerId -> {
					String topic = partnerId + "/" + IDAEventType.AUTH_TYPE_STATUS_UPDATE.name();
					webSubHelper.publishAuthTypeStatusUpdateEvent(uinHash, authTypeStatusList, topic, partnerId);
				});
	}

	/**
	 * It decrypts the UIN and returns the
	 * decrypted UIN
	 * 
	 * @param entity The entity that you want to decrypt.
	 * @return The decrypted UIN
	 */
	private String decryptUin(Uin entity) {
		try {
			return credentialStatusManager.decryptId(entity.getUin());
		} catch (IdRepoAppException e) {
			throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText(), e);
		}
	}
}
