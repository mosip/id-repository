package io.mosip.idrepository.credential.store.dto;

import java.util.List;

import lombok.Data;

/**
 * Parsed policy attributes section of a PMS partner credential policy.
 * <p>
 * Drives sharable KYC fields, allowed auth types, data-share TTL, and auth token type
 * during credential issuance.
 * </p>
 *
 * @see PartnerCredentialTypePolicyDto#getPolicies()
 */
@Data
public class PolicyAttributesDto {

	/** Authentication factors the partner is permitted to use with issued credentials. */
	private List<AuthPolicyDto> allowedAuthTypes;

	/** Identity attributes that may appear in the credential, with format and encryption rules. */
	private List<AllowedKycDto> shareableAttributes;

	/** Data Share upload constraints (validity, encryption, domain). */
	private DataShareDto dataSharePolicies;

	/** Partner auth token type expected by policy (e.g. partner-specific JWT). */
	private String authTokenType;
}
