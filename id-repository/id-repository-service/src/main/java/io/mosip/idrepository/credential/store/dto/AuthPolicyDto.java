package io.mosip.idrepository.credential.store.dto;

import lombok.Data;

/**
 * Allowed authentication modality from partner policy (IdAuth credentials).
 * <p>
 * Defines which auth factors (OTP, biometric, demo) the partner may use when
 * consuming the issued credential.
 * </p>
 *
 * @see PolicyAttributesDto#getAllowedAuthTypes()
 */
@Data
public class AuthPolicyDto {

	/** Primary auth factor (e.g. OTP, BIO, DEMO). */
	private String authType;

	/** Sub-type within the auth factor (e.g. FINGER, IRIS, FACE for BIO). */
	private String authSubType;

	/** When {@code true}, the partner policy requires this auth type to be present. */
	private boolean mandatory;
}
