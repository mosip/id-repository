package io.mosip.idrepository.credential.store.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Keymanager certificate payload returned when fetching or generating a signing/encryption key pair.
 * <p>
 * Nested inside {@link KeyManagerGetCertificateResponseDto} when credential-service retrieves
 * MOSIP or partner X.509 material from the keymanager service.
 * </p>
 *
 * @see KeyManagerGetCertificateResponseDto
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "Class representing a KeyPair Generator Response")
public class KeyPairGenerateResponseDto {

	/** PEM-encoded X.509 certificate for the requested application/reference key. */
	@ApiModelProperty(notes = "X509 self-signed certificate", required = false)
	private String certificate;

	/** PEM-encoded certificate signing request generated alongside the key pair, when applicable. */
	@ApiModelProperty(notes = "Certificate Signing Request Data", required = false)
	private String certSignRequest;

	/** UTC timestamp when the certificate was issued. */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	@ApiModelProperty(notes = "Timestamp of issuance of certificate", required = true)
	private LocalDateTime issuedAt;

	/** UTC timestamp when the certificate expires and must be rotated. */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	@ApiModelProperty(notes = "Timestamp of expiry of certificate", required = true)
	private LocalDateTime expiryAt;

	/** UTC timestamp of the public key material as recorded by keymanager. */
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	@ApiModelProperty(notes = "Timestamp of public key", required = true)
	private LocalDateTime timestamp;

}
