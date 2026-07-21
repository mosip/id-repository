package io.mosip.idrepository.credential.store.util;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.common.constant.LoggerFileConstant;
import io.mosip.idrepository.credential.store.exception.IdRepoException;
import io.mosip.idrepository.core.dto.CredentialServiceRequestDto;
import io.mosip.idrepository.core.dto.IdResponseDTO;
import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.pipeline.InProcessIdentityClient;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Retrieves identity demographic and biometric data for credential issuance via the SDK pipeline.
 */
@Component
public class IdrepositaryUtil {

	@Autowired
	private InProcessIdentityClient inProcessIdentityClient;

	private static final Logger LOGGER = IdRepoLogger.getLogger(IdrepositaryUtil.class);

	/**
	 * Retrieves identity data for the UIN/VID/handle in the credential request.
	 *
	 * @param credentialServiceRequestDto credential issue request with id and additional data
	 * @param bioAttributeFormatterMap    partner policy map of modality to CBEFF format
	 * @return identity response with demographic and biometric attributes
	 * @throws IdRepoException when identity retrieval fails
	 */
	public IdResponseDTO getData(CredentialServiceRequestDto credentialServiceRequestDto,
			Map<String, String> bioAttributeFormatterMap) throws IdRepoException {
		String requestId = credentialServiceRequestDto.getRequestId();
		try {
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(),
					requestId, "Id repository get data entry");
			IdResponseDTO responseObject = inProcessIdentityClient.retrieveIdentity(credentialServiceRequestDto,
					bioAttributeFormatterMap);
			LOGGER.debug(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					"Id repository get data exit");
			return responseObject;
		} catch (IdRepoException e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw e;
		} catch (Exception e) {
			LOGGER.error(IdRepoSecurityManager.getUser(), LoggerFileConstant.REQUEST_ID.toString(), requestId,
					ExceptionUtils.getStackTrace(e));
			throw new IdRepoException(e);
		}
	}
}
