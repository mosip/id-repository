package io.mosip.idrepository.credentialsfeeder.step;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	private static final boolean DEFAULT_SKIP_REQUESTING_EXISTING_CREDENTIALS_FOR_PARTNERS = true;

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
	
	private static final Set<String> processedIndividualIsAndPartnerIds = new LinkedHashSet<>();


	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@Override
	public void write(List<? extends CredentialRequestStatusEntity> requestIdEntities) throws Exception {
		List<? extends CredentialRequestStatusEntity> filteredEntities;
		//Skip processing already processed individual IDs.
		synchronized (processedIndividualIsAndPartnerIds) {
			filteredEntities = requestIdEntities.stream().filter(entity -> !processedIndividualIsAndPartnerIds
					.contains(getKeyForIndividualIdAndPartnerId(entity))).collect(Collectors.toList());
			
			Predicate<? super CredentialIssueRequestDto> additionalFilterCondition = req -> {
				boolean alreadyProcessed = processedIndividualIsAndPartnerIds
						.contains(this.getKeyForIndividualIdAndPartnerId(req));
				if(!alreadyProcessed) {
					processedIndividualIsAndPartnerIds.add(this.getKeyForIndividualIdAndPartnerId(req));
				}
				return alreadyProcessed;
			};
			
			sendEventsToCredService(filteredEntities,
					onlineVerificationPartnerIds, this::processCredentialRequestResponse, additionalFilterCondition);
			processedIndividualIsAndPartnerIds.addAll(filteredEntities.stream()
					.map(this::getKeyForIndividualIdAndPartnerId)
					.collect(Collectors.toSet()));
		}
	}

	private String getKeyForIndividualIdAndPartnerId(CredentialRequestStatusEntity entity) {
		return entity.getIndividualId() + SEPARATOR + entity.getPartnerId();
	}
	
	private String getKeyForIndividualIdAndPartnerId(CredentialIssueRequestDto event) {
		return event.getId() + SEPARATOR + event.getIssuer();
	}
	
	private void sendEventsToCredService(List<? extends CredentialRequestStatusEntity> requestEntities,
			 List<String> partnerIds, BiConsumer<CredentialIssueRequestWrapperDto, Map<String, Object>> credentialRequestResponseConsumer, Predicate<? super CredentialIssueRequestDto> additionalFilterCondition) {
		if (requestEntities != null) {
			Predicate<CredentialRequestStatusEntity> isExpiredCondition = this::isExpired;
			List<CredentialIssueRequestDto> requests = requestEntities
					.stream()
					.filter(isExpiredCondition.negate())
					.flatMap(entity -> {
				Predicate<? super String> skipExistingCredentialsForPartnersCondition = partnerId -> skipExistingCredentialsForPartners
						&& partnerId.equals(entity.getPartnerId());
				Predicate<? super CredentialIssueRequestDto> additionalPredicate = additionalFilterCondition == null ? t -> true: additionalFilterCondition;
				return partnerIds.stream()
						.filter(skipExistingCredentialsForPartnersCondition.negate())
						.map(partnerId -> {
					return createCredReqDto(entity.getIndividualId(), partnerId, entity.getIdExpiryDtimes(),
							entity.getIdTransactionLimit(), entity.getTokenId(),
							securityManager.getIdHashAndAttributes(entity.getIndividualId(), uinHashSaltRepo::retrieveSaltById));
				}).filter(additionalPredicate);
			}).collect(Collectors.toList());
			
			credentialRequestManager.sendRequestToCredService(requests, false, credentialRequestResponseConsumer);
			
		}
		
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
