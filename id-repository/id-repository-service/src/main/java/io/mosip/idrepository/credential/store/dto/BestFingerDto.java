package io.mosip.idrepository.credential.store.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Ranked fingerprint sub-type returned when policy format is {@code bestTwoFingers}.
 * <p>
 * Produced by {@link io.mosip.idrepository.credential.store.provider.CredentialProvider#getBestTwoFingers}
 * from CBEFF quality scores for IdAuth-style credentials.
 * </p>
 */
@Data
@AllArgsConstructor
public class BestFingerDto {

	/** CBEFF finger sub-type label (e.g. {@code Left Index}). */
	String subType;

	/** Rank by quality: {@code 1} is highest-scoring finger, {@code 2} is second best. */
	int rank;
}
