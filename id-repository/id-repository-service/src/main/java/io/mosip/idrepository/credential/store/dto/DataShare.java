package io.mosip.idrepository.credential.store.dto;

import lombok.Data;

/**
 * Result of uploading an issued credential to the MOSIP Data Share service.
 * <p>
 * Returned to partners as a time-limited URL with optional transaction limits and
 * digital signature over the share metadata.
 * </p>
 *
 * @see io.mosip.idrepository.credential.store.util.DataShareUtil
 */
@Data
public class DataShare {

	/** HTTPS URL where the partner retrieves the credential payload. */
	private String url;

	/** Minutes until the data-share link expires. */
	private int validForInMinutes;

	/** Maximum download transactions allowed for this share (0 = unlimited per policy). */
	private int transactionsAllowed;

	/** Data Share policy id applied during upload. */
	private String policyId;

	/** Partner/subscriber id registered with Data Share. */
	private String subscriberId;

	/** Signature over data-share response fields for integrity verification. */
	private String signature;
}
