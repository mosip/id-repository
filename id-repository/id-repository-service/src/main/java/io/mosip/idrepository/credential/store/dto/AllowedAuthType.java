package io.mosip.idrepository.credential.store.dto;

import lombok.Data;

/**
 * Allowed authentication modality entry (legacy/policy mirror of {@link AuthPolicyDto}).
 *
 * @see PolicyAttributesDto
 */
@Data
public class AllowedAuthType {

	/** Primary auth factor (e.g. OTP, BIO, DEMO). */
	private String authType;

	/** Sub-type within the auth factor (e.g. FINGER, IRIS, FACE for BIO). */
	private String authSubType;

	/** When {@code true}, the auth type is mandatory in partner policy. */
	private boolean mandatory;
}
