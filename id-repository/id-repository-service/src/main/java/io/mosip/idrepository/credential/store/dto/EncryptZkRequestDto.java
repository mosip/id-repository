package io.mosip.idrepository.credential.store.dto;

import java.util.List;

import lombok.Data;

/**
 * Request body for MOSIP cryptomanager ZK encryption of credential attributes.
 *
 * @see io.mosip.idrepository.credential.store.util.EncryptionUtil
 */
@Data
public class EncryptZkRequestDto {

	/** Application id or session reference passed to cryptomanager. */
	private String id;

	/** Plain attribute values to protect with zero-knowledge encryption. */
	private List<ZkDataAttribute> zkDataAttributes;
}
