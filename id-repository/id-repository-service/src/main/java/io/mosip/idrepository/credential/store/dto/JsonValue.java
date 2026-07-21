package io.mosip.idrepository.credential.store.dto;

import lombok.Data;

/**
 * Language-tagged string value from multi-language identity JSON arrays.
 * <p>
 * Used when formatting name and address attributes per language in
 * {@link io.mosip.idrepository.credential.store.provider.CredentialProvider}.
 * </p>
 */
@Data
public class JsonValue {

	/** BCP-47 or MOSIP language code (e.g. {@code eng}, {@code fra}). */
	private String language;

	/** Localized attribute text for the given language. */
	private String value;
}
