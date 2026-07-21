package io.mosip.idrepository.credential.store.dto;

import javax.validation.constraints.NotBlank;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Keymanager verifiable-credential JWT sign request for W3C VC proof generation.
 * <p>
 * Sent to the keymanager {@code /jwtSign} verifiable-credential endpoint by
 * {@link io.mosip.idrepository.credential.store.util.DigitalSignatureUtil#signVerCred(String, String)}
 * to produce a detached JWS attached to the VC {@code proof} block.
 * </p>
 *
 * @author Mahammed Taheer
 * @since 1.2.0
 * @see JWTSignatureRequestDto
 * @see io.mosip.idrepository.credential.store.util.DigitalSignatureUtil
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerCredSignatureRequestDto {

	/** Base64-encoded JSON-LD verifiable credential document to sign. */
	@NotBlank
	@ApiModelProperty(notes = "Base64 encoded JSON Data to sign", example = "ewogICAiYW55S2V5IjogIlRlc3QgSnNvbiIKfQ", required = true)
	private String dataToSign;

	/** Keymanager application id owning the signing key (e.g. {@code KERNEL}). */
	@ApiModelProperty(notes = "Application id to be used for signing", example = "KERNEL", required = false)
	private String applicationId;

	/** Keymanager reference id selecting the signing certificate within the application. */
	@ApiModelProperty(notes = "Refrence Id", example = "SIGN", required = false)
	private String referenceId;

	/** When {@code true}, embeds the signed payload in the JWT protected header. */
	@ApiModelProperty(notes = "Flag to include payload in  JWT Signature Header.", example = "false", required = false)
	private Boolean includePayload;

	/** When {@code true}, embeds the signing X.509 certificate in the JWT protected header. */
	@ApiModelProperty(notes = "Flag to include certificate in  JWT Signature Header.", example = "false", required = false)
	private Boolean includeCertificate;

	/** When {@code true}, embeds the SHA-256 hash of the signing certificate in the JWT protected header. */
	@ApiModelProperty(notes = "Flag to include certificate hash(sha256) in  JWT Signature Header.", example = "false", required = false)
	private Boolean includeCertHash;

	/** Optional x5u URL pointing to the signing certificate for JWT header inclusion. */
	@ApiModelProperty(notes = "Flag to include certificate URL in  JWT Signature Header.", required = false)
	private String certificateUrl;

	/** When {@code true}, validates that {@link #dataToSign} decodes to well-formed JSON before signing. */
	@ApiModelProperty(notes = "Flag to validate inputted JSON to be a valid JSON.", required = false)
	private Boolean validateJson;

	/** When {@code true}, Base64URL-encodes header parameters per JWS compact serialization rules. */
	@ApiModelProperty(notes = "Flag to determine the inputted data to be Base64URL encoded in signature process", required = false)
	private Boolean b64JWSHeaderParam;

	/** JWS signing algorithm; credential-service uses PS256 for verifiable credentials. */
	@ApiModelProperty(notes = "JWS Algorithm to use for data signing. Current supported Algorithm PS256.", required = false)
	private String signAlgorithm;

}
