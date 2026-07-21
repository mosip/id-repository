package io.mosip.idrepository.core.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Biometric modality summary included in an {@link AnonymousProfile} snapshot.
 *
 * <p>
 * Records modality type/sub-type, quality score, capture attempts, and digital
 * id reference without embedding raw biometric templates. Built during
 * anonymized identity issuance profiling.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Constructed via Lombok {@code @Builder} inside
 * {@link io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder}
 * when summarizing CBEFF / BIR metadata for old and new profiles.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link AnonymousProfile#getBiometricInfo()}</li>
 *   <li>Issuance-profile WebSub / analytics consumers</li>
 * </ul>
 *
 * @see AnonymousProfile
 * @see Exceptions
 * @see io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder
 */
@Data
@Builder(toBuilder = true)
public class BiometricInfo {

	/** Biometric modality type (for example, Finger, Iris, Face). */
	private String type;

	/** Biometric sub-type (for example, Left Index, Both Irises). */
	private String subType;

	/** Quality score reported by the biometric SDK for this capture. */
	private Long qualityScore;

	/** Number of capture attempts recorded for this modality. */
	private String attempts;

	/** Digital id reference associated with the biometric record. */
	private String digitalId;
}
