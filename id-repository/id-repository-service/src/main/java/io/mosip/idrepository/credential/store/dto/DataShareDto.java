package io.mosip.idrepository.credential.store.dto;

import lombok.Data;

/**
 * Data Share policy fragment from partner credential policy (PMS).
 * <p>
 * Drives how {@link io.mosip.idrepository.credential.store.util.DataShareUtil} uploads
 * credential artifacts (TTL, encryption, share domain).
 * </p>
 *
 * @see PolicyAttributesDto#getDataSharePolicies()
 */
@Data
public class DataShareDto {

	/** Credential data-share validity window in minutes (string as stored in policy JSON). */
	private String validForInMinutes;

	/** Allowed download count for the share URL (string as stored in policy JSON). */
	private String transactionsAllowed;

	/** Encryption applied at data-share layer (e.g. partner certificate encryption). */
	private String encryptionType;

	/** Domain/host constraint for data-share URLs. */
	private String shareDomain;

	/** Share delivery type configured in policy. */
	private String typeOfShare;

	/** Data Share source identifier (deployment-specific). */
	private String source;
}
