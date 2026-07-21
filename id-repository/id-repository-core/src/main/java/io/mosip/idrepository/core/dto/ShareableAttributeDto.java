package io.mosip.idrepository.core.dto;

import lombok.Data;

/**
 * Partner policy entry describing an identity attribute shareable in a credential.
 *
 * <p>
 * Binds attribute name, encryption flag, and output format when credential-request
 * or credential-store resolves partner sharable attributes from PMS / policy
 * configuration.
 * </p>
 *
 * <h2>API / pipeline context</h2>
 * <p>
 * Used while building credential payloads for {@code /v1/credentialservice/issue}.
 * Attribute names typically match ID schema fields; {@link #format} may request
 * transformations (for example, base64) before inclusion.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code CredentialProvider} and credential-store policy resolution</li>
 *   <li>Credential-request paths that filter {@code sharableAttributes}</li>
 * </ul>
 *
 * @see CredentialServiceRequestDto
 * @see CredentialIssueRequestDto
 * @see io.mosip.idrepository.credential.store.provider.CredentialProvider
 */
@Data
public class ShareableAttributeDto {

	/** Identity schema attribute name to include in the credential payload. */
	private String attributeName;

	/** {@code true} when the attribute value must be encrypted for the partner. */
	private boolean encrypted;

	/** Output format or transformation applied to the attribute (for example, base64). */
	private String format;
}
