package io.mosip.idrepository.credential.store.dto;

import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kernel signature service JWT sign request for verifiable credentials.
 *
 * @author Mahammed Taheer
 * @since 1.2.0
 * @see io.mosip.idrepository.credential.store.util.DigitalSignatureUtil
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JWTSignatureRequestDto {

	/** Base64-encoded JSON payload to sign (verifiable credential document). */
	@NotBlank
	@ApiModelProperty(notes = "Base64 encoded JSON Data to sign", example = "ewogICAiYW55S2V5IjogIlRlc3QgSnNvbiIKfQ", required = true)
	private String dataToSign;

	/** Keymanager application id owning the signing key (e.g. {@code KERNEL}). */
	@ApiModelProperty(notes = "Application id to be used for signing", example = "KERNEL", required = false)
	private String applicationId;

	/** Keymanager reference id for the signing certificate. */
	@ApiModelProperty(notes = "Refrence Id", example = "SIGN", required = false)
	private String referenceId;

	/** When {@code true}, embeds signed payload in JWT header. */
	@ApiModelProperty(notes = "Flag to include payload in  JWT Signature Header.", example = "false", required = false)
	private Boolean includePayload;

	/** When {@code true}, embeds signing certificate in JWT header. */
	@ApiModelProperty(notes = "Flag to include certificate in  JWT Signature Header.", example = "false", required = false)
	private Boolean includeCertificate;

	/** When {@code true}, embeds SHA-256 certificate hash in JWT header. */
	@ApiModelProperty(notes = "Flag to include certificate hash(sha256) in  JWT Signature Header.", example = "false", required = false)
	private Boolean includeCertHash;

	/** Optional URL to x5u certificate reference in JWT header. */
	@ApiModelProperty(notes = "Flag to include certificate hash(sha256) in  JWT Signature Header.", required = false)
	private String certificateUrl;
}
