package io.mosip.idrepository.credentialsfeeder.step;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.CredentialRequestManager;
import io.mosip.idrepository.credentialsfeeder.entity.idrepo.CredentialRequestStatusEntity;
import io.mosip.idrepository.credentialsfeeder.logger.IdRepoLogger;
import io.mosip.idrepository.credentialsfeeder.repository.idrepo.UinHashSaltRepo;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * The Class CredentialsFeedingWriter - Class to feed credentials using credential requests.
 * Implements {@code ItemWriter}.
 *
 * @author Manoj SP
 */
@Component
public class CredentialsFeedingWriter implements ItemWriter<CredentialRequestStatusEntity> {

	private static final Logger mosipLogger = IdRepoLogger.getLogger(CredentialsFeedingWriter.class);
	
	@Value("${online-verification-partner-ids}")
	private List<String> onlineVerificationPartnerIds;
	
	/** The uin hash salt repo. */
	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;
	
	/** The security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;
	
	@Autowired
	private CredentialRequestManager credentialRequestManager;
	
	private static final Map<String, String> processedIndividualIds = new ConcurrentHashMap<>();


	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@Override
	public void write(List<? extends CredentialRequestStatusEntity> requestIdEntities) throws Exception {
		List<? extends CredentialRequestStatusEntity> filteredEntities;
		//Skip processing already processed individual IDs.
		synchronized(processedIndividualIds) {
			filteredEntities = requestIdEntities.stream()
														.filter( entity -> !processedIndividualIds.containsKey(entity.getIndividualId()))
														.collect(Collectors.toList());
			processedIndividualIds.putAll(filteredEntities.stream().collect(Collectors.toMap(CredentialRequestStatusEntity::getIndividualId, CredentialRequestStatusEntity::getIndividualId)));
		}
		sendEventsToCredService(filteredEntities, onlineVerificationPartnerIds);
	}
	
	private void sendEventsToCredService(List<? extends CredentialRequestStatusEntity> requestEntities,
			 List<String> partnerIds) {
		if (requestEntities != null) {
			List<CredentialIssueRequestDto> requests = requestEntities.stream().flatMap(entity -> {
				return partnerIds.stream().map(partnerId -> {
					return createCredReqDto(entity.getIndividualId(), partnerId, entity.getIdExpiryDtimes(),
							entity.getIdTransactionLimit(), entity.getTokenId(),
							securityManager.getIdHashAndAttributes(entity.getIndividualId(), uinHashSaltRepo::retrieveSaltById));
				});
			}).collect(Collectors.toList());
			
			credentialRequestManager.sendRequestToCredService(requests, false, (req, res) -> {
				
			});
		}
		
	}

	private CredentialIssueRequestDto createCredReqDto(String id, String partnerId, LocalDateTime expiryTimestamp,
			Integer transactionLimit, String token,
			Map<? extends String, ? extends Object> idHashAttributes) {
		return credentialRequestManager.createCredReqDto(id, partnerId, expiryTimestamp, transactionLimit, token, idHashAttributes);
	}
	
	
}
