package io.mosip.idrepository.common.dto;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for the MOSIP kernel cryptomanager encrypt/decrypt REST API.
 * <p>
 * Wrapped in {@link io.mosip.kernel.core.http.RequestWrapper} when posted to
 * {@code CRYPTOMANAGER_ENCRYPT} / decrypt endpoints. Consolidated from duplicate DTOs in
 * pre-merge credential-service and credential-request-generator modules.
 * </p>
 *
 * <h2>Field usage by module</h2>
 * <table>
 *   <caption>Required vs optional fields</caption>
 *   <tr><th>Field</th><th>Credential store</th><th>Credential request (legacy REST)</th></tr>
 *   <tr><td>{@link #applicationId}</td><td>Yes — app id from config</td><td>Yes</td></tr>
 *   <tr><td>{@link #referenceId}</td><td>Yes — partner id</td><td>Yes — crypto ref id</td></tr>
 *   <tr><td>{@link #timeStamp}</td><td>Yes — sync with wrapper {@code requesttime}</td><td>Yes</td></tr>
 *   <tr><td>{@link #data}</td><td>Yes — Base64 payload</td><td>Yes</td></tr>
 *   <tr><td>{@link #prependThumbprint}</td><td>Yes — from {@code mosip.credential.service.prependthumbprint}</td><td>Rarely used</td></tr>
 *   <tr><td>{@link #salt} / {@link #aad}</td><td>Not used</td><td>Optional IV / AAD for advanced crypto</td></tr>
 * </table>
 *
 * <h2>Primary consumer</h2>
 * <p>
 * {@link io.mosip.idrepository.credential.store.util.EncryptionUtil#encryptData} builds this DTO
 * for partner-certificate encryption during credential issuance. Credential-request queue encryption
 * uses {@link io.mosip.idrepository.core.security.IdRepoSecurityManager} directly (not this DTO).
 * </p>
 *
 * <p>
 * Response type: {@link io.mosip.idrepository.credential.store.dto.CryptomanagerResponseDto}.
 * </p>
 *
 * @author Urvil Joshi
 * @since 1.0.0
 * @see io.mosip.idrepository.credential.store.util.EncryptionUtil
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "Model representing a Crypto-Manager-Service Request")
public class CryptomanagerRequestDto {

	/**
	 * MOSIP application id of the calling module (for example ID Repository app id from config).
	 * Maps to cryptomanager {@code applicationId} for key/certificate selection.
	 */
	@ApiModelProperty(notes = "Application id of decrypting module")
	private String applicationId;

	/**
	 * Partner or key reference id — credential store passes partner id; cryptomanager uses it
	 * to locate the partner encryption certificate.
	 */
	@ApiModelProperty(notes = "Reference Id")
	private String referenceId;

	/**
	 * Request timestamp embedded in the cryptomanager request body.
	 * Must align with {@code RequestWrapper.requesttime} (ISO-8601 UTC with millis).
	 */
	@NotNull
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	@ApiModelProperty(notes = "Timestamp as metadata")
	private LocalDateTime timeStamp;

	/**
	 * Plaintext or ciphertext in standard Base64 encoding (not URL-safe).
	 */
	@ApiModelProperty(notes = "Data in BASE64 encoding to encrypt/decrypt")
	private String data;

	/**
	 * When {@code true}, partner certificate thumbprint is prepended to the ciphertext
	 * (credential-service policy; driven by {@code mosip.credential.service.prependthumbprint}).
	 */
	private Boolean prependThumbprint;

	/**
	 * Base64-encoded salt sent as IV — preserved from credential-request-generator for API
	 * compatibility; unused by current credential-store encryption path.
	 */
	@ApiModelProperty(notes = "Base64 Encoded Salt to be sent as IV")
	private String salt;

	/**
	 * Base64-encoded additional authenticated data (AAD) — preserved from
	 * credential-request-generator for API compatibility; unused by current credential-store path.
	 */
	@ApiModelProperty(notes = "Base64 Encoded AAD (Advance Authentication Data)")
	private String aad;
}
