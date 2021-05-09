package io.mosip.idrepository.credentialsfeeder.step;

import static io.mosip.idrepository.core.constant.IdRepoConstants.MODULO_VALUE;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.mosip.idrepository.core.constant.IdType;
import io.mosip.idrepository.core.constant.RestServicesConstants;
import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.exception.IdRepoDataValidationException;
import io.mosip.idrepository.core.exception.RestServiceException;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.credentialsfeeder.entity.idrepo.CredentialRequestStatusEntity;
import io.mosip.idrepository.credentialsfeeder.logger.IdRepoLogger;
import io.mosip.idrepository.credentialsfeeder.repository.idrepo.UinHashSaltRepo;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;

/**
 * The Class CredentialsFeedingWriter - Class to feed credentials using credential requests.
 * Implements {@code ItemWriter}.
 *
 * @author Manoj SP
 */
@Component
public class CredentialsFeedingWriter implements ItemWriter<CredentialRequestStatusEntity> {

	private static final Logger mosipLogger = IdRepoLogger.getLogger(CredentialsFeedingWriter.class);
	
	private static final String TOKEN = "TOKEN";

	private static final String SALT = "SALT";

	private static final String MODULO = "MODULO";

	private static final String ID_HASH = "id_hash";

	private static final String EXPIRY_TIMESTAMP = "expiry_timestamp";

	private static final String TRANSACTION_LIMIT = "transaction_limit";
	
	
	@Value("${online-verification-partner-ids}")
	private List<String> onlineVerificationPartnerIds;
	
	/** The uin hash salt repo. */
	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;
	
	/** The security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;
	
	@Autowired
	private Environment env;


	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@Override
	public void write(List<? extends CredentialRequestStatusEntity> requestIdEntitiess) throws Exception {
		
	}
	
	private void sendEventsToCredService(List<CredentialRequestStatusEntity> requestEntities, boolean isUpdate,
			 List<String> partnerIds) {
		if (requestEntities != null) {
			List<CredentialIssueRequestDto> vidRequests = requestEntities.stream().flatMap(entity -> {
				return partnerIds.stream().map(partnerId -> {
					String token = tokenIDGenerator.generateTokenID(uin, partnerId);
					return createCredReqDto(entity.getVid(), partnerId, entity.getIdExpiryDtimes(),
							entity.getIdTransactionLimit(), token, IdType.VID.getIdType(),
							entity.getHashAttributes());
				});
			}).collect(Collectors.toList());
			eventRequestsList.addAll(vidRequests);
		}

		eventRequestsList.forEach(reqDto -> {
			CredentialIssueRequestWrapperDto requestWrapper = new CredentialIssueRequestWrapperDto();
			requestWrapper.setRequest(reqDto);
			requestWrapper.setRequesttime(DateUtils.getUTCCurrentDateTime());
			String eventTypeDisplayName = isUpdate ? "Update ID" : "Create ID";
			mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "notify",
					"notifying Credential Service for event " + eventTypeDisplayName);
			sendRequestToCredService(requestWrapper);
			mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "notify",
					"notified Credential Service for event" + eventTypeDisplayName);
		});
	}

	private void sendRequestToCredService(CredentialIssueRequestWrapperDto requestWrapper) {
		try {
			Map<String, Object> response = restHelper.requestSync(restBuilder
					.buildRequest(RestServicesConstants.CREDENTIAL_REQUEST_SERVICE, requestWrapper, Map.class));
			mosipLogger.info(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "sendRequestToCredService",
					"Response of Credential Request: " + mapper.writeValueAsString(response));
		} catch (RestServiceException | IdRepoDataValidationException | JsonProcessingException e) {
			mosipLogger.error(IdRepoSecurityManager.getUser(), ID_REPO_SERVICE_IMPL, "sendRequestToCredService",
					e.getMessage());
		}
	}

	private CredentialIssueRequestDto createCredReqDto(String id, String partnerId, LocalDateTime expiryTimestamp,
			Integer transactionLimit, String token, String idType,
			Map<? extends String, ? extends Object> idHashAttributes) {
		Map<String, Object> data = new HashMap<>();
		data.putAll(idHashAttributes);
		data.put(EXPIRY_TIMESTAMP, Optional.ofNullable(expiryTimestamp).map(DateUtils::formatToISOString).orElse(null));
		data.put(TRANSACTION_LIMIT, transactionLimit);
		data.put(TOKEN, token);

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
		Integer moduloValue = env.getProperty(MODULO_VALUE, Integer.class);
		int modResult = (int) (Long.parseLong(id) % moduloValue);
		String hashSalt = uinHashSaltRepo.retrieveSaltById(modResult);
		String hash = securityManager.hashwithSalt(id.getBytes(), hashSalt.getBytes());
		hashWithAttributes.put(ID_HASH, hash);
		hashWithAttributes.put(MODULO, String.valueOf(modResult));
		hashWithAttributes.put(SALT, hashSalt);
		return hashWithAttributes;
	}

}
