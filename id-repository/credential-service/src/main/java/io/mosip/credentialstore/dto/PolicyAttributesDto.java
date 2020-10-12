package io.mosip.credentialstore.dto;

import java.util.List;

import lombok.Data;

@Data
public class PolicyAttributesDto {
	/**
	 * list of auth policy dto's.
	 */
	private List<AuthPolicyDto> allowedAuthTypes;

	/**
	 * list of allowed Kyc dto's
	 */
	private List<AllowedKycDto> shareableAttributes;

	/**
	 * Data share policies
	 */
	private DataShareDto dataSharePolicies;

	/**
	 * auth token type
	 */
	private String authTokenType;
}
