package io.mosip.idrepository.core.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Biometric exception entry (missing or unavailable modality) in an anonymous profile.
 *
 * <p>
 * Records which biometric type and sub-type could not be captured during
 * registration or update. Included in {@link AnonymousProfile#getExceptions()}
 * without exposing raw biometric data.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Built via Lombok {@code @Builder} by
 * {@link io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder}
 * when anonymizing identity payloads for issuance profiling.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link AnonymousProfile}</li>
 *   <li>{@link IdentityIssuanceProfile}</li>
 * </ul>
 *
 * @see AnonymousProfile
 * @see BiometricInfo
 * @see io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder
 */
@Data
@Builder
public class Exceptions {

	/** Biometric modality type subject to the exception. */
	private String type;

	/** Biometric sub-type subject to the exception. */
	private String subType;
}
