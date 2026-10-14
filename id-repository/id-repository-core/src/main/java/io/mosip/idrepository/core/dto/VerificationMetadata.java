package io.mosip.idrepository.core.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * One verified-attribute entry on identity v2 create and update.
 *
 * <p>
 * V1 keeps {@code verifiedAttributes} as a list of claim names. V2 sends this
 * object instead: the framework, the process, the claim names, and optional
 * metadata. Stored on the identity JSON under {@code verifiedAttributes}.
 * </p>
 *
 * @see RequestV2DTO
 */
@Data
public class VerificationMetadata {

	/** Trust framework that produced this verification, for example {@code internal} or {@code kyc}. */
	private String trustFramework;

	/** Process that verified the claims, for example {@code otp_verified}. */
	private String verificationProcess;

	/** Identity field names covered by this verification, for example {@code phone}. */
	private List<String> claims;

	/** Extra verification details such as assurance level and time. */
	private Map<String, Object> metadata;
}
