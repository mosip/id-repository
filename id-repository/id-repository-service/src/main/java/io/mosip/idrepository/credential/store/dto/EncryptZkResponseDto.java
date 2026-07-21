package io.mosip.idrepository.credential.store.dto;

import java.util.List;

import lombok.Data;

/**
 * Keymanager ZK-encrypt response for IdAuth credential attributes.
 * <p>
 * Contains ZK-protected attribute values plus session random key material for IdAuth verification.
 * </p>
 */
@Data
public class EncryptZkResponseDto {

	/** ZK-protected attribute identifier/value pairs. */
	private List<ZkDataAttribute> zkDataAttributes;

	/** Encrypted session random key for demo/bio attribute decryption at partner. */
	private String encryptedRandomKey;

	/** Index referencing the random key slot in cryptomanager response. */
	private String rankomKeyIndex;
}
