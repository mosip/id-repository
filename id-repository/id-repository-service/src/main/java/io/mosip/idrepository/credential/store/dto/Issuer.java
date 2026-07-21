package io.mosip.idrepository.credential.store.dto;

import lombok.Data;

/**
 * Credential issuer metadata embedded in MOSIP credential JSON ({@code issuer} block).
 * <p>
 * Typically populated from environment configuration during issuance, not from identity data.
 * </p>
 */
@Data
public class Issuer {

	/** Issuer organization code (country or agency identifier). */
	private String code;

	/** Display name of the issuing authority. */
	private String name;
}
