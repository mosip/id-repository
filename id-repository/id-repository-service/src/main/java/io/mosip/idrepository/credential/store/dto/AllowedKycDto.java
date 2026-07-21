package io.mosip.idrepository.credential.store.dto;

import java.util.List;

import lombok.Data;

/**
 * Partner policy descriptor for a single KYC attribute shareable in a credential.
 * <p>
 * Parsed from PMS policy JSON and used by {@link io.mosip.idrepository.credential.store.provider.CredentialProvider}
 * to locate identity fields, apply format/mask rules, and decide encryption.
 * </p>
 *
 * @see PolicyAttributesDto#getShareableAttributes()
 */
@Data
public class AllowedKycDto {

	/** Credential JSON key exposed to the partner (may differ from identity attribute name). */
	public String attributeName;

	/** Attribute group: {@code null} for demographic fields, {@code CBEFF} for biometrics. */
	public String group;

	/** One or more identity sources and filters defining how the attribute is resolved. */
	public List<Source> source;

	/** When {@code true}, attribute value is PIN-encrypted in the issued credential. */
	public boolean encrypted;

	/**
	 * Output format directive: MVEL formatter name, {@code bestTwoFingers}, {@code jpeg},
	 * {@code retrieve} for existing VID, etc.
	 */
	public String format;
}
