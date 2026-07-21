package io.mosip.idrepository.credential.store.dto;

import java.util.List;

import lombok.Data;

/**
 * PMS partner biometric extraction policy returned for a partner and credential policy.
 * <p>
 * Lists which identity attributes require partner-specific biometric extractors during
 * credential issuance. Cached and consumed by
 * {@link io.mosip.idrepository.credential.store.util.PolicyUtil#getPartnerExtractorFormat(String, String, String)}
 * and applied in {@link io.mosip.idrepository.credential.store.service.impl.CredentialStoreServiceImpl}.
 * </p>
 *
 * @see PartnerExtractor
 * @see PartnerExtractorResponseDto
 */
@Data
public class PartnerExtractorResponse {

	/** Partner-configured extractor mappings per sharable biometric attribute. */
	List<PartnerExtractor> extractors;

}
