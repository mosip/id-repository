package io.mosip.idrepository.credentialsfeeder.step;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.idrepository.core.util.CredentialRequestManager;
import io.mosip.idrepository.credentialsfeeder.entity.CredentialRequestStatusEntity;
import io.mosip.idrepository.credentialsfeeder.repository.UinHashSaltRepo;
import io.mosip.kernel.core.util.DateUtils;

/**
 * The Class CredentialsFeedingWriter - Class to feed credentials using credential requests.
 * Implements {@code ItemWriter}.
 *
 * @author Loganathan Sekar
 */
@Component
public class CredentialsFeedingWriter implements ItemWriter<CredentialRequestStatusEntity> {

	private static final boolean DEFAULT_SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS = false;

	private static final String PROP_SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS = "skip-requesting-existing-credentials-for-partners";

	private static final String PROP_ONLINE_VERIFICATION_PARTNER_IDS = "online-verification-partner-ids";

	private static final String SEPARATOR = ":";

	@Value("${" + PROP_ONLINE_VERIFICATION_PARTNER_IDS + "}")
	private List<String> onlineVerificationPartnerIds;
	
	@Value("${" + PROP_SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS + ":"
			+ DEFAULT_SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS + "}")
	private boolean skipExistingCredentialsForPartners;
	
	/** The uin hash salt repo. */
	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;
	
	/** The security manager. */
	@Autowired
	private IdRepoSecurityManager securityManager;
	
	@Autowired
	private CredentialRequestManager credentialRequestManager;
	
	private static final Map<String, String> processedIndividualIsAndPartnerIds = new ConcurrentHashMap<>();


	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@Override
	public void write(List<? extends CredentialRequestStatusEntity> requestIdEntities) throws Exception {
		List<? extends CredentialRequestStatusEntity> filteredEntities;
		//Skip processing already processed individual IDs.
		synchronized(processedIndividualIsAndPartnerIds) {
			filteredEntities = requestIdEntities.stream()
														.filter( entity -> !processedIndividualIsAndPartnerIds.containsKey(getKeyForIndividualIdAndPartnerId(entity)))
														.collect(Collectors.toList());
			List<CredentialIssueRequestDto> eventsToCredService = sendEventsToCredService(filteredEntities, onlineVerificationPartnerIds, this::processCredentialRequestResponse);
			processedIndividualIsAndPartnerIds.putAll(filteredEntities.stream().collect(Collectors.toMap(this::getKeyForIndividualIdAndPartnerId, this::getKeyForIndividualIdAndPartnerId)));
			processedIndividualIsAndPartnerIds.putAll(eventsToCredService.stream().collect(Collectors.toMap(this::getKeyForIndividualIdAndPartnerId, this::getKeyForIndividualIdAndPartnerId)));
		}
	}

	private String getKeyForIndividualIdAndPartnerId(CredentialRequestStatusEntity entity) {
		return entity.getIndividualId() + SEPARATOR + entity.getPartnerId();
	}
	
	private String getKeyForIndividualIdAndPartnerId(CredentialIssueRequestDto event) {
		return event.getId() + SEPARATOR + event.getIssuer();
	}
	
	private List<CredentialIssueRequestDto> sendEventsToCredService(List<? extends CredentialRequestStatusEntity> requestEntities,
			 List<String> partnerIds, BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer) {
		if (requestEntities != null) {
			Predicate<CredentialRequestStatusEntity> isExpiredCondition = this::isExpired;
			List<CredentialIssueRequestDto> requests = requestEntities
					.stream()
					.filter(isExpiredCondition.negate())
					.flatMap(entity -> {
				Predicate<? super String> skipExistingCredentialsForParthersCondition = partnerId -> skipExistingCredentialsForPartners
						&& partnerId.equals(entity.getPartnerId());
				return partnerIds.stream()
						.filter(skipExistingCredentialsForParthersCondition.negate())
						.map(partnerId -> {
					return createCredReqDto(entity.getIndividualId(), partnerId, entity.getIdExpiryDtimes(),
							entity.getIdTransactionLimit(), entity.getTokenId(),
							securityManager.getIdHashAndAttributes(entity.getIndividualId(), uinHashSaltRepo::retrieveSaltById));
				});
			}).collect(Collectors.toList());
			
			credentialRequestManager.sendRequestToCredService(requests, false, credentialRequestResponseConsumer);
			
			return requests;
			
		}
		
		return List.of();
		
	}

	private CredentialIssueRequestDto createCredReqDto(String id, String partnerId, LocalDateTime expiryTimestamp,
			Integer transactionLimit, String token,
			Map<? extends String, ? extends Object> idHashAttributes) {
		return credentialRequestManager.createCredReqDto(id, partnerId, expiryTimestamp, transactionLimit, token, idHashAttributes);
	}

	private boolean isExpired(CredentialRequestStatusEntity entity) {
		return entity.getIdExpiryDtimes() != null && entity.getIdExpiryDtimes().isBefore(DateUtils.getUTCCurrentDateTime());
	}
	
	private void processCredentialRequestResponse(CredentialIssueRequestWrapperDto credentialRequestResponseConsumer,Map<String, Object> response) {
		// TODO Auto-generated method stub
	}
	
	
}
