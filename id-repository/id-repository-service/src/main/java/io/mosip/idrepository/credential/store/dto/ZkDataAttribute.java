package io.mosip.idrepository.credential.store.dto;

import lombok.Data;

/**
 * Zero-knowledge proof attribute pair for IdAuth ZK credential flows.
 * <p>
 * Maps an attribute identifier to its ZK-protected value returned by cryptomanager ZK APIs.
 * </p>
 */
@Data
public class ZkDataAttribute {

	/** Attribute name or identifier in the ZK proof structure. */
	private String identifier;

	/** Base64-encoded ZK-protected attribute value. */
	private String value;
}
