package io.mosip.idrepository.credentialsfeeder.step;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.mosip.idrepository.core.dto.CredentialIssueRequestDto;
import io.mosip.idrepository.core.dto.CredentialIssueRequestWrapperDto;
import io.mosip.idrepository.core.entity.CredentialRequestStatus;
import io.mosip.idrepository.core.exception.IdRepoAppException;
import io.mosip.idrepository.core.exception.IdRepoAppUncheckedException;
import io.mosip.idrepository.core.manager.CredentialServiceManager;
import io.mosip.idrepository.core.manager.CredentialStatusManager;
import io.mosip.idrepository.core.repository.UinHashSaltRepo;

/**
 * The Class CredentialsFeedingWriter - Class to feed credentials using
 * credential requests. Implements {@code ItemWriter}.
 *
 * @author Loganathan Sekar
 */
@Component
public class CredentialsFeedingWriter implements ItemWriter<CredentialRequestStatus> {

	private static final String PROP_ONLINE_VERIFICATION_PARTNER_IDS = "online-verification-partner-ids";

	private static final String SEPARATOR = ":";

	@Value("${" + PROP_ONLINE_VERIFICATION_PARTNER_IDS + "}")
	private List<String> onlineVerificationPartnerIds;

	/** The uin hash salt repo. */
	@Autowired
	private UinHashSaltRepo uinHashSaltRepo;

	@Autowired
	private CredentialServiceManager credentialServiceManager;

	@Autowired
	private CredentialStatusManager credentialStatusManager;

	private static final Set<String> processedIndividualIsAndPartnerIds = new LinkedHashSet<>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@Override
	public void write(List<? extends CredentialRequestStatus> requestIdEntities) throws Exception {
		List<? extends CredentialRequestStatus> filteredEntities;
		// Skip processing already processed individual IDs.
		synchronized (processedIndividualIsAndPartnerIds) {
			filteredEntities = requestIdEntities.stream()
					.map(entity -> {
						try {
							entity.setIndividualId(credentialStatusManager.decryptId(entity.getIndividualId()));
						} catch (IdRepoAppException e) {
							throw new IdRepoAppUncheckedException(e.getErrorCode(), e.getErrorText());
						}
						return entity;
					})
					.filter(entity -> !processedIndividualIsAndPartnerIds.contains(getKeyForIndividualIdAndPartnerId(entity)))
					.collect(Collectors.toList());

			Predicate<? super CredentialIssueRequestDto> additionalFilterCondition = req -> {
				boolean alreadyProcessed = processedIndividualIsAndPartnerIds
						.contains(this.getKeyForIndividualIdAndPartnerId(req));
				if (!alreadyProcessed) {
					processedIndividualIsAndPartnerIds.add(this.getKeyForIndividualIdAndPartnerId(req));
				}
				return !alreadyProcessed;
			};

			credentialServiceManager.sendEventsToCredService(filteredEntities, onlineVerificationPartnerIds,
					this::processCredentialRequestResponse, additionalFilterCondition, uinHashSaltRepo::retrieveSaltById);
			processedIndividualIsAndPartnerIds
					.addAll(filteredEntities.stream().map(this::getKeyForIndividualIdAndPartnerId).collect(Collectors.toSet()));
		}
	}

	private String getKeyForIndividualIdAndPartnerId(CredentialRequestStatus entity) {
		return entity.getIndividualId() + SEPARATOR + entity.getPartnerId();
	}

	private String getKeyForIndividualIdAndPartnerId(CredentialIssueRequestDto event) {
		return event.getId() + SEPARATOR + event.getIssuer();
	}

	private void processCredentialRequestResponse(CredentialIssueRequestWrapperDto request, Map<String, Object> response) {
		credentialStatusManager.credentialRequestResponseConsumer(request, response);
	}

}
