package io.mosip.idrepository.identity.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite primary key for {@link UinBiometric} ({@code bio_ref_id}, {@code bio_type}, {@code bio_sub_type}).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiometricPK implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -1124172782509039861L;

	/** Uin ref id. */
	private String uinRefId;

	/** Biometric file type. */
	private String biometricFileType;

}
