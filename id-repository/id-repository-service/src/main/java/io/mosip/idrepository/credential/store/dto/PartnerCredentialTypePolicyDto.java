package io.mosip.idrepository.credential.store.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Partner Management Service (PMS) policy for a partner and credential type combination.
 * <p>
 * Fetched during issuance to determine sharable attributes, auth types, data-share rules,
 * and schema version. Consumed by {@link io.mosip.idrepository.credential.store.util.PolicyUtil}
 * and {@link io.mosip.idrepository.credential.store.provider.CredentialProvider}.
 * </p>
 */
@Data
public class PartnerCredentialTypePolicyDto {

	/** Partner identifier registered in PMS. */
	private String partnerId;

	/** Credential type code (IdAuth, QRCode, VerifiableCredential, etc.). */
	private String credentialType;

	/** PMS policy identifier. */
	private String policyId;

	/** Human-readable policy name. */
	private String policyName;

	/** Policy description from PMS. */
	private String policyDesc;

	/** Policy classification in PMS. */
	private String policyType;

	/** Timestamp when the policy was published. */
	private LocalDateTime publishDate;

	/** Policy expiry; issuance rejected after this time. */
	private LocalDateTime validTill;

	/** PMS policy status (e.g. ACTIVE). */
	private String status;

	/** Policy schema/version string. */
	private String version;

	/** JSON schema URL or identifier for credential payload validation. */
	private String schema;

	/** Whether the policy is currently active in PMS. */
	private Boolean is_Active;

	/** Created-by user id in PMS audit columns. */
	private String cr_by;

	/** Created timestamp in PMS audit columns. */
	private LocalDateTime cr_dtimes;

	/** Last updated-by user id in PMS audit columns. */
	private String up_by;

	/** Last updated timestamp in PMS audit columns. */
	private LocalDateTime upd_dtimes;

	/** Parsed policy rules: sharable attributes, auth types, and data-share settings. */
	private PolicyAttributesDto policies;
}
