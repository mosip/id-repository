package io.mosip.idrepository.credential.store.dto;

import java.util.List;

import lombok.Data;

/**
 * Constraint on identity attribute extraction for credential packaging.
 * <p>
 * Demographic filters typically specify language; biometric filters specify CBEFF type
 * (Face, Finger) and finger sub-types (Left Index, Right Thumb, etc.).
 * </p>
 *
 * @see Source
 */
@Data
public class Filter {

	/** ISO language code for multi-language demographic attributes. */
	public String language;

	/** Biometric modality value from {@link io.mosip.kernel.biometrics.constant.BiometricType}. */
	public String type;

	/** Finger or iris sub-types within the biometric type (e.g. {@code Left Index}). */
	public List<String> subType;
}
