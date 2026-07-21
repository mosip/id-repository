package io.mosip.idrepository.credential.store.dto;

import java.util.List;

import lombok.Data;

/**
 * Maps a sharable credential attribute to an identity schema field.
 * <p>
 * Each {@link AllowedKycDto} may list multiple sources; credential issuance uses the first
 * source entry when resolving demographic and biometric attributes.
 * </p>
 *
 * @see AllowedKycDto
 * @see Filter
 */
@Data
public class Source {

	/** Identity JSON attribute name (e.g. {@code fullName}, {@code individualBiometrics}, {@code VID}). */
	public String attribute;

	/** Optional language, biometric type, and sub-type constraints applied during extraction. */
	public List<Filter> filter;
}
