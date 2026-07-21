package io.mosip.idrepository.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Handle value and hash returned in identity retrieve/update responses.
 * <p>
 * Represents a resident contact handle (email or phone) without exposing the raw value when hashed.
 * </p>
 */
@Data
@AllArgsConstructor
public class HandleDto {

	/** Resident handle string (email or phone), when returned in clear form. */
	private String handle;

	/** SHA-256 hash of the handle for privacy-preserving lookup. */
	private String handleHash;
}
