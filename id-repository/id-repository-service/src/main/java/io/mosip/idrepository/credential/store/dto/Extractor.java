package io.mosip.idrepository.credential.store.dto;

import lombok.Data;

/**
 * Partner biometric extractor plugin reference from PMS policy.
 * <p>
 * Identifies the extractor provider and version used when packaging CBEFF subsets.
 * </p>
 */
@Data
public class Extractor {

	/** Extractor implementation id registered with MOSIP partner services. */
	private String provider;

	/** Extractor schema/API version supported by the provider. */
	private String version;
}
