package io.mosip.idrepository.credential.store.dto;

import lombok.Data;

/**
 * Partner-specific biometric extractor mapping from PMS extraction policy.
 *
 * @see PartnerExtractorResponse
 */
@Data
public class PartnerExtractor {

	/** Identity or CBEFF attribute name subject to extraction. */
	private String attributeName;

	/** Biometric modality filter (Face, Finger, etc.). */
	private String biometric;

	/** Extractor provider id and version. */
	private Extractor extractor;
}
