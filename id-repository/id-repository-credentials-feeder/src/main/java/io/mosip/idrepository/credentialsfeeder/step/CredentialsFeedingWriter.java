package io.mosip.idrepository.credentialsfeeder.step;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.builder.RestRequestBuilder;
import io.mosip.idrepository.core.constant.IdRepoErrorConstants;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.RestRequestDTO;
import io.mosip.idrepository.core.dto.VidInfoDTO;
import io.mosip.idrepository.core.dto.VidsInfosDTO;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.helper.RestHelper;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.manager.CredentialStatusManager;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;
import io.mosip.idrepository.credentialsfeeder.entity.Uin;

/**
 * The Class CredentialsFeedingWriter - Class to feed credentials using
 * credential requests. Implements {@code ItemWriter}.
 *
 * @author Loganathan Sekar
 * @author Manoj SP
 */
@Component
public class CredentialsFeedingWriter implements ItemWriter<Uin> {

	private static final String MOSIP_IDREPO_IDENTITY_UIN_STATUS_REGISTERED = "mosip.idrepo.identity.uin-status.registered";

	private static final String MOSIP_IDREPO_VID_ACTIVE_STATUS = "mosip.idrepo.vid.active-status";

	private static final String PROP_ONLINE_VERIFICATION_PARTNER_IDS = "online-verification-partner-ids";

	@Value("${" + PROP_ONLINE_VERIFICATION_PARTNER_IDS + "}")
	private String[] onlineVerificationPartnerIds;

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

	@Value("${" + MOSIP_IDREPO_IDENTITY_UIN_STATUS_REGISTERED + "}")
	private String uinActiveStatus;

	@Value("${" + MOSIP_IDREPO_VID_ACTIVE_STATUS + "}")
	private String vidActiveStatus;

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
	}

	/**
	 * This function is responsible for issuing credential to the partner
	 * 
	 * @param uin The UIN of the resident
	 */
	private void issueUinCredential(String uin) {
		credentialServiceManager.notifyUinCredential(uin, null, uinActiveStatus, false, null,
				uinHashSaltRepo::retrieveSaltById, credentialStatusManager::credentialRequestResponseConsumer,
				credentialStatusManager::idaEventConsumer, Arrays.asList(onlineVerificationPartnerIds));
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
